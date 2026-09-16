package com.example.smartcityassistant.data.auth

import android.content.Context

class AuthRepository(context: Context) {
    private val tokenStorage = SecureTokenStorage(context)
    private val api = AuthClient.service

    init {
        AuthClient.authToken = tokenStorage.getToken()
    }

    fun getStoredToken(): String? = tokenStorage.getToken()

    suspend fun login(identifier: String, pass: String): AuthResponse {
        val res = api.login(mapOf("identifier" to identifier, "password" to pass))
        if (res.status == "SUCCESS" && res.token != null) {
            tokenStorage.saveToken(res.token)
            AuthClient.authToken = res.token
            res.user?.let {
                tokenStorage.saveUserSession(it.id, it.username, it.email, it.phone, it.fullName)
            }
        }
        return res
    }

    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        phone: String,
        pass: String,
        confirmPass: String
    ): AuthResponse {
        val res = api.register(
            mapOf(
                "fullName" to fullName,
                "username" to username,
                "email" to email,
                "phone" to phone,
                "password" to pass,
                "confirmPassword" to confirmPass
            )
        )
        if (res.status == "SUCCESS" && res.token != null) {
            tokenStorage.saveToken(res.token)
            AuthClient.authToken = res.token
            res.user?.let {
                tokenStorage.saveUserSession(it.id, it.username, it.email, it.phone, it.fullName)
            }
        }
        return res
    }

    suspend fun getMe(): UserDto? {
        return try {
            val res = api.getMe()
            if (res.status == "SUCCESS") res.user else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {}
        tokenStorage.clearUserSession()
        tokenStorage.clearToken()
        AuthClient.authToken = null
    }

    suspend fun forgotPassword(identifier: String): GenericApiResponse {
        return api.forgotPassword(mapOf("identifier" to identifier))
    }

    suspend fun resetPassword(identifier: String, otp: String, newPass: String, confirmPass: String): GenericApiResponse {
        return api.resetPassword(
            mapOf(
                "identifier" to identifier,
                "otp" to otp,
                "newPassword" to newPass,
                "confirmPassword" to confirmPass
            )
        )
    }

    suspend fun sendEmailOtp(): GenericApiResponse = api.sendEmailOtp()
    suspend fun verifyEmail(otp: String): GenericApiResponse = api.verifyEmail(mapOf("otp" to otp))

    suspend fun sendPhoneOtp(): GenericApiResponse = api.sendPhoneOtp()
    suspend fun verifyPhone(otp: String): GenericApiResponse = api.verifyPhone(mapOf("otp" to otp))
}
