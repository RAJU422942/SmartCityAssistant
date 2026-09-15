package com.example.smartcityassistant.ui.explore

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExploreViewModel : ViewModel() {
    private val allCategories = listOf(
        ExploreCategorySection(
            title = "Transport",
            services = listOf(
                ExploreService("bus", "Bus", "Bus routes and services", "Transport", "directions_bus", "transport_bus"),
                ExploreService("railway", "Railway", "Train search, status & PNR", "Transport", "train", "transport_railway"),
                ExploreService("routes", "Routes", "Plan your journey", "Transport", "map", "transport")
            )
        ),
        ExploreCategorySection(
            title = "City Services",
            services = listOf(
                ExploreService("report", "Report a Problem", "Report civic issues", "City Services", "report", "report"),
                ExploreService("complaints", "My Complaints", "Track your complaints", "City Services", "assignment", "complaints"),
                ExploreService("gov", "Government Services", "Government services & schemes", "City Services", "account_balance", "explore"),
                ExploreService("alerts", "City Alerts", "Important city updates", "City Services", "notifications", "city_alerts")
            )
        ),
        ExploreCategorySection(
            title = "Nearby",
            services = listOf(
                ExploreService("nearby", "Nearby Services", "Hospital • Police • Essential services", "Nearby", "local_hospital", "nearby")
            )
        ),
        ExploreCategorySection(
            title = "Health & Environment",
            services = listOf(
                ExploreService("aqi", "Air Quality", "Live AQI & pollutant information", "Health", "air", "aqi_details")
            )
        ),
        ExploreCategorySection(
            title = "Safety",
            services = listOf(
                ExploreService("emergency", "Emergency", "Emergency help & contacts", "Safety", "emergency", "emergency")
            )
        )
    )

    private val _uiState = MutableStateFlow(ExploreUiState(categories = allCategories, filteredCategories = allCategories))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            if (query.isBlank()) {
                state.copy(searchQuery = query, filteredCategories = allCategories)
            } else {
                val filtered = allCategories.mapNotNull { section ->
                    val matchingServices = section.services.filter { service ->
                        service.title.contains(query, true) ||
                        service.subtitle.contains(query, true) ||
                        service.category.contains(query, true)
                    }
                    if (matchingServices.isNotEmpty()) {
                        section.copy(services = matchingServices)
                    } else null
                }
                state.copy(searchQuery = query, filteredCategories = filtered)
            }
        }
    }
}
