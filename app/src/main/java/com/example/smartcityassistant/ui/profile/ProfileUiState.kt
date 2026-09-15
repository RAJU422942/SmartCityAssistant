package com.example.smartcityassistant.ui.profile

data class EmergencyContact(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "",
    val phone: String = ""
)

data class UserProfile(
    val name: String = "raju kumar sah",
    val email: String = "rk1821762@gmail.com",
    val phoneNumber: String = "7295907951",
    val city: String = "madhubani",
    val state: String = "Bihar",
    val preferredCity: String = "madhubani",
    val language: String = "English",
    val notificationsEnabled: Boolean = true,
    val locationAccessEnabled: Boolean = true,
    val avatarUri: String? = null,
    val emergencyContacts: List<EmergencyContact> = listOf(
        EmergencyContact(name = "Police Control Room", phone = "112"),
        EmergencyContact(name = "Ambulance Services", phone = "108"),
        EmergencyContact(name = "Municipal Helpline", phone = "1916")
    )
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val profile: UserProfile,
        val isEditing: Boolean = false,
        val showLogoutDialog: Boolean = false,
        val activeDialog: ProfileDialogType? = null
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

enum class ProfileDialogType {
    LANGUAGE, PRIVACY, ABOUT, EMERGENCY_CONTACTS, SAFETY_GUIDELINES
}
