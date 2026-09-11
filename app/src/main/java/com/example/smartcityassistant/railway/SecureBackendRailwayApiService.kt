package com.example.smartcityassistant.railway

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
}
