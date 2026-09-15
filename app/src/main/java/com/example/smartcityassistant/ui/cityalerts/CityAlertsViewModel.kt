package com.example.smartcityassistant.ui.cityalerts

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcityassistant.TransportService
import com.example.smartcityassistant.data.cityalerts.CityAlertsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class CityAlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CityAlertsRepository(application)
    private val _uiState = MutableStateFlow<CityAlertsUiState>(CityAlertsUiState.Loading)
    val uiState: StateFlow<CityAlertsUiState> = _uiState.asStateFlow()

    private var currentLat: Double = 25.6022
    private var currentLon: Double = 85.1376
    private var currentLocationName: String = "Madhuban, Bihar"
    private var isCustom: Boolean = false
    private var currentCategory: String? = null

    init {
        loadAlerts(currentLat, currentLon, currentLocationName, false, null)
    }

    fun loadAlerts(
        lat: Double = currentLat,
        lon: Double = currentLon,
        locName: String = currentLocationName,
        custom: Boolean = isCustom,
        category: String? = currentCategory
    ) {
        currentLat = lat
        currentLon = lon
        currentLocationName = locName
        isCustom = custom
        currentCategory = category

        _uiState.value = CityAlertsUiState.Loading
        viewModelScope.launch {
            val result = repository.getAlerts(lat = lat, lon = lon, city = locName, category = category)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = CityAlertsUiState.Success(
                        alerts = response.alerts,
                        selectedCategory = category,
                        locationName = locName,
                        isUsingCustomLocation = custom,
                        latitude = lat,
                        longitude = lon
                    )
                },
                onFailure = { err ->
                    _uiState.value = CityAlertsUiState.Error(err.localizedMessage ?: "City alerts unavailable")
                }
            )
        }
    }

    fun fetchCurrentLocationAndLoad(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val address = TransportService.getAddressFromLocation(getApplication(), lat, lon)
                loadAlerts(lat, lon, address, false, currentCategory)
            } catch (e: Exception) {
                loadAlerts(lat, lon, "Current Location", false, currentCategory)
            }
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
                    loadAlerts(lat, lon, displayName, true, currentCategory)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun resetToCurrentLocation(lat: Double, lon: Double, locName: String) {
        loadAlerts(lat, lon, locName, false, currentCategory)
    }
}
