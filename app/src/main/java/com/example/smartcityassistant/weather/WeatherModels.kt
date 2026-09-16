package com.example.smartcityassistant.weather

data class WeatherResponseDto(
    val status: String,
    val temperature: Double?,
    val apparentTemperature: Double?,
    val humidity: Int?,
    val windSpeed: Double?,
    val condition: String?,
    val weatherCode: Int?,
    val sunrise: String?,
    val sunset: String?,
    val source: String?,
    val message: String?
)

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val temperature: Double,
        val apparentTemperature: Double,
        val humidity: Int,
        val windSpeed: Double,
        val condition: String,
        val weatherCode: Int,
        val sunrise: String,
        val sunset: String,
        val isCached: Boolean = false,
        val lastUpdatedText: String = "LIVE"
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
