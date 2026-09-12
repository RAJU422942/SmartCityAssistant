package com.example.smartcityassistant.aqi

sealed interface AqiUiState {
    data object Loading : AqiUiState
    data class Success(
        val aqi: Int,
        val category: String,
        val dominantPollutant: String?,
        val stationName: String?,
        val pm25: Double?,
        val pm10: Double?,
        val co: Double?,
        val no2: Double?,
        val o3: Double?,
        val timeString: String?,
        val isCached: Boolean = false,
        val lastUpdatedText: String = "LIVE"
    ) : AqiUiState
    data class Error(val message: String) : AqiUiState
}
