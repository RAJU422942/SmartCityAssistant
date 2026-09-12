package com.example.smartcityassistant.railway

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

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
}

object SecureBackendClient {
    private const val BASE_URL = "https://smart-city-assistant-railway-backend.onrender.com/"
    val service: SecureBackendRailwayApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SecureBackendRailwayApiService::class.java)
    }
}
