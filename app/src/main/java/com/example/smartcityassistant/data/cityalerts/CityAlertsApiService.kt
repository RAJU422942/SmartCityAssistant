package com.example.smartcityassistant.data.cityalerts

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CityAlertsApiService {
    @GET("api/v1/alerts")
    suspend fun getAlerts(
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
        @Query("city") city: String?,
        @Query("district") district: String?,
        @Query("category") category: String?,
    ): CityAlertsResponse
}

object CityAlertsClient {
    private const val BASE_URL = "https://smart-city-assistant-railway-backend.onrender.com/"
    val service: CityAlertsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CityAlertsApiService::class.java)
    }
}
