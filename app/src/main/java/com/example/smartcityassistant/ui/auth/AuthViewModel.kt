package com.example.smartcityassistant.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcityassistant.data.auth.AuthRepository
import com.example.smartcityassistant.data.auth.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: UserDto?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class SuccessMessage(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    init {
        checkExistingSession()
    }

    fun checkExistingSession() {
        val token = repository.getStoredToken()
        if (token != null) {
            _uiState.value = AuthUiState.Authenticated(null)
        } else {
            _uiState.value = AuthUiState.Idle
        }
    }

    private fun handleException(e: Exception, endpoint: String): String {
        return when (e) {
            is HttpException -> {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string() ?: "No error body"
                Log.e("AuthDebug", "HTTP Error $code on endpoint $endpoint. Body: $errorBody")
                when (code) {
                    400 -> "Please check your information."
                    401 -> "Incorrect username, email, phone number or password."
                    404 -> "Authentication endpoint not found. Please ensure the backend is deployed on Render."
                    409 -> "Username, email or mobile number is already registered."
                    500 -> "Server error. Please try again later."
                    else -> "Request failed (HTTP $code). Please try again."
                }
            }
            is SocketTimeoutException -> {
                Log.e("AuthDebug", "SocketTimeoutException on endpoint $endpoint: ${e.message}", e)
                "Server is taking too long to respond (Render cold start). Please try again."
            }
            is UnknownHostException -> {
                Log.e("AuthDebug", "UnknownHostException on endpoint $endpoint: ${e.message}", e)
                "Service is temporarily unavailable. Please check your network connection."
            }
            is ConnectException -> {
                Log.e("AuthDebug", "ConnectException on endpoint $endpoint: ${e.message}", e)
                "Unable to connect to server. Please check your internet connection."
            }
            is SSLException -> {
                Log.e("AuthDebug", "SSLException on endpoint $endpoint: ${e.message}", e)
                "Secure connection (TLS) failed. Please check date/time settings."
            }
            else -> {
                Log.e("AuthDebug", "Unknown exception on endpoint $endpoint: ${e.javaClass.simpleName} - ${e.message}", e)
                "Unable to connect. Please check your internet connection."
            }
        }
    }

    fun login(identifier: String, pass: String) {
        if (identifier.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your identifier and password.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = repository.login(identifier, pass)
                if (res.status == "SUCCESS") {
                    _currentUser.value = res.user
                    _uiState.value = AuthUiState.Authenticated(res.user)
                } else {
                    _uiState.value = AuthUiState.Error(res.message ?: "Incorrect username, email, phone number or password.")
                }
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/login")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        phone: String,
        pass: String,
        confirmPass: String
    ) {
        if (fullName.isBlank() || username.isBlank() || email.isBlank() || phone.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
            _uiState.value = AuthUiState.Error("All fields are required.")
            return
        }
        if (pass != confirmPass) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters long.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = repository.register(fullName, username, email, phone, pass, confirmPass)
                if (res.status == "SUCCESS") {
                    _currentUser.value = res.user
                    _uiState.value = AuthUiState.Authenticated(res.user)
                } else {
                    _uiState.value = AuthUiState.Error(res.message ?: "Registration failed.")
                }
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/register")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun forgotPassword(identifier: String) {
        if (identifier.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email, username, or phone number.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = repository.forgotPassword(identifier)
                _uiState.value = AuthUiState.SuccessMessage(res.message ?: "Password reset instructions sent.")
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/forgot-password")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun resetPassword(identifier: String, otp: String, newPass: String, confirmPass: String) {
        if (identifier.isBlank() || otp.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _uiState.value = AuthUiState.Error("All fields are required.")
            return
        }
        if (newPass != confirmPass) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val res = repository.resetPassword(identifier, otp, newPass, confirmPass)
                if (res.status == "SUCCESS") {
                    _uiState.value = AuthUiState.SuccessMessage("Password updated successfully. Please log in.")
                } else {
                    _uiState.value = AuthUiState.Error(res.message ?: "Password reset failed.")
                }
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/reset-password")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun sendEmailOtp() {
        viewModelScope.launch {
            try {
                repository.sendEmailOtp()
                _uiState.value = AuthUiState.SuccessMessage("Verification code sent to your email.")
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/send-email-otp")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun verifyEmail(otp: String) {
        viewModelScope.launch {
            try {
                val res = repository.verifyEmail(otp)
                if (res.status == "SUCCESS") {
                    _uiState.value = AuthUiState.SuccessMessage("Email verified successfully!")
                    _currentUser.value = _currentUser.value?.copy(emailVerified = true)
                } else {
                    _uiState.value = AuthUiState.Error(res.message ?: "Incorrect verification code.")
                }
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/verify-email")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun sendPhoneOtp() {
        viewModelScope.launch {
            try {
                repository.sendPhoneOtp()
                _uiState.value = AuthUiState.SuccessMessage("Verification code sent to your mobile.")
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/send-phone-otp")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun verifyPhone(otp: String) {
        viewModelScope.launch {
            try {
                val res = repository.verifyPhone(otp)
                if (res.status == "SUCCESS") {
                    _uiState.value = AuthUiState.SuccessMessage("Mobile number verified successfully!")
                    _currentUser.value = _currentUser.value?.copy(phoneVerified = true)
                } else {
                    _uiState.value = AuthUiState.Error(res.message ?: "Incorrect verification code.")
                }
            } catch (e: Exception) {
                val msg = handleException(e, "POST /api/v1/auth/verify-phone")
                _uiState.value = AuthUiState.Error(msg)
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.logout()
            } catch (_: Exception) {}
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
            onLoggedOut()
        }
    }
}
