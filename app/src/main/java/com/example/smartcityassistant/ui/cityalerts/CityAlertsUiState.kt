package com.example.smartcityassistant.ui.cityalerts

import com.example.smartcityassistant.data.cityalerts.CityAlert

sealed interface CityAlertsUiState {
    data object Loading : CityAlertsUiState
    data class Success(
        val alerts: List<CityAlert>,
        val selectedCategory: String? = null,
        val locationName: String = "Madhuban, Bihar",
        val isUsingCustomLocation: Boolean = false,
        val latitude: Double = 25.6022,
        val longitude: Double = 85.1376
    ) : CityAlertsUiState
    data class Error(val message: String) : CityAlertsUiState
}
