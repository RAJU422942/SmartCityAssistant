package com.example.smartcityassistant.railway

import android.content.Context
import com.example.smartcityassistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealRailwayRepository(private val context: Context) : RailwayRepository {
    private val demoRepo = DemoRailwayRepository

    override suspend fun getStations(query: String): List<RailwayStation> {
        return withContext(Dispatchers.IO) {
            demoRepo.getStations(query)
        }
    }

    override suspend fun getPnrStatus(pnr: String): Result<BackendPnrResponse> {
        return withContext(Dispatchers.IO) {
            try {
                demoRepo.getPnrStatus(pnr)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSeatAvailability(train: String, from: String, to: String, date: String, cls: String, quota: String): Result<BackendAvailabilityResponse> {
        return withContext(Dispatchers.IO) {
            try {
                demoRepo.getSeatAvailability(train, from, to, date, cls, quota)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchTrains(from: String, to: String): List<Train> {
        return withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.MAPS_API_KEY == "YOUR_API_KEY_HERE" || BuildConfig.MAPS_API_KEY.isEmpty()) {
                    return@withContext demoRepo.searchTrains(from, to)
                }
                
                val response = RailwayApiService.service.getDirections(
                    origin = from,
                    destination = to,
                    transitMode = "rail",
                    apiKey = BuildConfig.MAPS_API_KEY
                )

                if (response.status == "OK" && response.routes.isNotEmpty()) {
                    response.routes.mapIndexed { index, route ->
                        val leg = route.legs.firstOrNull()
                        val transit = leg?.steps?.find { it.travel_mode == "TRANSIT" && it.transit_details != null }?.transit_details
                        Train(
                            number = transit?.line?.short_name ?: "12${index}0${index}",
                            name = transit?.line?.name ?: "Express Service",
                            from = transit?.departure_stop?.name ?: from,
                            to = transit?.arrival_stop?.name ?: to,
                            departure = leg?.departure_time?.text ?: "08:00 AM",
                            arrival = leg?.arrival_time?.text ?: "04:00 PM",
                            duration = leg?.duration?.text ?: "8h 00m",
                            runningDays = "Daily",
                            classes = listOf("3A", "SL"),
                            status = "On Time",
                            dataSource = TransportDataSourceType.SCHEDULED,
                            availability = "Available",
                            fare = "₹350 - ₹950"
                        )
                    }
                } else {
                    demoRepo.searchTrains(from, to)
                }
            } catch (e: Exception) {
                demoRepo.searchTrains(from, to)
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
