package com.example.smartcityassistant.data.auth

data class AuthResponse(
    val status: String,
    val message: String?,
    val token: String?,
    val user: UserDto?
)

data class MeResponse(
    val status: String,
    val message: String?,
    val user: UserDto?
)

data class UserDto(
    val id: Long,
    val fullName: String,
    val username: String,
    val email: String,
    val phone: String,
    val emailVerified: Boolean,
    val phoneVerified: Boolean,
    val createdAt: String?
)

data class GenericApiResponse(
    val status: String,
    val message: String?
)
