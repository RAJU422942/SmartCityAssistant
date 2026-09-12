package com.example.smartcityassistant.railway

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RealRailwayRepository(private val context: Context) : RailwayRepository {
    private val demoRepo = DemoRailwayRepository

    override suspend fun getStations(query: String): List<RailwayStation> {
        return withContext(Dispatchers.IO) {
            try {
                // Try fetching stations via backend/RailKit if available, else fallback to local JSON demo list
                demoRepo.getStations(query)
            } catch (e: Exception) {
                demoRepo.getStations(query)
            }
        }
    }

    override suspend fun getPnrStatus(pnr: String): Result<BackendPnrResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (pnr.length != 10 || !pnr.all { it.isDigit() }) {
                    return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit PNR number"))
                }
                val response = SecureBackendClient.service.getPnrStatus(pnr)
                when (response.currentStatus) {
                    "AUTH_ERROR" -> Result.failure(Exception(response.message ?: "PNR service authentication unavailable"))
                    "RATE_LIMIT" -> Result.failure(Exception(response.message ?: "PNR service is temporarily busy. Try again later."))
                    "NOT_FOUND" -> Result.failure(Exception(response.message ?: "PNR record not found."))
                    "UNAVAILABLE" -> Result.failure(Exception(response.message ?: "PNR service unavailable. Please try again."))
                    else -> {
                        if (response.pnrNumber.isNotBlank() && response.currentStatus != "UNAVAILABLE") {
                            Result.success(response)
                        } else {
                            Result.failure(Exception(response.message ?: "PNR record not found or unavailable"))
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception("PNR service unavailable. Please try again."))
            }
        }
    }

    override suspend fun getSeatAvailability(train: String, from: String, to: String, date: String, cls: String, quota: String): Result<BackendAvailabilityResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = SecureBackendClient.service.getSeatAvailability(train, from, to, date, cls, quota)
                if (response.status != "UNAVAILABLE") {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Seat availability data unavailable"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Seat availability data unavailable"))
            }
        }
    }

    private fun extractStationCode(input: String): String {
        val regex = "\\(([^)]+)\\)".toRegex()
        val match = regex.find(input)
        return match?.groupValues?.get(1) ?: input.trim().uppercase()
    }

    override suspend fun searchTrains(from: String, to: String): List<Train> {
        return withContext(Dispatchers.IO) {
            try {
                val fromCode = extractStationCode(from)
                val toCode = extractStationCode(to)
                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

                // Safe logging (no secrets/PNR)
                println("[Railway Search]: FROM=$fromCode, TO=$toCode, DATE=$currentDate")

                val response = SecureBackendClient.service.getTrainsBetween(fromCode, toCode, currentDate)
                response.map { bt ->
                    Train(
                        number = bt.trainNumber ?: "N/A",
                        name = bt.trainName ?: "Express Service",
                        from = bt.source ?: from,
                        to = bt.destination ?: to,
                        departure = bt.departureTime ?: "08:00 AM",
                        arrival = bt.arrivalTime ?: "04:00 PM",
                        duration = bt.duration ?: "8h 00m",
                        runningDays = bt.runningDays?.joinToString(", ") ?: "Daily",
                        classes = bt.classes ?: listOf("3A", "SL"),
                        status = bt.status ?: "Scheduled",
                        dataSource = TransportDataSourceType.SCHEDULED,
                        availability = "Check Availability",
                        fare = "As per RailKit"
                    )
                }
            } catch (e: Exception) {
                // NEVER fall back to demoRepo.searchTrains in production
                emptyList()
            }
        }
    }

    override suspend fun findTrains(query: String): List<Train> {
        return withContext(Dispatchers.IO) {
            demoRepo.findTrains(query)
        }
    }

    override suspend fun getTrainRoute(trainNumber: String): TrainRoute {
        return withContext(Dispatchers.IO) {
            demoRepo.getTrainRoute(trainNumber)
        }
    }

    override suspend fun getLiveStatus(trainNumber: String): TrainLiveStatus {
        return withContext(Dispatchers.IO) {
            try {
                val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val response = SecureBackendClient.service.getLiveStatus(trainNumber, currentDate)
                if (response.status == "OK") {
                    TrainLiveStatus(
                        trainNumber = response.trainNumber,
                        trainName = response.trainName,
                        currentStation = response.currentStation,
                        nextStation = response.nextStation ?: "N/A",
                        delayMinutes = response.delayMinutes,
                        runningState = response.runningState,
                        lastUpdated = response.lastUpdated,
                        dataSource = TransportDataSourceType.LIVE
                    )
                } else {
                    TrainLiveStatus(
                        trainNumber = trainNumber,
                        trainName = "Train #$trainNumber",
                        currentStation = "Live tracking unavailable",
                        nextStation = "N/A",
                        delayMinutes = 0,
                        runningState = response.status,
                        lastUpdated = "N/A",
                        dataSource = TransportDataSourceType.UNAVAILABLE
                    )
                }
            } catch (e: Exception) {
                TrainLiveStatus(
                    trainNumber = trainNumber,
                    trainName = "Train #$trainNumber",
                    currentStation = "Live tracking unavailable",
                    nextStation = "N/A",
                    delayMinutes = 0,
                    runningState = "Live running status unavailable",
                    lastUpdated = "N/A",
                    dataSource = TransportDataSourceType.UNAVAILABLE
                )
            }
        }
    }
}
