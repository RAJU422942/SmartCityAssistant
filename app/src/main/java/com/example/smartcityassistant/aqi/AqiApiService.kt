package com.example.smartcityassistant.aqi

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AqiApiService {
    @GET("api/v1/railway/aqi")
    suspend fun getAqi(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): AqiResponseDto
}

object AqiClient {
    private const val BASE_URL = "https://smart-city-assistant-railway-backend.onrender.com/"
    val service: AqiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AqiApiService::class.java)
    }
}
