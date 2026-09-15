package com.example.smartcityassistant.aqi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AqiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AqiRepository(application)
    private val _uiState = MutableStateFlow<AqiUiState>(AqiUiState.Loading)
    val uiState: StateFlow<AqiUiState> = _uiState.asStateFlow()

    fun loadAqi(lat: Double, lon: Double) {
        // Pass lat/lon so a cached reading from a DIFFERENT location is never reused.
        val cached = repository.getCachedAqi(lat, lon)
        if (cached != null) {
            val isSuccessFresh = (cached as? AqiUiState.Success)?.isCached == false
            if (isSuccessFresh || cached is AqiUiState.NoNearby) {
                _uiState.value = cached
                if (isSuccessFresh) return
            } else {
                _uiState.value = cached
            }
        } else {
            _uiState.value = AqiUiState.Loading
        }

        viewModelScope.launch {
            val result = repository.fetchAqi(lat, lon)
            result.fold(
                onSuccess = { state ->
                    _uiState.value = state
                },
                onFailure = { err ->
                    if (cached != null) {
                        _uiState.value = cached
                    } else {
                        _uiState.value = AqiUiState.Error(err.localizedMessage ?: "AQI unavailable")
                    }
                },
            )
        }
    }
}
