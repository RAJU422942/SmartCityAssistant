package com.example.smartcityassistant.data.government

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface GovernmentApiService {
    @GET("api/v1/government/schemes")
    suspend fun getSchemes(): List<GovScheme>

    @GET("api/v1/government/benefits")
    suspend fun getBenefits(): List<WelfareBenefit>

    @GET("api/v1/government/notices")
    suspend fun getNotices(): List<GovNotice>

    @GET("api/v1/government/helplines")
    suspend fun getHelplines(): List<GovHelpline>
}

object GovernmentClient {
    private const val BASE_URL = "https://smart-city-assistant-railway-backend.onrender.com/"
    val service: GovernmentApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GovernmentApiService::class.java)
    }
}
