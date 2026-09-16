package com.example.smartcityassistant.ui.explore

data class ExploreService(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val iconName: String,
    val route: String
)

data class ExploreCategorySection(
    val title: String,
    val services: List<ExploreService>
)

data class ExploreUiState(
    val searchQuery: String = "",
    val categories: List<ExploreCategorySection> = emptyList(),
    val filteredCategories: List<ExploreCategorySection> = emptyList(),
    val recentServices: List<ExploreService> = emptyList()
)
