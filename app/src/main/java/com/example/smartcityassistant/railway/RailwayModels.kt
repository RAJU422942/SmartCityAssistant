package com.example.smartcityassistant.railway

import com.example.smartcityassistant.weather.ForecastDayDto

data class Train(
    val number: String,
    val name: String,
    val from: String,
    val to: String,
    val departure: String,
    val arrival: String,
    val duration: String,
    val runningDays: String,
    val classes: List<String>,
    val status: String = "On Time",
    val dataSource: TransportDataSourceType = TransportDataSourceType.SCHEDULED,
    val availability: String = "Available (GN, 3A, 2A)",
    val fare: String = "₹210 - ₹1250 (Est.)"
)

data class RailwayStation(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Float = 0f
)

data class TrainRouteStation(
    val stationName: String,
    val arrivalTime: String,
    val departureTime: String,
    val dayCount: Int,
    val distanceKm: Double
)

data class TrainRoute(
    val trainNumber: String,
    val trainName: String,
    val origin: String,
    val destination: String,
    val stations: List<TrainRouteStation>,
    val dataSource: TransportDataSourceType = TransportDataSourceType.SCHEDULED
)

data class TrainLiveStatus(
    val trainNumber: String,
    val trainName: String,
    val currentStation: String,
    val nextStation: String,
    val delayMinutes: Int,
    val runningState: String,
    val lastUpdated: String,
    val dataSource: TransportDataSourceType = TransportDataSourceType.UNAVAILABLE
)

enum class TransportDataSourceType {
    LIVE, SCHEDULED, ESTIMATED, DEMO, UNAVAILABLE
}

data class BackendLiveStatusResponse(
    val trainNumber: String,
    val trainName: String,
    val currentStation: String,
    val nextStation: String?,
    val delayMinutes: Int,
    val runningState: String,
    val lastUpdated: String,
    val status: String,
    val message: String? = null
)

data class BackendPnrResponse(
    val pnrNumber: String,
    val trainNumber: String,
    val trainName: String,
    val journeyDate: String,
    val from: String,
    val to: String,
    val bookingStatus: String,
    val currentStatus: String,
    val coachBerth: String,
    val chartingStatus: String,
    val status: String? = null,
    val message: String? = null
)

data class BackendAvailabilityResponse(
    val trainNumber: String,
    val status: String,
    val availabilityText: String,
    val fare: String?,
    val message: String? = null
)

data class BackendTrainResponse(
    val trainNumber: String?,
    val trainName: String?,
    val source: String?,
    val destination: String?,
    val departureTime: String?,
    val arrivalTime: String?,
    val duration: String?,
    val runningDays: List<String>?,
    val status: String?,
    val classes: List<String>?
)

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
    val forecast: List<ForecastDayDto>?,
    val message: String?
)
