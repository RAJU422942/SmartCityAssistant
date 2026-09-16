package com.example.smartcityassistant.ui.cityalerts

import com.example.smartcityassistant.aqi.AqiUiState
import com.example.smartcityassistant.data.cityalerts.CityAlert
import com.example.smartcityassistant.weather.WeatherUiState

enum class LocationSource {
    CURRENT_LOCATION,
    SEARCH
}

data class SelectedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val source: LocationSource
)

sealed interface CityAlertsUiState {
    data object Unselected : CityAlertsUiState
    data object Loading : CityAlertsUiState
    data class Success(
        val selectedCategory: String? = null,
        val selectedLocation: SelectedLocation,
        val weatherState: WeatherUiState?,
        val aqiState: AqiUiState?,
        val disasterAlerts: List<CityAlert>,
        val weatherAdvisories: List<CityAlert>,
        val aqiAdvisories: List<CityAlert>,
        val updatedAt: String? = null
    ) : CityAlertsUiState
    data class Error(val message: String, val selectedLocation: SelectedLocation? = null) : CityAlertsUiState
}
