package com.example.smartcityassistant.ui.cityalerts

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcityassistant.TransportService
import com.example.smartcityassistant.aqi.AqiRepository
import com.example.smartcityassistant.aqi.AqiUiState
import com.example.smartcityassistant.data.cityalerts.CityAlert
import com.example.smartcityassistant.data.cityalerts.CityAlertsRepository
import com.example.smartcityassistant.weather.WeatherRepository
import com.example.smartcityassistant.weather.WeatherUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class CityAlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val cityAlertsRepository = CityAlertsRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val aqiRepository = AqiRepository(application)

    private val _uiState = MutableStateFlow<CityAlertsUiState>(CityAlertsUiState.Unselected)
    val uiState: StateFlow<CityAlertsUiState> = _uiState.asStateFlow()

    private var currentSelectedLocation: SelectedLocation? = null
    private var currentCategory: String? = null

    fun selectLocationAndLoad(location: SelectedLocation, category: String? = currentCategory) {
        currentSelectedLocation = location
        currentCategory = category
        _uiState.value = CityAlertsUiState.Loading

        viewModelScope.launch {
            val weatherDeferred = async { weatherRepository.fetchWeather(location.latitude, location.longitude) }
            val aqiDeferred = async { aqiRepository.fetchAqi(location.latitude, location.longitude) }
            val alertsDeferred = async { cityAlertsRepository.getAlerts(lat = location.latitude, lon = location.longitude, city = location.name, category = "DISASTER") }

            val weatherState = weatherDeferred.await()
            val aqiResult = aqiDeferred.await()
            val alertsResult = alertsDeferred.await()

            val aqiState = aqiResult.getOrElse { AqiUiState.Error("Air quality unavailable") }
            val disasterAlerts = alertsResult.getOrNull()?.alerts ?: emptyList()

            // Generate Weather Advisories from weatherState
            val weatherAdvisories = mutableListOf<CityAlert>()
            if (weatherState is WeatherUiState.Success) {
                val temp = weatherState.temperature
                val wind = weatherState.windSpeed
                val condition = weatherState.condition.lowercase()
                val precipProb = weatherState.hourly.firstOrNull()?.precipitationProbability ?: 0.0

                if (precipProb >= 70.0 || weatherState.condition.contains("Rain", true)) {
                    weatherAdvisories.add(
                        CityAlert(
                            id = "advisory_rain_${System.currentTimeMillis()}",
                            type = "WEATHER",
                            title = "Weather Advisory: Heavy Rain",
                            description = "Rainfall and high precipitation probability detected (${precipProb.toInt()}%). Exercise caution while commuting.",
                            category = "WEATHER",
                            severity = "MODERATE",
                            status = "ACTIVE",
                            startTime = null,
                            endTime = null,
                            lastUpdated = weatherState.lastUpdatedText,
                            locationName = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            sourceName = "MET Norway",
                            sourceUrl = "https://www.met.no",
                            isVerified = true
                        )
                    )
                }
                if (condition.contains("thunder", true)) {
                    weatherAdvisories.add(
                        CityAlert(
                            id = "advisory_thunder_${System.currentTimeMillis()}",
                            type = "WEATHER",
                            title = "Weather Advisory: Thunderstorm",
                            description = "Thunderstorm conditions detected in forecast. Stay indoors and avoid open areas.",
                            category = "WEATHER",
                            severity = "HIGH",
                            status = "ACTIVE",
                            startTime = null,
                            endTime = null,
                            lastUpdated = weatherState.lastUpdatedText,
                            locationName = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            sourceName = "MET Norway",
                            sourceUrl = "https://www.met.no",
                            isVerified = true
                        )
                    )
                }
                if (wind >= 40.0) {
                    weatherAdvisories.add(
                        CityAlert(
                            id = "advisory_wind_${System.currentTimeMillis()}",
                            type = "WEATHER",
                            title = "Weather Advisory: Strong Wind",
                            description = "Strong wind speeds detected (${wind.toInt()} km/h). Secure loose outdoor items.",
                            category = "WEATHER",
                            severity = if (wind >= 60.0) "HIGH" else "MODERATE",
                            status = "ACTIVE",
                            startTime = null,
                            endTime = null,
                            lastUpdated = weatherState.lastUpdatedText,
                            locationName = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            sourceName = "MET Norway",
                            sourceUrl = "https://www.met.no",
                            isVerified = true
                        )
                    )
                }
                if (temp >= 40.0) {
                    weatherAdvisories.add(
                        CityAlert(
                            id = "advisory_heat_${System.currentTimeMillis()}",
                            type = "WEATHER",
                            title = "Weather Advisory: Extreme Heat",
                            description = "Temperature has reached ${temp.toInt()}°C. Stay hydrated and avoid prolonged outdoor sun exposure.",
                            category = "WEATHER",
                            severity = "HIGH",
                            status = "ACTIVE",
                            startTime = null,
                            endTime = null,
                            lastUpdated = weatherState.lastUpdatedText,
                            locationName = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            sourceName = "MET Norway",
                            sourceUrl = "https://www.met.no",
                            isVerified = true
                        )
                    )
                }
                if (temp <= 5.0) {
                    weatherAdvisories.add(
                        CityAlert(
                            id = "advisory_cold_${System.currentTimeMillis()}",
                            type = "WEATHER",
                            title = "Weather Advisory: Extreme Cold",
                            description = "Temperature has dropped to ${temp.toInt()}°C. Wear appropriate warm clothing.",
                            category = "WEATHER",
                            severity = "MODERATE",
                            status = "ACTIVE",
                            startTime = null,
                            endTime = null,
                            lastUpdated = weatherState.lastUpdatedText,
                            locationName = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            sourceName = "MET Norway",
                            sourceUrl = "https://www.met.no",
                            isVerified = true
                        )
                    )
                }
            }

            // Generate AQI Advisory from aqiState
            val aqiAdvisories = mutableListOf<CityAlert>()
            if (aqiState is AqiUiState.Success) {
                val aqiVal = aqiState.aqi
                val aqiDesc = when {
                    aqiVal <= 50 -> "No significant air quality concern."
                    aqiVal <= 100 -> "Air quality is acceptable, but unusually sensitive people may want to reduce prolonged outdoor exposure."
                    aqiVal <= 150 -> "Air quality is acceptable for most, but sensitive groups may experience minor health effects."
                    aqiVal <= 200 -> "Members of sensitive groups may experience health effects. The general public is less likely to be affected."
                    aqiVal <= 300 -> "Everyone may begin to experience health effects; members of sensitive groups may experience more serious health effects."
                    else -> "Health warning of emergency conditions: everyone is more likely to be affected."
                }
                aqiAdvisories.add(
                    CityAlert(
                        id = "advisory_aqi_${System.currentTimeMillis()}",
                        type = "AIR_QUALITY",
                        title = "Air Quality Advisory: ${aqiState.category}",
                        description = "AQI is $aqiVal (${aqiState.category}). $aqiDesc",
                        category = "AIR_QUALITY",
                        severity = if (aqiVal > 200) "HIGH" else if (aqiVal > 100) "MODERATE" else "LOW",
                        status = "ACTIVE",
                        startTime = null,
                        endTime = null,
                        lastUpdated = aqiState.lastUpdatedText,
                        locationName = location.name,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        sourceName = aqiState.sourceLabel ?: "Open-Meteo • CAMS",
                        sourceUrl = "https://aqicn.org",
                        isVerified = true
                    )
                )
            }

            _uiState.value = CityAlertsUiState.Success(
                selectedCategory = category,
                selectedLocation = location,
                weatherState = weatherState,
                aqiState = aqiState,
                disasterAlerts = disasterAlerts,
                weatherAdvisories = weatherAdvisories,
                aqiAdvisories = aqiAdvisories,
                updatedAt = "Just now"
            )
        }
    }

    fun setCategory(category: String?) {
        currentCategory = category
        val currentState = _uiState.value
        if (currentState is CityAlertsUiState.Success) {
            selectLocationAndLoad(currentState.selectedLocation, category)
        } else if (currentSelectedLocation != null) {
            selectLocationAndLoad(currentSelectedLocation!!, category)
        }
    }

    fun fetchCurrentLocationAndLoad(lat: Double, lon: Double) {
        viewModelScope.launch {
            val address = try {
                TransportService.getAddressFromLocation(getApplication(), lat, lon)
            } catch (e: Exception) {
                "Current GPS Location"
            }
            val location = SelectedLocation(
                name = address,
                latitude = lat,
                longitude = lon,
                source = LocationSource.CURRENT_LOCATION
            )
            selectLocationAndLoad(location, currentCategory)
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val lat = addr.latitude
                    val lon = addr.longitude
                    val name = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: query
                    val state = addr.adminArea ?: ""
                    val displayName = if (state.isNotBlank() && !name.contains(state)) "$name, $state" else name
                    
                    val location = SelectedLocation(
                        name = displayName,
                        latitude = lat,
                        longitude = lon,
                        source = LocationSource.SEARCH
                    )
                    selectLocationAndLoad(location, currentCategory)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
