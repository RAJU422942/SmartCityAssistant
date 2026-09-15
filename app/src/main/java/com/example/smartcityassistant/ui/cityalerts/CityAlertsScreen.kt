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
import com.example.smartcityassistant.data.cityalerts.CityAlert
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
            title = { Text("Search Location", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Enter city, district or area (e.g., Patna, Delhi, Mumbai):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("City / Area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E))
                ) {
                    Text("SEARCH")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar & Location Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D2B4E),
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
                Text("Local disaster, weather & air quality updates", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)

                Spacer(modifier = Modifier.height(14.dp))

                // Location Bar Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).clickable { showSearchDialog = true }
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val locTitle = (uiState as? CityAlertsUiState.Success)?.locationName ?: "Madhuban, Bihar"
                            val locType = if ((uiState as? CityAlertsUiState.Success)?.isUsingCustomLocation == true) "Selected location" else "Current location"
                            Column {
                                Text(locTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(locType, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
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

        // Category Filter Chips
        ScrollableTabRow(
            selectedTabIndex = when ((uiState as? CityAlertsUiState.Success)?.selectedCategory) {
                null -> 0
                "WEATHER" -> 1
                "DISASTER" -> 2
                "AIR_QUALITY" -> 3
                else -> 0
            },
            edgePadding = 16.dp,
            containerColor = Color.White,
            contentColor = Color(0xFF0D2B4E)
        ) {
            Tab(
                selected = (uiState as? CityAlertsUiState.Success)?.selectedCategory == null,
                onClick = { viewModel.loadAlerts(category = null) },
                text = { Text("All Alerts", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = (uiState as? CityAlertsUiState.Success)?.selectedCategory == "WEATHER",
                onClick = { viewModel.loadAlerts(category = "WEATHER") },
                text = { Text("Weather", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = (uiState as? CityAlertsUiState.Success)?.selectedCategory == "DISASTER",
                onClick = { viewModel.loadAlerts(category = "DISASTER") },
                text = { Text("Disaster", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = (uiState as? CityAlertsUiState.Success)?.selectedCategory == "AIR_QUALITY",
                onClick = { viewModel.loadAlerts(category = "AIR_QUALITY") },
                text = { Text("Air Quality", fontWeight = FontWeight.Bold) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is CityAlertsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0D2B4E))
                    }
                }
                is CityAlertsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(state.message, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadAlerts() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E))
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is CityAlertsUiState.Success -> {
                    if (state.alerts.isEmpty()) {
                        val (title, subtitle) = when (state.selectedCategory) {
                            "WEATHER" -> "No significant weather alerts" to "for this location"
                            "DISASTER" -> "No nearby disaster alerts" to "No verified GDACS disaster events were found for this location."
                            "AIR_QUALITY" -> "No air-quality alerts" to "for this location"
                            else -> "No verified alerts for this location" to "All monitored services are operating normally."
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.alerts) { alert ->
                                AlertCard(alert = alert)
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
}

@Composable
fun AlertCard(alert: CityAlert) {
    val severityColor = when (alert.severity) {
        "CRITICAL" -> Color(0xFFD32F2F)
        "HIGH" -> Color(0xFFE65100)
        "MODERATE" -> Color(0xFFF57C00)
        else -> Color(0xFF1E88E5)
    }

    val categoryIcon = when (alert.category) {
        "WEATHER" -> Icons.Default.WbSunny
        "DISASTER" -> Icons.Default.Warning
        "AIR_QUALITY" -> Icons.Default.Air
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
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
                        color = severityColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(categoryIcon, contentDescription = null, tint = severityColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(alert.category.replace('_', ' '), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = severityColor)
                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
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
            Text(alert.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
