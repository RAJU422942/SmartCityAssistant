package com.example.smartcityassistant.aqi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AqiDetailsScreen(
    state: AqiUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        AqiTopAppBar(title = "AQI Details", onBack = onBack)

        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (state) {
                is AqiUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AqiUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Air Quality Index", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("AQI unavailable", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
                is AqiUiState.NoNearby -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Air Quality Index", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Air quality unavailable nearby", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1A1A1A))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, fontSize = 14.sp, color = Color.Gray)
                            state.distanceKm?.let { dist ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nearest monitoring station is ~$dist km away (>100 km threshold).", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Source: Central Pollution Control Board (CPCB)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D2B4E))
                        }
                    }
                }
                is AqiUiState.Success -> {
                    val style = AqiClassification.getStyle(state.aqi)
                    val advisory = AqiClassification.getAdvisory(state.aqi)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Current Air Quality", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = state.aqi.toString(),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = style.textColor,
                                    modifier = Modifier
                                        .background(style.backgroundColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = state.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = buildString {
                                            append("Nearest available monitoring station:\n")
                                            append(state.stationName ?: "Monitoring Station")
                                            state.distanceKm?.let { dist ->
                                                append("\nDistance: ~$dist km from your location")
                                            }
                                        },
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Source: ${state.source ?: "Central Pollution Control Board (CPCB)"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D2B4E))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Last updated: ${state.timeString ?: state.lastUpdatedText}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Pollutants Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
                            Spacer(modifier = Modifier.height(16.dp))

                            PollutantRow("PM2.5", state.pm25)
                            PollutantRow("PM10", state.pm10)
                            PollutantRow("CO", state.co)
                            PollutantRow("NO2", state.no2)
                            PollutantRow("O3", state.o3)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Health Advisory", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(advisory, fontSize = 14.sp, color = Color(0xFF1A1A1A), lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PollutantRow(name: String, value: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF333333))
        Text(
            text = value?.let { "$it µg/m³" } ?: "--",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (value != null) Color(0xFF1A1A1A) else Color.Gray
        )
    }
}

@Composable
fun AqiTopAppBar(title: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0D2B4E),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
