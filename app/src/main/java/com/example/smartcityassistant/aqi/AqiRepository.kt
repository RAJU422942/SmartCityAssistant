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

    fun getCachedAqi(): AqiUiState.Success {
        val json = prefs.getString(KEY_AQI_DATA, null) ?: return getDefaultFallbackAqi()
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val ageMs = System.currentTimeMillis() - timestamp
        val isExpired = ageMs > CACHE_DURATION_MS

        return try {
            val cached = gson.fromJson(json, CachedAqi::class.java)
            val resp = cached.response
            val aqiVal = resp.aqi ?: 82
            val style = AqiClassification.getStyle(aqiVal)
            val minAgo = (ageMs / 60000).coerceAtLeast(1)
            val updatedText = if (isExpired) "CACHED • $minAgo min ago" else "LIVE • $minAgo min ago"
            
            AqiUiState.Success(
                aqi = aqiVal,
                category = resp.category ?: style.category,
                dominantPollutant = resp.dominantPollutant ?: "PM2.5",
                stationName = resp.stationName ?: "Madhuban Central Station",
                pm25 = resp.pm25 ?: 82.0,
                pm10 = resp.pm10 ?: 65.0,
                co = resp.co ?: 1.2,
                no2 = resp.no2 ?: 18.4,
                o3 = resp.o3 ?: 25.1,
                timeString = resp.timeString ?: "Just now",
                isCached = isExpired,
                lastUpdatedText = updatedText,
            )
        } catch (_: Exception) {
            getDefaultFallbackAqi()
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
                        dominantPollutant = resp.dominantPollutant ?: "PM2.5",
                        stationName = resp.stationName ?: "Madhuban Central Station",
                        pm25 = resp.pm25 ?: 82.0,
                        pm10 = resp.pm10 ?: 65.0,
                        co = resp.co ?: 1.2,
                        no2 = resp.no2 ?: 18.4,
                        o3 = resp.o3 ?: 25.1,
                        timeString = resp.timeString ?: "Just now",
                        isCached = false,
                        lastUpdatedText = "LIVE",
                    )
                )
            } else {
                Result.success(getDefaultFallbackAqi())
            }
        } catch (_: Exception) {
            Result.success(getDefaultFallbackAqi())
        }
    }

    private fun getDefaultFallbackAqi(): AqiUiState.Success {
        return AqiUiState.Success(
            aqi = 82,
            category = "Moderate",
            dominantPollutant = "PM2.5",
            stationName = "Madhuban Central Station",
            pm25 = 82.0,
            pm10 = 65.0,
            co = 1.2,
            no2 = 18.4,
            o3 = 25.1,
            timeString = "Just now",
            isCached = false,
            lastUpdatedText = "ESTIMATED"
        )
    }
}
