package com.example.smartcityassistant.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Success(repository.getProfile()))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun updateProfile(updatedProfile: UserProfile) {
        repository.saveProfile(updatedProfile)
        _uiState.value = ProfileUiState.Success(updatedProfile, isEditing = false)
    }

    fun setEditing(editing: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(isEditing = editing)
        }
    }

    fun setShowLogoutDialog(show: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(showLogoutDialog = show)
        }
    }

    fun setActiveDialog(dialogType: ProfileDialogType?) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(activeDialog = dialogType)
        }
    }

    fun updateLanguage(language: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val updated = currentState.profile.copy(language = language)
            repository.saveProfile(updated)
            _uiState.value = ProfileUiState.Success(updated, activeDialog = null)
        }
    }

    fun updateAvatar(uri: String?) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val updated = currentState.profile.copy(avatarUri = uri)
            repository.saveProfile(updated)
            _uiState.value = ProfileUiState.Success(updated)
        }
    }

    fun addEmergencyContact(name: String, phone: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val list = currentState.profile.emergencyContacts.toMutableList()
            list.add(EmergencyContact(name = name, phone = phone))
            val updated = currentState.profile.copy(emergencyContacts = list)
            repository.saveProfile(updated)
            _uiState.value = ProfileUiState.Success(updated)
        }
    }

    fun removeEmergencyContact(contactId: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val list = currentState.profile.emergencyContacts.filterNot { it.id == contactId }
            val updated = currentState.profile.copy(emergencyContacts = list)
            repository.saveProfile(updated)
            _uiState.value = ProfileUiState.Success(updated)
        }
    }

    fun updatePreference(notifications: Boolean, location: Boolean, language: String, city: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            val updated = currentState.profile.copy(
                notificationsEnabled = notifications,
                locationAccessEnabled = location,
                language = language,
                preferredCity = city,
                city = city
            )
            repository.saveProfile(updated)
            _uiState.value = ProfileUiState.Success(updated)
        }
    }
}
