package com.example.smartcityassistant.ui.government.helplines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.government.helplines.HelplineCategory
import com.example.smartcityassistant.data.government.helplines.HelplineRepository
import com.example.smartcityassistant.data.government.helplines.ImportantHelpline
import com.example.smartcityassistant.data.government.helplines.HelplinePriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GovernmentHelplinesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HelplineRepository()
    private val allHelplines = repository.getHelplines()

    private val _uiState = MutableStateFlow(
        GovernmentHelplinesUiState(
            helplines = allHelplines,
            filteredHelplines = allHelplines,
            quickEmergencies = allHelplines.filter { it.isEmergency || it.priority == HelplinePriority.CRITICAL }.take(6)
        )
    )
    val uiState: StateFlow<GovernmentHelplinesUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterHelplines(allHelplines, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredHelplines = filtered)
        }
    }

    fun onCategorySelected(category: HelplineCategory) {
        _uiState.update { state ->
            val filtered = filterHelplines(allHelplines, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredHelplines = filtered)
        }
    }

    fun selectHelpline(helpline: ImportantHelpline?) {
        _uiState.update { it.copy(selectedHelplineForDetail = helpline) }
    }

    private fun filterHelplines(list: List<ImportantHelpline>, category: HelplineCategory, query: String): List<ImportantHelpline> {
        val trimmedQuery = query.trim().lowercase()

        // When searchQuery is blank, selectedCategory controls the displayed list
        if (trimmedQuery.isBlank()) {
            return if (category == HelplineCategory.ALL) {
                list
            } else {
                list.filter { it.category == category }
            }
        }

        // When searchQuery is not blank, search MUST be global across ALL verified helplines (ignore category)
        val queryTokens = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val normalizedQueryNum = trimmedQuery.replace(Regex("[^0-9]"), "")

        data class ScoredHelpline(
            val helpline: ImportantHelpline,
            val score: Int
        )

        val scoredList = list.mapNotNull { helpline ->
            val nameLower = helpline.name.lowercase()
            val normNumber = helpline.number.replace(Regex("[^0-9]"), "")
            val purposeLower = helpline.purpose.lowercase()
            val descLower = helpline.shortDescription.lowercase()
            val authLower = helpline.authority.lowercase()
            val catNameLower = helpline.category.displayName.lowercase()
            val keywordsLower = helpline.keywords.map { it.lowercase() }

            var bestScore = 0

            // 1. Exact name match
            if (nameLower == trimmedQuery) {
                bestScore = maxOf(bestScore, 100)
            }
            // 2. Name starts with query
            else if (nameLower.startsWith(trimmedQuery)) {
                bestScore = maxOf(bestScore, 90)
            }
            // 3. Name contains query
            else if (nameLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 80)
            }

            // 4. Exact number match
            if (normalizedQueryNum.isNotBlank() && normNumber == normalizedQueryNum) {
                bestScore = maxOf(bestScore, 95)
            }
            // 5. Number contains query
            else if (normalizedQueryNum.isNotBlank() && normNumber.contains(normalizedQueryNum)) {
                bestScore = maxOf(bestScore, 85)
            }

            // 6. Exact keyword match or token matches in keywords
            val keywordMatch = keywordsLower.any { kw -> kw == trimmedQuery || queryTokens.all { token -> kw.contains(token) } }
            if (keywordMatch) {
                bestScore = maxOf(bestScore, 75)
            } else if (keywordsLower.any { kw -> queryTokens.any { token -> kw.contains(token) } }) {
                bestScore = maxOf(bestScore, 60)
            }

            // 7. Purpose match (token matching)
            val purposeTokenMatches = queryTokens.all { token -> purposeLower.contains(token) }
            if (purposeTokenMatches) {
                bestScore = maxOf(bestScore, 65)
            } else if (queryTokens.any { token -> purposeLower.contains(token) }) {
                bestScore = maxOf(bestScore, 50)
            }

            // 8. Description match
            if (queryTokens.all { token -> descLower.contains(token) }) {
                bestScore = maxOf(bestScore, 45)
            } else if (queryTokens.any { token -> descLower.contains(token) }) {
                bestScore = maxOf(bestScore, 35)
            }

            // 9. Category match
            if (catNameLower.contains(trimmedQuery) || queryTokens.any { token -> catNameLower.contains(token) }) {
                bestScore = maxOf(bestScore, 40)
            }

            // 10. Authority match
            if (authLower.contains(trimmedQuery) || queryTokens.any { token -> authLower.contains(token) }) {
                bestScore = maxOf(bestScore, 35)
            }

            // 11. Fuzzy match (Levenshtein distance for name or keywords)
            if (bestScore == 0) {
                val isFuzzyMatch = queryTokens.any { token ->
                    if (token.length >= 3) {
                        nameLower.split(Regex("\\s+")).any { word ->
                            levenshteinDistance(word, token) <= maxOf(1, token.length / 3)
                        } || keywordsLower.any { kw ->
                            kw.split(Regex("\\s+")).any { word ->
                                levenshteinDistance(word, token) <= maxOf(1, token.length / 3)
                            }
                        }
                    } else {
                        false
                    }
                }
                if (isFuzzyMatch) {
                    bestScore = maxOf(bestScore, 20)
                }
            }

            if (bestScore > 0) {
                ScoredHelpline(helpline, bestScore)
            } else {
                null
            }
        }

        return scoredList.sortedByDescending { it.score }.map { it.helpline }
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(
                    cost[j] + 1,
                    newCost[j - 1] + 1,
                    cost[j - 1] + match
                )
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}

data class GovernmentHelplinesUiState(
    val helplines: List<ImportantHelpline>,
    val filteredHelplines: List<ImportantHelpline>,
    val quickEmergencies: List<ImportantHelpline>,
    val searchQuery: String = "",
    val selectedCategory: HelplineCategory = HelplineCategory.ALL,
    val selectedHelplineForDetail: ImportantHelpline? = null
)
