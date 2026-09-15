package com.example.smartcityassistant.ui.government.schemes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.government.schemes.GovernmentScheme
import com.example.smartcityassistant.data.government.schemes.GovernmentSchemeRepository
import com.example.smartcityassistant.data.government.schemes.SchemeCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GovernmentSchemesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GovernmentSchemeRepository()
    private val allSchemes = repository.getSchemes()

    private val _uiState = MutableStateFlow(
        GovernmentSchemesUiState(
            schemes = allSchemes,
            filteredSchemes = allSchemes,
            quickSchemes = allSchemes.take(6)
        )
    )
    val uiState: StateFlow<GovernmentSchemesUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterSchemes(allSchemes, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredSchemes = filtered)
        }
    }

    fun onCategorySelected(category: SchemeCategory) {
        _uiState.update { state ->
            val filtered = filterSchemes(allSchemes, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredSchemes = filtered)
        }
    }

    fun selectScheme(scheme: GovernmentScheme?) {
        _uiState.update { it.copy(selectedSchemeForDetail = scheme) }
    }

    private fun filterSchemes(list: List<GovernmentScheme>, category: SchemeCategory, query: String): List<GovernmentScheme> {
        val trimmedQuery = query.trim().lowercase()

        // When searchQuery is blank, selectedCategory controls the displayed list
        if (trimmedQuery.isBlank()) {
            return if (category == SchemeCategory.ALL) {
                list
            } else {
                list.filter { it.category == category }
            }
        }

        // When searchQuery is not blank, search MUST be global across ALL verified schemes (ignore category)
        val queryTokens = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        data class ScoredScheme(
            val scheme: GovernmentScheme,
            val score: Int
        )

        val scoredList = list.mapNotNull { scheme ->
            val nameLower = scheme.name.lowercase()
            val descLower = scheme.shortDescription.lowercase()
            val purposeLower = scheme.purpose.lowercase()
            val beneficiaryLower = scheme.beneficiary.lowercase()
            val eligibilityLower = scheme.eligibility.lowercase()
            val benefitsLower = scheme.benefits.lowercase()
            val catNameLower = scheme.category.displayName.lowercase()
            val authLower = scheme.authority.lowercase()
            val keywordsLower = scheme.keywords.map { it.lowercase() }

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

            // 4. Exact keyword match or token matches in keywords
            val keywordMatch = keywordsLower.any { kw -> kw == trimmedQuery || queryTokens.all { token -> kw.contains(token) } }
            if (keywordMatch) {
                bestScore = maxOf(bestScore, 75)
            } else if (keywordsLower.any { kw -> queryTokens.any { token -> kw.contains(token) } }) {
                bestScore = maxOf(bestScore, 60)
            }

            // 5. Purpose match
            if (queryTokens.all { token -> purposeLower.contains(token) }) {
                bestScore = maxOf(bestScore, 65)
            } else if (queryTokens.any { token -> purposeLower.contains(token) }) {
                bestScore = maxOf(bestScore, 50)
            }

            // 6. Beneficiary match
            if (beneficiaryLower.contains(trimmedQuery) || queryTokens.any { token -> beneficiaryLower.contains(token) }) {
                bestScore = maxOf(bestScore, 55)
            }

            // 7. Eligibility match
            if (queryTokens.all { token -> eligibilityLower.contains(token) }) {
                bestScore = maxOf(bestScore, 45)
            } else if (queryTokens.any { token -> eligibilityLower.contains(token) }) {
                bestScore = maxOf(bestScore, 35)
            }

            // 8. Benefits match
            if (queryTokens.any { token -> benefitsLower.contains(token) }) {
                bestScore = maxOf(bestScore, 42)
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
                ScoredScheme(scheme, bestScore)
            } else {
                null
            }
        }

        return scoredList.sortedByDescending { it.score }.map { it.scheme }
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

data class GovernmentSchemesUiState(
    val schemes: List<GovernmentScheme>,
    val filteredSchemes: List<GovernmentScheme>,
    val quickSchemes: List<GovernmentScheme>,
    val searchQuery: String = "",
    val selectedCategory: SchemeCategory = SchemeCategory.ALL,
    val selectedSchemeForDetail: GovernmentScheme? = null
)
