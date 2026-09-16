package com.example.smartcityassistant.ui.cityalerts

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartcityassistant.aqi.AqiUiState
import com.example.smartcityassistant.data.cityalerts.CityAlert
import com.example.smartcityassistant.weather.WeatherUiState
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
@Composable
fun CityAlertsScreen(
    viewModel: CityAlertsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            isLocating = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                isLocating = false
                loc?.let {
                    viewModel.fetchCurrentLocationAndLoad(it.latitude, it.longitude)
                } ?: run {
                    Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                isLocating = false
                Toast.makeText(context, "Location error", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission is required to find your current location.", Toast.LENGTH_LONG).show()
        }
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Location", fontWeight = FontWeight.Bold, color = Color(0xFF0D2B45)) },
            text = {
                Column {
                    Text("Enter city, district or area (e.g., Delhi, Ahmedabad, Darbhanga, Mehsana, Patna, Mumbai):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0D2B45),
                            unfocusedTextColor = Color(0xFF0D2B45),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.searchLocation(searchQuery.trim())
                            showSearchDialog = false
                            searchQuery = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("SEARCH")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // Top App Bar & Location Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D2B45),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("City Alerts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Professional civic alert center", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)

                Spacer(modifier = Modifier.height(14.dp))

                // Location Bar Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSearchDialog = true }
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val locationTitle = when (uiState) {
                                is CityAlertsUiState.Success -> (uiState as CityAlertsUiState.Success).selectedLocation.name
                                is CityAlertsUiState.Error -> (uiState as CityAlertsUiState.Error).selectedLocation?.name ?: "Search location"
                                else -> "Search location"
                            }
                            val locationSourceText = when (uiState) {
                                is CityAlertsUiState.Success -> {
                                    if ((uiState as CityAlertsUiState.Success).selectedLocation.source == LocationSource.CURRENT_LOCATION)
                                        "Current Location" else "Search Location"
                                }
                                else -> "Tap to search or select location"
                            }
                            Column {
                                Text(locationTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(locationSourceText, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = {
                                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                        if (fine == PackageManager.PERMISSION_GRANTED) {
                                            isLocating = true
                                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                                isLocating = false
                                                loc?.let {
                                                    viewModel.fetchCurrentLocationAndLoad(it.latitude, it.longitude)
                                                } ?: run {
                                                    Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                                                }
                                            }.addOnFailureListener {
                                                isLocating = false
                                                Toast.makeText(context, "Location error", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Use current location", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { showSearchDialog = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Search, contentDescription = "Search Location", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Horizontally Scrollable Category Filter Tabs
        val selectedCategory = (uiState as? CityAlertsUiState.Success)?.selectedCategory
        val selectedIndex = when (selectedCategory) {
            null -> 0
            "WEATHER" -> 1
            "DISASTER" -> 2
            "AIR_QUALITY" -> 3
            else -> 0
        }

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 16.dp,
            containerColor = Color.White,
            contentColor = Color(0xFF0D2B45),
            divider = {}
        ) {
            Tab(
                selected = selectedCategory == null,
                onClick = { viewModel.setCategory(null) },
                text = { Text("All", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategory == "WEATHER",
                onClick = { viewModel.setCategory("WEATHER") },
                text = { Text("Weather", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategory == "DISASTER",
                onClick = { viewModel.setCategory("DISASTER") },
                text = { Text("Disaster", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategory == "AIR_QUALITY",
                onClick = { viewModel.setCategory("AIR_QUALITY") },
                text = { Text("Air Quality", fontWeight = FontWeight.Bold) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is CityAlertsUiState.Unselected -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.LocationSearching, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Search location", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D2B45), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Please search for a location or tap GPS to view real-time civic alerts, weather data, and air quality updates.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showSearchDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search location")
                            }
                        }
                    }
                }
                is CityAlertsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0D2B45))
                    }
                }
                is CityAlertsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Unable to update alerts. Tap to retry.", fontSize = 15.sp, color = Color(0xFF0D2B45), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { state.selectedLocation?.let { viewModel.selectLocationAndLoad(it) } ?: run { showSearchDialog = true } },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B45))
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is CityAlertsUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val showWeather = state.selectedCategory == null || state.selectedCategory == "WEATHER"
                        val showAirQuality = state.selectedCategory == null || state.selectedCategory == "AIR_QUALITY"
                        val showDisaster = state.selectedCategory == null || state.selectedCategory == "DISASTER"

                        // 1. Weather Section
                        if (showWeather) {
                            item {
                                Text("WEATHER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            item {
                                when (val wState = state.weatherState) {
                                    is WeatherUiState.Success -> {
                                        WeatherCard(wState)
                                    }
                                    is WeatherUiState.Error -> {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Text("Weather unavailable: ${wState.message}", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Color.Gray)
                                        }
                                    }
                                    else -> {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.weatherAdvisories.isEmpty()) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Text("No weather advisories right now.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            } else {
                                items(state.weatherAdvisories) { adv ->
                                    AlertCard(alert = adv)
                                }
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        // 2. Air Quality Section
                        if (showAirQuality) {
                            item {
                                Text("AIR QUALITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            item {
                                when (val aqiState = state.aqiState) {
                                    is AqiUiState.Success -> {
                                        AqiCompactCard(aqiState)
                                    }
                                    is AqiUiState.NoNearby -> {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Text(aqiState.message, modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Color.Gray)
                                        }
                                    }
                                    is AqiUiState.Error -> {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Text("Air quality unavailable: ${aqiState.message}", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Color.Gray)
                                        }
                                    }
                                    else -> {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.aqiAdvisories.isEmpty()) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Text("No significant air quality alert.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            } else {
                                items(state.aqiAdvisories) { adv ->
                                    AlertCard(alert = adv)
                                }
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        // 3. Disaster Section
                        if (showDisaster) {
                            item {
                                Text("DISASTER ALERTS (GDACS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (state.disasterAlerts.isEmpty()) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No active disaster alerts near this location.", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D2B45), textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("No verified GDACS disaster events found within 300 km.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            } else {
                                items(state.disasterAlerts) { alert ->
                                    AlertCard(alert = alert)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${weather.temperature.toInt()}°C", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D2B45))
                    Text(weather.condition, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                }
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Feels like ${weather.apparentTemperature.toInt()}°C", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeatherMetricItem("Humidity", "${weather.humidity}%")
                WeatherMetricItem("Wind", "${weather.windSpeed.toInt()} km/h")
                val precip = weather.hourly.firstOrNull()?.precipitationProbability?.toInt() ?: 0
                WeatherMetricItem("Rain Prob", "$precip%")
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Source: MET Norway", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(weather.lastUpdatedText, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun WeatherMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D2B45))
    }
}

@Composable
fun AqiCompactCard(aqi: AqiUiState.Success) {
    val aqiColor = when {
        aqi.aqi <= 50 -> Color(0xFF2E7D32)
        aqi.aqi <= 100 -> Color(0xFFF57C00)
        aqi.aqi <= 150 -> Color(0xFFE65100)
        aqi.aqi <= 200 -> Color(0xFFD32F2F)
        else -> Color(0xFF7B1FA2)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = aqiColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${aqi.aqi}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = aqiColor)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(aqi.category, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = aqiColor)
                        Text("Dominant Pollutant: ${aqi.dominantPollutant ?: "PM2.5"}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Source: ${aqi.sourceLabel ?: "Open-Meteo • CAMS"}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(aqi.lastUpdatedText, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AlertCard(alert: CityAlert) {
    val severityColor = when (alert.severity) {
        "CRITICAL", "SEVERE" -> Color(0xFFD32F2F)
        "HIGH" -> Color(0xFFE65100)
        "MODERATE" -> Color(0xFFF57C00)
        else -> Color(0xFF1976D2)
    }

    val categoryIcon = when (alert.category) {
        "WEATHER" -> Icons.Default.WbSunny
        "DISASTER" -> Icons.Default.Warning
        "AIR_QUALITY" -> Icons.Default.Air
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = severityColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(categoryIcon, contentDescription = null, tint = severityColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(alert.category.replace('_', ' '), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = severityColor)
                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D2B45), maxLines = 2)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = severityColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = alert.severity,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(alert.description, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Source: ${alert.sourceName}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(alert.status, fontSize = 11.sp, color = if (alert.status == "ACTIVE") Color(0xFF2E7D32) else Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
