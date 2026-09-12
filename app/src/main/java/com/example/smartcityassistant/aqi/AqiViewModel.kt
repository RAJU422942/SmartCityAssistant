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
        val cached = repository.getCachedAqi()
        if ((cached != null) && (!cached.isCached)) {
            _uiState.value = cached
            return
        }

        if (cached != null) {
            _uiState.value = cached
        } else {
            _uiState.value = AqiUiState.Loading
        }

        viewModelScope.launch {
            val result = repository.fetchAqi(lat, lon)
            result.fold(
                onSuccess = { successState ->
                    _uiState.value = successState
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
