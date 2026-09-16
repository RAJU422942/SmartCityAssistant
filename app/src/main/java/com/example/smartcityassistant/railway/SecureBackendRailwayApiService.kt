package com.example.smartcityassistant.railway

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SecureBackendRailwayApiService {
    @GET("api/v1/railway/live/{trainNumber}")
    suspend fun getLiveStatus(
        @Path("trainNumber") trainNumber: String,
        @Query("date") date: String
    ): BackendLiveStatusResponse

    @GET("api/v1/railway/pnr/{pnr}")
    suspend fun getPnrStatus(
        @Path("pnr") pnr: String
    ): BackendPnrResponse

    @GET("api/v1/railway/availability")
    suspend fun getSeatAvailability(
        @Query("train") trainNumber: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("date") date: String,
        @Query("class") trainClass: String,
        @Query("quota") quota: String
    ): BackendAvailabilityResponse

    @GET("api/v1/trains/between/{from}/{to}")
    suspend fun getTrainsBetween(
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("date") date: String
    ): List<BackendTrainResponse>

    @GET("api/v1/railway/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): WeatherResponseDto
}

object SecureBackendClient {
    private const val BASE_URL = "https://smart-city-assistant-railway-backend.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: SecureBackendRailwayApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SecureBackendRailwayApiService::class.java)
    }
}
