package com.example.smartcityassistant.railway

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class DirectionsResponse(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val legs: List<Leg>
)

data class Leg(
    val arrival_time: TimeInfo?,
    val departure_time: TimeInfo?,
    val duration: DurationInfo?,
    val distance: DistanceInfo?,
    val start_address: String,
    val end_address: String,
    val steps: List<Step>
)

data class TimeInfo(
    val text: String,
    val time_zone: String,
    val value: Long
)

data class DurationInfo(
    val text: String,
    val value: Long
)

data class DistanceInfo(
    val text: String,
    val value: Long
)

data class Step(
    val html_instructions: String,
    val travel_mode: String,
    val transit_details: TransitDetails?
)

data class TransitDetails(
    val arrival_stop: Stop,
    val departure_stop: Stop,
    val headsign: String,
    val line: Line
)

data class Stop(
    val name: String,
    val location: LatLngLiteral
)

data class LatLngLiteral(
    val lat: Double,
    val lng: Double
)

data class Line(
    val name: String,
    val short_name: String,
    val agencies: List<TransitAgency>
)

data class TransitAgency(val name: String)

interface RailwayTransitApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "transit",
        @Query("transit_mode") transitMode: String = "rail",
        @Query("key") apiKey: String
    ): DirectionsResponse
}

object RailwayApiService {
    private const val BASE_URL = "https://maps.googleapis.com/"
    val service: RailwayTransitApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RailwayTransitApiService::class.java)
    }
}
