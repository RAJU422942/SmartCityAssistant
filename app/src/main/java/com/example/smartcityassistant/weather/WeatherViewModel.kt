package com.example.smartcityassistant.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val repository = WeatherRepository(application)
    private var lastLat: Double? = null
    private var lastLon: Double? = null

    fun loadWeather(lat: Double, lon: Double, forceRefresh: Boolean = false) {
        lastLat = lat
        lastLon = lon
        viewModelScope.launch {
            if (!forceRefresh) {
                repository.getCached(lat, lon)?.let {
                    _uiState.value = it
                    return@launch
                }
            }
            _uiState.value = WeatherUiState.Loading
            _uiState.value = repository.fetchWeather(lat, lon)
        }
    }

    fun retry() {
        lastLat?.let { lat ->
            lastLon?.let { lon ->
                loadWeather(lat, lon, forceRefresh = true)
            }
        } ?: run {
            _uiState.value = WeatherUiState.Error("No location available")
        }
    }
}
