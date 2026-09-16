package com.example.smartcityassistant.weather

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.smartcityassistant.railway.SecureBackendClient
import com.example.smartcityassistant.railway.WeatherResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Locale
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class WeatherRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_city_weather_cache_v2", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "WeatherRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val inFlightMutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, Deferred<WeatherUiState>>()

    data class CachedWeather(
        val response: WeatherResponseDto,
        val timestamp: Long,
        val lat: Double,
        val lon: Double
    )

    private fun getNormKey(lat: Double, lon: Double): String {
        return String.format(Locale.US, "%.2f_%.2f", lat, lon)
    }

    private fun getCacheKeyData(lat: Double, lon: Double): String = "cached_weather_json_${getNormKey(lat, lon)}"
    private fun getCacheKeyTimestamp(lat: Double, lon: Double): String = "cached_weather_timestamp_${getNormKey(lat, lon)}"

    fun getCached(lat: Double, lon: Double): WeatherUiState? {
        val normKey = getNormKey(lat, lon)
        val jsonKey = getCacheKeyData(lat, lon)
        val timeKey = getCacheKeyTimestamp(lat, lon)

        val json = prefs.getString(jsonKey, null) ?: return null
        val timestamp = prefs.getLong(timeKey, 0L)
        val ageMs = System.currentTimeMillis() - timestamp
        val isExpired = ageMs > CACHE_DURATION_MS

        return try {
            val cached = gson.fromJson(json, CachedWeather::class.java)
            val resp = cached.response

            if (resp.status != "OK" || resp.temperature == null || resp.forecast.isNullOrEmpty() || resp.hourly.isNullOrEmpty()) {
                prefs.edit { remove(jsonKey); remove(timeKey) }
                return null
            }

            val minAgo = (ageMs / 60000).coerceAtLeast(1)
            val updatedText = if (isExpired) "CACHED • $minAgo min ago" else "LIVE • $minAgo min ago"
            Log.d(TAG, "[ANDROID WEATHER CACHE] CACHE HIT sunrise=${resp.sunrise} sunset=${resp.sunset}")

            WeatherUiState.Success(
                temperature = resp.temperature,
                apparentTemperature = resp.apparentTemperature ?: resp.temperature,
                humidity = resp.humidity ?: 50,
                windSpeed = resp.windSpeed ?: 10.0,
                condition = resp.condition ?: "Partly Cloudy",
                weatherCode = resp.weatherCode ?: 2,
                sunrise = resp.sunrise ?: "06:00 AM",
                sunset = resp.sunset ?: "06:30 PM",
                forecast = resp.forecast,
                hourly = resp.hourly,
                isCached = isExpired,
                lastUpdatedText = updatedText
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cached weather for $normKey", e)
            prefs.edit { remove(jsonKey); remove(timeKey) }
            null
        }
    }

    private fun saveCache(response: WeatherResponseDto, lat: Double, lon: Double) {
        try {
            if (response.status == "OK" && response.temperature != null) {
                val cached = CachedWeather(response, System.currentTimeMillis(), lat, lon)
                val jsonKey = getCacheKeyData(lat, lon)
                val timeKey = getCacheKeyTimestamp(lat, lon)
                prefs.edit {
                    putString(jsonKey, gson.toJson(cached))
                    putLong(timeKey, System.currentTimeMillis())
                }
                Log.d(TAG, "[ANDROID WEATHER CACHE SAVE] sunrise=${response.sunrise} sunset=${response.sunset}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving weather cache", e)
        }
    }

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherUiState {
        val normKey = getNormKey(lat, lon)
        Log.d(TAG, "[WEATHER REQUEST] lat=$lat lon=$lon normKey=$normKey")

        getCached(lat, lon)?.let { return it }

        val deferred = inFlightMutex.withLock {
            inFlightRequests[normKey]?.let {
                Log.d(TAG, "[WEATHER DEDUPLICATE] Reusing active in-flight request for $normKey")
                return@withLock it
            }

            val newDeferred = CoroutineScope(Dispatchers.IO).async {
                try {
                    val response = SecureBackendClient.service.getWeather(lat, lon)
                    Log.d(TAG, "[ANDROID WEATHER NETWORK] NETWORK RESPONSE sunrise=${response.sunrise} sunset=${response.sunset}")
                    if (response.status == "OK" && response.temperature != null) {
                        saveCache(response, lat, lon)
                        WeatherUiState.Success(
                            temperature = response.temperature,
                            apparentTemperature = response.apparentTemperature ?: response.temperature,
                            humidity = response.humidity ?: 50,
                            windSpeed = response.windSpeed ?: 10.0,
                            condition = response.condition ?: "Partly Cloudy",
                            weatherCode = response.weatherCode ?: 2,
                            sunrise = response.sunrise ?: "06:00 AM",
                            sunset = response.sunset ?: "06:30 PM",
                            forecast = response.forecast ?: emptyList(),
                            hourly = response.hourly ?: emptyList(),
                            isCached = false,
                            lastUpdatedText = "LIVE"
                        )
                    } else {
                        Log.w(TAG, "[WEATHER ERROR] status=${response.status} message=${response.message}")
                        WeatherUiState.Error(response.message ?: "Weather unavailable")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG, "[WEATHER ERROR] timeout", e)
                    WeatherUiState.Error("Weather server is waking up — please retry.")
                } catch (e: HttpException) {
                    Log.e(TAG, "[WEATHER ERROR] http error ${e.code()}", e)
                    WeatherUiState.Error("Server error (HTTP ${e.code()}).")
                } catch (e: IOException) {
                    Log.e(TAG, "[WEATHER ERROR] network io error", e)
                    WeatherUiState.Error("Network error. Check connection.")
                } catch (e: Exception) {
                    Log.e(TAG, "[WEATHER ERROR] unknown error", e)
                    WeatherUiState.Error(e.localizedMessage ?: "Weather unavailable")
                } finally {
                    inFlightMutex.withLock {
                        inFlightRequests.remove(normKey)
                    }
                }
            }
            inFlightRequests[normKey] = newDeferred
            newDeferred
        }
        return deferred.await()
    }
}
