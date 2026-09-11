package com.example.smartcityassistant.railway

import android.content.Context
import kotlinx.coroutines.delay

object DemoRailwayRepository : RailwayRepository {
    private var cachedStations: List<RailwayStation>? = null
    private var cachedTrains: List<Train>? = null

    private fun getStationsList(context: Context? = null): List<RailwayStation> {
        if (cachedStations == null && context != null) {
            cachedStations = RailwayJsonDataSource.loadStations(context)
        }
        return cachedStations ?: listOf(
            RailwayStation("s1", "PNBE", "Patna Junction", "Patna", 25.6022, 85.1376),
            RailwayStation("s2", "NDLS", "New Delhi", "New Delhi", 28.6429, 77.2197),
            RailwayStation("s3", "BSB", "Varanasi Junction", "Varanasi", 25.3216, 82.9873),
            RailwayStation("s4", "DNR", "Danapur", "Patna", 25.6333, 85.0333),
            RailwayStation("s5", "GAYA", "Gaya Junction", "Gaya", 24.7914, 85.0002),
            RailwayStation("s6", "MFP", "Muzaffarpur Junction", "Muzaffarpur", 26.1209, 85.3910),
            RailwayStation("s7", "HWH", "Howrah Junction", "Kolkata", 22.5833, 88.3417),
            RailwayStation("s8", "BCT", "Mumbai Central", "Mumbai", 18.9690, 72.8218)
        )
    }

    private fun getTrainsList(context: Context? = null): List<Train> {
        if (cachedTrains == null && context != null) {
            cachedTrains = RailwayJsonDataSource.loadTrains(context)
        }
        return cachedTrains ?: listOf(
            Train(
                number = "12393",
                name = "Sampoorna Kranti Express",
                from = "Patna Junction (PNBE)",
                to = "New Delhi (NDLS)",
                departure = "05:15 PM",
                arrival = "06:55 AM",
                duration = "13h 40m",
                runningDays = "Daily",
                classes = listOf("1A", "2A", "3A", "SL"),
                status = "On Time",
                dataSource = TransportDataSourceType.DEMO,
                availability = "Available (3A: AVL 42)",
                fare = "₹640 - ₹2150"
            ),
            Train(
                number = "12309",
                name = "Rajdhani Express",
                from = "Patna Junction (PNBE)",
                to = "New Delhi (NDLS)",
                departure = "07:10 PM",
                arrival = "05:20 AM",
                duration = "10h 10m",
                runningDays = "Daily",
                classes = listOf("1A", "2A", "3A"),
                status = "On Time",
                dataSource = TransportDataSourceType.DEMO,
                availability = "Available (2A: AVL 12)",
                fare = "₹1420 - ₹2850"
            ),
            Train(
                number = "13257",
                name = "Jansadharan Express",
                from = "Danapur (DNR)",
                to = "Anand Vihar (ANVT)",
                departure = "01:30 PM",
                arrival = "06:00 AM",
                duration = "16h 30m",
                runningDays = "M, W, F",
                classes = listOf("2S", "GN"),
                status = "Delayed 15m",
                dataSource = TransportDataSourceType.DEMO,
                availability = "Available (2S: AVL 180)",
                fare = "₹225"
            )
        )
    }

    override suspend fun getStations(query: String): List<RailwayStation> {
        delay(300)
        val list = getStationsList()
        return if (query.isBlank()) list else list.filter { 
            it.name.contains(query, true) || it.code.contains(query, true) || it.city.contains(query, true)
        }
    }

    override suspend fun searchTrains(from: String, to: String): List<Train> {
        delay(600)
        return getTrainsList().map { t ->
            t.copy(
                from = if (from.isNotBlank()) from else t.from,
                to = if (to.isNotBlank()) to else t.to
            )
        }
    }

    override suspend fun findTrains(query: String): List<Train> {
        delay(400)
        val allTrains = getTrainsList()
        if (query.isBlank()) return allTrains
        return allTrains.filter { 
            it.number.contains(query, true) || 
            it.name.contains(query, true) || 
            it.from.contains(query, true) || 
            it.to.contains(query, true) 
        }
    }

    override suspend fun getTrainRoute(trainNumber: String): TrainRoute {
        delay(400)
        return TrainRoute(
            trainNumber = trainNumber,
            trainName = if (trainNumber == "12309") "Rajdhani Express" else "Sampoorna Kranti Express",
            origin = "Patna Junction (PNBE)",
            destination = "New Delhi (NDLS)",
            stations = listOf(
                TrainRouteStation("Patna Junction (PNBE)", "00:00", "05:15 PM", 1, 0.0),
                TrainRouteStation("Danapur (DNR)", "05:30 PM", "05:32 PM", 1, 10.2),
                TrainRouteStation("Pt. Deen Dayal Upadhyaya Jn (DDU)", "10:20 PM", "10:30 PM", 1, 211.5),
                TrainRouteStation("Prayagraj Jn (PRYJ)", "12:35 AM", "12:40 AM", 2, 364.2),
                TrainRouteStation("Kanpur Central (CNB)", "03:05 AM", "03:10 AM", 2, 558.7),
                TrainRouteStation("New Delhi (NDLS)", "06:55 AM", "End", 2, 998.4)
            ),
            dataSource = TransportDataSourceType.DEMO
        )
    }

    override suspend fun getLiveStatus(trainNumber: String): TrainLiveStatus {
        delay(400)
        return TrainLiveStatus(
            trainNumber = trainNumber,
            trainName = "Sampoorna Kranti Express",
            currentStation = "Prayagraj Jn (PRYJ)",
            nextStation = "Kanpur Central (CNB)",
            delayMinutes = 0,
            runningState = "Running On Time",
            lastUpdated = "10 mins ago",
            dataSource = TransportDataSourceType.DEMO
        )
    }

    override suspend fun getPnrStatus(pnr: String): Result<BackendPnrResponse> {
        delay(500)
        if (pnr.length != 10 || !pnr.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit PNR number"))
        }
        return if (pnr == "1234567890") {
            Result.success(
                BackendPnrResponse(
                    pnrNumber = pnr,
                    trainNumber = "12393",
                    trainName = "Sampoorna Kranti Express",
                    journeyDate = "2025-10-25",
                    from = "Patna Junction (PNBE)",
                    to = "New Delhi (NDLS)",
                    bookingStatus = "CNF / B2 / 42",
                    currentStatus = "CONFIRMED",
                    coachBerth = "B2 - 42 (Middle)",
                    chartingStatus = "CHART PREPARED"
                )
            )
        } else {
            Result.failure(Exception("PNR record not found or unavailable"))
        }
    }

    override suspend fun getSeatAvailability(train: String, from: String, to: String, date: String, cls: String, quota: String): Result<BackendAvailabilityResponse> {
        delay(500)
        return Result.success(
            BackendAvailabilityResponse(
                trainNumber = train,
                status = "AVAILABLE",
                availabilityText = "AVAILABLE - 42 seats ($cls / $quota)",
                fare = "₹1,250 (Estimated)"
            )
        )
    }
}
