package com.example.smartcityassistant.railway

interface RailwayRepository {
    suspend fun getStations(query: String = ""): List<RailwayStation>
    suspend fun searchTrains(from: String, to: String): List<Train>
    suspend fun findTrains(query: String): List<Train>
    suspend fun getTrainRoute(trainNumber: String): TrainRoute
    suspend fun getLiveStatus(trainNumber: String): TrainLiveStatus
    suspend fun getPnrStatus(pnr: String): Result<BackendPnrResponse>
    suspend fun getSeatAvailability(train: String, from: String, to: String, date: String, cls: String, quota: String): Result<BackendAvailabilityResponse>
}
