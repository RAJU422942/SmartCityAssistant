package com.example.smartcityassistant.data.cityalerts

data class CityAlert(
    val id: String,
    val type: String?,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val status: String,
    val startTime: String?,
    val endTime: String?,
    val lastUpdated: String?,
    val locationName: String?,
    val city: String? = null,
    val district: String? = null,
    val latitude: Double?,
    val longitude: Double?,
    val sourceName: String,
    val sourceUrl: String?,
    val isVerified: Boolean
)

data class CityAlertLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val district: String?
)

data class CityAlertsResponse(
    val status: String,
    val location: CityAlertLocation?,
    val alerts: List<CityAlert>,
    val updatedAt: String?
)
