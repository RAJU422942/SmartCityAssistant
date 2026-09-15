package com.example.smartcityassistant.aqi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.math.abs
import retrofit2.HttpException

class AqiRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_city_aqi_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_AQI_DATA = "cached_aqi_json"
        private const val KEY_TIMESTAMP = "cached_aqi_timestamp"
        private const val CACHE_DURATION_MS = 20 * 60 * 1000L // 20 minutes
        // ~0.05 degrees is roughly 5km — close enough to reuse a cached reading.
        private const val LOCATION_MATCH_THRESHOLD = 0.05
    }

    data class CachedAqi(
        val response: AqiResponseDto,
        val timestamp: Long,
        val lat: Double = 0.0,
        val lon: Double = 0.0,
    )

    /**
     * Returns a cached reading only if one exists AND (when lat/lon are supplied)
     * it was captured near the requested coordinates. Previously this ignored
     * lat/lon entirely, so a fresh cached reading from one location would be shown
     * for a completely different location without ever hitting the network.
     */
    fun getCachedAqi(lat: Double? = null, lon: Double? = null): AqiUiState.Success? {
        val json = prefs.getString(KEY_AQI_DATA, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val ageMs = System.currentTimeMillis() - timestamp
        val isExpired = ageMs > CACHE_DURATION_MS

        return try {
            val cached = gson.fromJson(json, CachedAqi::class.java)

            if (lat != null && lon != null) {
                val sameLocation = abs(cached.lat - lat) < LOCATION_MATCH_THRESHOLD &&
                        abs(cached.lon - lon) < LOCATION_MATCH_THRESHOLD
                if (!sameLocation) return null
            }

            val resp = cached.response
            val aqiVal = resp.aqi ?: return null
            val style = AqiClassification.getStyle(aqiVal)
            val minAgo = (ageMs / 60000).coerceAtLeast(1)
            val updatedText = if (isExpired) "CACHED • $minAgo min ago" else "LIVE • $minAgo min ago"

            AqiUiState.Success(
                aqi = aqiVal,
                category = resp.category ?: style.category,
                dominantPollutant = resp.dominantPollutant ?: "PM2.5",
                stationName = resp.stationName ?: "Monitoring Station",
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

    fun saveCache(response: AqiResponseDto, lat: Double, lon: Double) {
        try {
            val cached = CachedAqi(response, System.currentTimeMillis(), lat, lon)
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
                saveCache(resp, lat, lon)
                val aqiVal = resp.aqi
                val style = AqiClassification.getStyle(aqiVal)
                Result.success(
                    AqiUiState.Success(
                        aqi = aqiVal,
                        category = resp.category ?: style.category,
                        dominantPollutant = resp.dominantPollutant ?: "PM2.5",
                        stationName = resp.stationName ?: "Monitoring Station",
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
                Result.failure(Exception(resp.message ?: "AQI data unavailable for location"))
            }
        } catch (e: SocketTimeoutException) {
            // Most common cause with a free-tier host: the server was asleep and didn't
            // wake up in time. Surfacing this distinctly makes it obvious in the UI/logs.
            Result.failure(Exception("AQI server is waking up (free hosting sleeps when idle) — please retry in a few seconds."))
        } catch (e: HttpException) {
            Result.failure(Exception("AQI server returned an error (HTTP ${e.code()})."))
        } catch (e: IOException) {
            Result.failure(Exception("Couldn't reach the AQI server. Check your internet connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}