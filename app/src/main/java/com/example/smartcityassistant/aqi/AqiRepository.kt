package com.example.smartcityassistant.aqi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson

class AqiRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_city_aqi_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_AQI_DATA = "cached_aqi_json"
        private const val KEY_TIMESTAMP = "cached_aqi_timestamp"
        private const val CACHE_DURATION_MS = 20 * 60 * 1000L // 20 minutes
    }

    data class CachedAqi(
        val response: AqiResponseDto,
        val timestamp: Long,
    )

    fun getCachedAqi(): AqiUiState.Success? {
        val json = prefs.getString(KEY_AQI_DATA, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val ageMs = System.currentTimeMillis() - timestamp
        val isExpired = ageMs > CACHE_DURATION_MS

        return try {
            val cached = gson.fromJson(json, CachedAqi::class.java)
            val resp = cached.response
            val aqiVal = resp.aqi ?: 0
            val style = AqiClassification.getStyle(aqiVal)
            val minAgo = (ageMs / 60000).coerceAtLeast(1)
            val updatedText = if (isExpired) "CACHED • $minAgo min ago" else "LIVE • $minAgo min ago"
            
            AqiUiState.Success(
                aqi = aqiVal,
                category = resp.category ?: style.category,
                dominantPollutant = resp.dominantPollutant,
                stationName = resp.stationName,
                pm25 = resp.pm25,
                pm10 = resp.pm10,
                co = resp.co,
                no2 = resp.no2,
                o3 = resp.o3,
                timeString = resp.timeString,
                isCached = isExpired,
                lastUpdatedText = updatedText,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveCache(response: AqiResponseDto) {
        try {
            val cached = CachedAqi(response, System.currentTimeMillis())
            prefs.edit {
                putString(KEY_AQI_DATA, gson.toJson(cached))
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
        } catch (_: Exception) {
        }
    }

    suspend fun fetchAqi(lat: Double, lon: Double): Result<AqiUiState.Success> {
        return try {
            val resp = AqiClient.service.getAqi(lat, lon)
            if ((resp.status == "OK") && (resp.aqi != null)) {
                saveCache(resp)
                val aqiVal = resp.aqi
                val style = AqiClassification.getStyle(aqiVal)
                Result.success(
                    AqiUiState.Success(
                        aqi = aqiVal,
                        category = resp.category ?: style.category,
                        dominantPollutant = resp.dominantPollutant,
                        stationName = resp.stationName,
                        pm25 = resp.pm25,
                        pm10 = resp.pm10,
                        co = resp.co,
                        no2 = resp.no2,
                        o3 = resp.o3,
                        timeString = resp.timeString,
                        isCached = false,
                        lastUpdatedText = "LIVE",
                    )
                )
            } else {
                Result.failure(Exception(resp.message ?: "AQI unavailable"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
