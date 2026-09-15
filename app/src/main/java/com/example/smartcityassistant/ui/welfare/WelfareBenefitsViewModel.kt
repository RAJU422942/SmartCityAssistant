package com.example.smartcityassistant.ui.welfare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.welfare.GovernmentBenefit
import com.example.smartcityassistant.data.welfare.WelfareBenefitRepository
import com.example.smartcityassistant.data.welfare.WelfareCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WelfareBenefitsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WelfareBenefitRepository()
    private val allBenefits = repository.getBenefits()

    private val _uiState = MutableStateFlow(
        WelfareBenefitsUiState(
            benefits = allBenefits,
            filteredBenefits = allBenefits
        )
    )
    val uiState: StateFlow<WelfareBenefitsUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterBenefits(allBenefits, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredBenefits = filtered)
        }
    }

    fun onCategorySelected(category: WelfareCategory) {
        _uiState.update { state ->
            val filtered = filterBenefits(allBenefits, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredBenefits = filtered)
        }
    }

    fun selectBenefit(benefit: GovernmentBenefit?) {
        _uiState.update { it.copy(selectedBenefitForDetail = benefit) }
    }

    private fun filterBenefits(list: List<GovernmentBenefit>, category: WelfareCategory, query: String): List<GovernmentBenefit> {
        val trimmedQuery = query.trim().lowercase()

        // When searchQuery is blank, selectedCategory controls the displayed list
        if (trimmedQuery.isBlank()) {
            return if (category == WelfareCategory.ALL) {
                list
            } else {
                list.filter { it.category == category }
            }
        }

        // When searchQuery is not blank, search MUST be global across ALL verified benefits (ignore category)
        val queryTokens = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        data class ScoredBenefit(
            val benefit: GovernmentBenefit,
            val score: Int
        )

        val scoredList = list.mapNotNull { benefit ->
            val nameLower = benefit.name.lowercase()
            val descLower = benefit.shortDescription.lowercase()
            val aboutLower = benefit.about.lowercase()
            val keyBenefitLower = benefit.keyBenefit.lowercase()
            val eligibilityLower = benefit.eligibility.lowercase()
            val authLower = benefit.authority.lowercase()
            val catNameLower = benefit.category.displayName.lowercase()
            val keywordsLower = benefit.keywords.map { it.lowercase() }

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

            // 4. Keyword match
            val keywordMatch = keywordsLower.any { kw -> kw == trimmedQuery || queryTokens.all { token -> kw.contains(token) } }
            if (keywordMatch) {
                bestScore = maxOf(bestScore, 75)
            } else if (keywordsLower.any { kw -> queryTokens.any { token -> kw.contains(token) } }) {
                bestScore = maxOf(bestScore, 60)
            }

            // 5. Key benefit match
            if (keyBenefitLower.contains(trimmedQuery) || queryTokens.any { token -> keyBenefitLower.contains(token) }) {
                bestScore = maxOf(bestScore, 65)
            }

            // 6. Eligibility match
            if (eligibilityLower.contains(trimmedQuery) || queryTokens.all { token -> eligibilityLower.contains(token) }) {
                bestScore = maxOf(bestScore, 55)
            }

            // 7. About/Description match
            if (aboutLower.contains(trimmedQuery) || descLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 45)
            }

            // 8. Category match
            if (catNameLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 40)
            }

            // 9. Authority match
            if (authLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 35)
            }

            if (bestScore > 0) {
                ScoredBenefit(benefit, bestScore)
            } else {
                null
            }
        }

        return scoredList.sortedByDescending { it.score }.map { it.benefit }
    }
}

data class WelfareBenefitsUiState(
    val benefits: List<GovernmentBenefit>,
    val filteredBenefits: List<GovernmentBenefit>,
    val searchQuery: String = "",
    val selectedCategory: WelfareCategory = WelfareCategory.ALL,
    val selectedBenefitForDetail: GovernmentBenefit? = null
)
