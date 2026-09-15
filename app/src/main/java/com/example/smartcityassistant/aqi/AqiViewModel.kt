package com.example.smartcityassistant.aqi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AqiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AqiRepository(application)
    private val _uiState = MutableStateFlow<AqiUiState>(AqiUiState.Loading)
    val uiState: StateFlow<AqiUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadAqi(lat: Double, lon: Double) {
        loadJob?.cancel()

        val cached = repository.getCachedAqi(lat, lon)
        if (cached != null) {
            val isSuccessFresh = (cached as? AqiUiState.Success)?.isCached == false
            if (isSuccessFresh) {
                _uiState.value = cached
                return
            } else {
                _uiState.value = cached
            }
        } else {
            if (_uiState.value !is AqiUiState.Success) {
                _uiState.value = AqiUiState.Loading
            }
        }

        loadJob = viewModelScope.launch {
            val result = repository.fetchAqi(lat, lon)
            result.fold(
                onSuccess = { state ->
                    if (state is AqiUiState.Success || _uiState.value !is AqiUiState.Success) {
                        _uiState.value = state
                    }
                },
                onFailure = { err ->
                    if (_uiState.value !is AqiUiState.Success) {
                        if (cached != null) {
                            _uiState.value = cached
                        } else {
                            _uiState.value = AqiUiState.Error(err.localizedMessage ?: "AQI unavailable")
                        }
                    }
                }
            )
        }
    }
}
