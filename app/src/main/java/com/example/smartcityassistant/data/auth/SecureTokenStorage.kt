package com.example.smartcityassistant.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStorage(context: Context) {
    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "smart_city_secure_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("smart_city_auth_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun clearToken() {
        prefs.edit().remove("auth_token").apply()
    }

    fun saveUserSession(userId: Long, username: String, email: String, phone: String, fullName: String) {
        prefs.edit()
            .putLong("user_id", userId)
            .putString("username", username)
            .putString("email", email)
            .putString("phone", phone)
            .putString("full_name", fullName)
            .apply()
    }

    fun clearUserSession() {
        prefs.edit()
            .remove("user_id")
            .remove("username")
            .remove("email")
            .remove("phone")
            .remove("full_name")
            .remove("auth_token")
            .apply()
    }
}
