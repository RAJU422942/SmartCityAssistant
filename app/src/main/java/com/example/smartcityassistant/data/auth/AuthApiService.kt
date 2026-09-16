package com.example.smartcityassistant.data.auth

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: Map<String, String>): AuthResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(): GenericApiResponse

    @GET("api/v1/auth/me")
    suspend fun getMe(): Map<String, Any>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: Map<String, String>): GenericApiResponse

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: Map<String, String>): GenericApiResponse

    @POST("api/v1/auth/send-email-otp")
    suspend fun sendEmailOtp(): GenericApiResponse

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: Map<String, String>): GenericApiResponse

    @POST("api/v1/auth/send-phone-otp")
    suspend fun sendPhoneOtp(): GenericApiResponse

    @POST("api/v1/auth/verify-phone")
    suspend fun verifyPhone(@Body request: Map<String, String>): GenericApiResponse
}

object AuthClient {
    private const val BASE_URL = "https://smart-city-assistant-railway-backend.onrender.com/"

    var authToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        authToken?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}
