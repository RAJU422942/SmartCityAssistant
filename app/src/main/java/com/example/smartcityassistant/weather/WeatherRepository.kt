package com.example.smartcityassistant.weather

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.smartcityassistant.railway.SecureBackendClient
import com.example.smartcityassistant.railway.WeatherResponseDto
import com.google.gson.Gson
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.math.abs
import retrofit2.HttpException

class WeatherRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_city_weather_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DATA = "cached_weather_json"
        private const val KEY_TIMESTAMP = "cached_weather_timestamp"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val LOCATION_MATCH_THRESHOLD = 0.05
    }

    data class CachedWeather(
        val response: WeatherResponseDto,
        val timestamp: Long,
        val lat: Double,
        val lon: Double
    )

    fun getCached(lat: Double, lon: Double): WeatherUiState? {
        val json = prefs.getString(KEY_DATA, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val ageMs = System.currentTimeMillis() - timestamp
        val isExpired = ageMs > CACHE_DURATION_MS

        return try {
            val cached = gson.fromJson(json, CachedWeather::class.java)
            val resp = cached.response

            if (resp.status != "OK" || resp.temperature == null) {
                prefs.edit { clear() }
                return null
            }

            val sameLocation = abs(cached.lat - lat) < LOCATION_MATCH_THRESHOLD &&
                    abs(cached.lon - lon) < LOCATION_MATCH_THRESHOLD
            if (!sameLocation) return null

            val minAgo = (ageMs / 60000).coerceAtLeast(1)
            val updatedText = if (isExpired) "CACHED • $minAgo min ago" else "LIVE • $minAgo min ago"

            WeatherUiState.Success(
                temperature = resp.temperature,
                apparentTemperature = resp.apparentTemperature ?: resp.temperature,
                humidity = resp.humidity ?: 50,
                windSpeed = resp.windSpeed ?: 10.0,
                condition = resp.condition ?: "Partly Cloudy",
                weatherCode = resp.weatherCode ?: 2,
                sunrise = resp.sunrise ?: "06:00 AM",
                sunset = resp.sunset ?: "06:30 PM",
                isCached = isExpired,
                lastUpdatedText = updatedText
            )
        } catch (_: Exception) {
            prefs.edit { clear() }
            null
        }
    }

    private fun saveCache(response: WeatherResponseDto, lat: Double, lon: Double) {
        try {
            if (response.status == "OK" && response.temperature != null) {
                val cached = CachedWeather(response, System.currentTimeMillis(), lat, lon)
                prefs.edit {
                    putString(KEY_DATA, gson.toJson(cached))
                    putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                }
            } else {
                prefs.edit { clear() }
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherUiState {
        return try {
            val response = SecureBackendClient.service.getWeather(lat, lon)
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
                    isCached = false,
                    lastUpdatedText = "LIVE"
                )
            } else {
                WeatherUiState.Error(response.message ?: "Weather unavailable")
            }
        } catch (e: SocketTimeoutException) {
            WeatherUiState.Error("Weather server is waking up — please retry.")
        } catch (e: HttpException) {
            WeatherUiState.Error("Server error (HTTP ${e.code()}).")
        } catch (e: IOException) {
            WeatherUiState.Error("Network error. Check connection.")
        } catch (e: Exception) {
            WeatherUiState.Error(e.localizedMessage ?: "Weather unavailable")
        }
    }
}
