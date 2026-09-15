package com.example.smartcityassistant.data.profile

import android.content.Context
import android.content.SharedPreferences
import com.example.smartcityassistant.ui.profile.UserProfile
import com.google.gson.Gson

class ProfileRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_city_profile_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PROFILE = "user_profile_data"
    }

    fun getProfile(): UserProfile {
        val json = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            gson.fromJson(json, UserProfile::class.java) ?: UserProfile()
        } catch (e: Exception) {
            UserProfile()
        }
    }

    fun saveProfile(profile: UserProfile) {
        try {
            prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply()
        } catch (e: Exception) {
        }
    }
}
