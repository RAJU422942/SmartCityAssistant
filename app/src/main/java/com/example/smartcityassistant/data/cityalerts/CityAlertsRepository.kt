package com.example.smartcityassistant.data.cityalerts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CityAlertsRepository(private val context: Context) {
    private val api = CityAlertsClient.service

    suspend fun getAlerts(
        lat: Double? = null,
        lon: Double? = null,
        city: String? = null,
        district: String? = null,
        category: String? = null,
    ): Result<CityAlertsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAlerts(lat, lon, city, district, category)
                if (response.status == "OK") {
                    Result.success(response)
                } else {
                    Result.failure(Exception("City alerts unavailable"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
