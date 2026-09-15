package com.example.smartcityassistant.ui.notices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.notices.GovernmentNotice
import com.example.smartcityassistant.data.notices.GovernmentNoticeRepository
import com.example.smartcityassistant.data.notices.NoticeCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GovernmentNoticesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GovernmentNoticeRepository()
    private val allNotices = repository.getNotices()

    private val _uiState = MutableStateFlow(
        GovernmentNoticesUiState(
            notices = allNotices,
            filteredNotices = allNotices
        )
    )
    val uiState: StateFlow<GovernmentNoticesUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterNotices(allNotices, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredNotices = filtered)
        }
    }

    fun onCategorySelected(category: NoticeCategory) {
        _uiState.update { state ->
            val filtered = filterNotices(allNotices, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredNotices = filtered)
        }
    }

    fun selectNotice(notice: GovernmentNotice?) {
        _uiState.update { it.copy(selectedNoticeForDetail = notice) }
    }

    private fun filterNotices(list: List<GovernmentNotice>, category: NoticeCategory, query: String): List<GovernmentNotice> {
        val trimmedQuery = query.trim().lowercase()

        // When searchQuery is blank, selectedCategory controls the displayed list
        if (trimmedQuery.isBlank()) {
            return if (category == NoticeCategory.ALL) {
                list
            } else {
                list.filter { it.category == category }
            }
        }

        // When searchQuery is not blank, search MUST be global across ALL verified notices (ignore category)
        val queryTokens = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        data class ScoredNotice(
            val notice: GovernmentNotice,
            val score: Int
        )

        val scoredList = list.mapNotNull { notice ->
            val titleLower = notice.title.lowercase()
            val descLower = notice.shortDescription.lowercase()
            val fullDescLower = notice.fullDescription.lowercase()
            val authLower = notice.authority.lowercase()
            val catNameLower = notice.category.displayName.lowercase()
            val keywordsLower = notice.keywords.map { it.lowercase() }

            var bestScore = 0

            // 1. Exact title match
            if (titleLower == trimmedQuery) {
                bestScore = maxOf(bestScore, 100)
            }
            // 2. Title starts with query
            else if (titleLower.startsWith(trimmedQuery)) {
                bestScore = maxOf(bestScore, 90)
            }
            // 3. Title contains query
            else if (titleLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 80)
            }

            // 4. Keyword match
            val keywordMatch = keywordsLower.any { kw -> kw == trimmedQuery || queryTokens.all { token -> kw.contains(token) } }
            if (keywordMatch) {
                bestScore = maxOf(bestScore, 75)
            } else if (keywordsLower.any { kw -> queryTokens.any { token -> kw.contains(token) } }) {
                bestScore = maxOf(bestScore, 60)
            }

            // 5. Description match
            if (queryTokens.all { token -> descLower.contains(token) || fullDescLower.contains(token) }) {
                bestScore = maxOf(bestScore, 65)
            } else if (queryTokens.any { token -> descLower.contains(token) || fullDescLower.contains(token) }) {
                bestScore = maxOf(bestScore, 50)
            }

            // 6. Category match
            if (catNameLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 40)
            }

            // 7. Authority match
            if (authLower.contains(trimmedQuery)) {
                bestScore = maxOf(bestScore, 35)
            }

            if (bestScore > 0) {
                ScoredNotice(notice, bestScore)
            } else {
                null
            }
        }

        return scoredList.sortedByDescending { it.score }.map { it.notice }
    }
}

data class GovernmentNoticesUiState(
    val notices: List<GovernmentNotice>,
    val filteredNotices: List<GovernmentNotice>,
    val searchQuery: String = "",
    val selectedCategory: NoticeCategory = NoticeCategory.ALL,
    val selectedNoticeForDetail: GovernmentNotice? = null
)
