package com.example.smartcityassistant.data.cityalerts

data class CityAlert(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val city: String?,
    val district: String?,
    val latitude: Double?,
    val longitude: Double?,
    val affectedAreas: List<String>?,
    val issuedAt: String,
    val expiresAt: String?,
    val sourceName: String,
    val sourceUrl: String?,
    val status: String,
    val isVerified: Boolean
)

data class CityAlertLocation(
    val city: String?,
    val district: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class CityAlertsResponse(
    val status: String,
    val location: CityAlertLocation?,
    val alerts: List<CityAlert>
)
