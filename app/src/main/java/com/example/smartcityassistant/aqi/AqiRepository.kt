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
        private const val LOCATION_MATCH_THRESHOLD = 0.05
    }

    data class CachedAqi(
        val response: AqiResponseDto,
        val timestamp: Long,
        val lat: Double = 0.0,
        val lon: Double = 0.0,
    )

    fun getCachedAqi(lat: Double? = null, lon: Double? = null): AqiUiState? {
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
            if (resp.status == "NO_NEARBY_AQI_STATION") {
                return AqiUiState.NoNearby(resp.message ?: "No nearby air quality monitoring station was found for your current location.", resp.distanceKm)
            }
            if (resp.status != "OK" || resp.aqi == null) {
                return null
            }

            val aqiVal = resp.aqi
            val style = AqiClassification.getStyle(aqiVal)
            val minAgo = (ageMs / 60000).coerceAtLeast(1)
            val updatedText = if (isExpired) "CACHED • $minAgo min ago" else "LIVE • $minAgo min ago"

            AqiUiState.Success(
                aqi = aqiVal,
                category = resp.category ?: style.category,
                dominantPollutant = resp.dominantPollutant ?: "PM2.5",
                stationName = resp.stationName ?: "Monitoring Station",
                stationLatitude = resp.stationLatitude,
                stationLongitude = resp.stationLongitude,
                distanceKm = resp.distanceKm,
                pm25 = resp.pm25,
                pm10 = resp.pm10,
                co = resp.co,
                no2 = resp.no2,
                o3 = resp.o3,
                timeString = resp.timeString,
                source = resp.source ?: "CPCB",
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

    suspend fun fetchAqi(lat: Double, lon: Double): Result<AqiUiState> {
        return try {
            val resp = AqiClient.service.getAqi(lat, lon)
            saveCache(resp, lat, lon)

            when (resp.status) {
                "OK" -> {
                    if (resp.aqi != null) {
                        val aqiVal = resp.aqi
                        val style = AqiClassification.getStyle(aqiVal)
                        Result.success(
                            AqiUiState.Success(
                                aqi = aqiVal,
                                category = resp.category ?: style.category,
                                dominantPollutant = resp.dominantPollutant ?: "PM2.5",
                                stationName = resp.stationName ?: "Monitoring Station",
                                stationLatitude = resp.stationLatitude,
                                stationLongitude = resp.stationLongitude,
                                distanceKm = resp.distanceKm,
                                pm25 = resp.pm25,
                                pm10 = resp.pm10,
                                co = resp.co,
                                no2 = resp.no2,
                                o3 = resp.o3,
                                timeString = resp.timeString,
                                source = resp.source ?: "CPCB",
                                isCached = false,
                                lastUpdatedText = "LIVE"
                            )
                        )
                    } else {
                        Result.success(AqiUiState.NoNearby(resp.message ?: "No nearby air quality monitoring station was found for your current location.", resp.distanceKm))
                    }
                }
                "NO_NEARBY_AQI_STATION" -> {
                    Result.success(AqiUiState.NoNearby(resp.message ?: "No nearby air quality monitoring station was found for your current location.", resp.distanceKm))
                }
                else -> {
                    Result.success(AqiUiState.Error(resp.message ?: "Air quality data unavailable"))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.success(AqiUiState.Error("AQI server is waking up (free hosting sleeps when idle) — please retry in a few seconds."))
        } catch (e: HttpException) {
            Result.success(AqiUiState.Error("AQI server returned an error (HTTP ${e.code()})."))
        } catch (e: IOException) {
            Result.success(AqiUiState.Error("Couldn't reach the AQI server. Check your internet connection."))
        } catch (e: Exception) {
            Result.success(AqiUiState.Error(e.localizedMessage ?: "Air quality unavailable"))
        }
    }
}
