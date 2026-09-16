package com.example.smartcityassistant.aqi

data class AqiResponseDto(
    val status: String,
    val aqi: Int?,
    val category: String?,
    val dominantPollutant: String?,
    val stationName: String?,
    val stationLatitude: Double?,
    val stationLongitude: Double?,
    val distanceKm: Double?,
    val pm25: Double?,
    val pm10: Double?,
    val co: Double?,
    val no2: Double?,
    val o3: Double?,
    val so2: Double?,
    val nh3: Double?,
    val timeString: String?,
    val message: String?,
    val source: String?,
    val sourceLabel: String?
)
