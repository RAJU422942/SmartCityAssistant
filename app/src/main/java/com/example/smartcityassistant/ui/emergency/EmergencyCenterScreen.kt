package com.example.smartcityassistant.ui.emergency

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartcityassistant.R
import com.example.smartcityassistant.data.emergency.EmergencyMessageBuilder
import com.example.smartcityassistant.data.emergency.EmergencyService
import com.example.smartcityassistant.data.emergency.EmergencyType
import com.example.smartcityassistant.util.GoogleMapsLauncher
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCenterScreen(onBack: () -> Unit) {
    val viewModel: EmergencyViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val name = if (nameIdx >= 0) cursor.getString(nameIdx) else "Contact"
                            val phone = if (numIdx >= 0) cursor.getString(numIdx) else ""

                            if (phone.isNotBlank()) {
                                viewModel.addContact(context, name, phone)
                            } else {
                                Toast.makeText(context, "This contact doesn't have a phone number.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Unable to read selected contact.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to read contact.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                viewModel.processRecognizedSpeech(spokenText)
                Toast.makeText(context, "Recognized: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No speech detected. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D2B4E),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text("Emergency", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Quick access to emergency help", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                // Offline/Online Status Indicator
                Surface(
                    color = if (uiState.isOnline) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFD32F2F).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (uiState.isOnline) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (uiState.isOnline) "Online" else "Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emergency Capsule View when an emergency type is active
            if (uiState.isCapsuleActive && uiState.selectedEmergencyType != null) {
                val etype = uiState.selectedEmergencyType!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                    Text("EMERGENCY CAPSULE", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.setShowExitConfirmation(true) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("✕ Exit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Status Bar
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("● Emergency Session Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFEB3B))
                                    Text("Situation: ${etype.displayName}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(uiState.locationStatusText, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                                        Text("✓ ${uiState.contacts.size} Emergency Contacts Ready", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                                    }
                                }
                            }

                            // Recognized Speech / Voice Banner if available
                            if (!uiState.recognizedVoiceText.isNullOrBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Text("Voice Transcript Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                                        }
                                        Text("\"${uiState.recognizedVoiceText}\"", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color.White)
                                    }
                                }
                            }

                            Text("QUICK ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))

                            // Action 1: Send Emergency Alert Preview
                            Button(
                                onClick = { viewModel.setShowPreviewDialog(true) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("SEND EMERGENCY ALERT", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB71C1C), maxLines = 1)
                                    Text("WhatsApp vs SMS options", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                }
                            }

                            // Action 1b: Quick WhatsApp vs SMS buttons using saved contacts directly
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.sendWhatsAppTextAlert(context) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    WhatsAppIcon(modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp — ${uiState.contacts.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = { viewModel.sendSmsAlert(context) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🔵 SMS — ${uiState.contacts.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                            }

                            // Action 2: Call 112
                            Button(
                                onClick = {
                                    val primaryService = EmergencyService(
                                        id = "s_112",
                                        title = "National Emergency Number",
                                        subtitle = "Unified response",
                                        phoneNumber = "112",
                                        category = "General",
                                        sourceName = "Government of India",
                                        isPrimary = true
                                    )
                                    viewModel.showCallConfirmation(primaryService)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CALL 112", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            }

                            // Context-Specific Secondary Number Call
                            val secondaryCallNum = when (etype) {
                                EmergencyType.MEDICAL, EmergencyType.ACCIDENT -> Pair("CALL 102 (Ambulance)", "102")
                                EmergencyType.POLICE -> Pair("CALL 100 (Police)", "100")
                                EmergencyType.FIRE -> Pair("CALL 101 (Fire Brigade)", "101")
                                else -> null
                            }
                            if (secondaryCallNum != null) {
                                Button(
                                    onClick = {
                                        val secService = EmergencyService(
                                            id = "sec_${secondaryCallNum.second}",
                                            title = secondaryCallNum.first,
                                            subtitle = "Specialized helpline",
                                            phoneNumber = secondaryCallNum.second,
                                            category = "Specialized",
                                            sourceName = "Government of India"
                                        )
                                        viewModel.showCallConfirmation(secService)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(secondaryCallNum.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }
                            }

                            // Action 3: Share Location
                            OutlinedButton(
                                onClick = { viewModel.shareLocation(context) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SHARE LOCATION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Action 4: Tailored Nearby Help based on type
                            val targetPlace = when (etype) {
                                EmergencyType.MEDICAL, EmergencyType.ACCIDENT -> "Nearest Hospital"
                                EmergencyType.POLICE -> "Police Station"
                                EmergencyType.FIRE -> "Fire Station"
                                else -> "Nearest Hospital"
                            }
                            Button(
                                onClick = { GoogleMapsLauncher.openNearbySearch(context, targetPlace) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FIND $targetPlace", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }

                            // Action 5: Edit Situation / Record Again
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.setNeedHelpDialogVisible(true) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Edit Situation", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setVoiceDialogVisible(true) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Record Again", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Standard Emergency View
                // 1. Primary Emergency Assistance Hero Card (112), I NEED HELP, & VOICE EMERGENCY
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Text("EMERGENCY ASSISTANCE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            }
                            Text(
                                "Need immediate help? Police, Fire, and Medical assistance are unified under India's national helpline.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val primaryService = EmergencyService(
                                            id = "s_112",
                                            title = "National Emergency Number",
                                            subtitle = "Unified response for police, fire, and health",
                                            phoneNumber = "112",
                                            category = "General",
                                            sourceName = "Government of India",
                                            isPrimary = true
                                        )
                                        viewModel.showCallConfirmation(primaryService)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CALL 112", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD32F2F), maxLines = 1)
                                }

                                Button(
                                    onClick = { viewModel.setNeedHelpDialogVisible(true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Support, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("I NEED HELP", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                                }
                            }

                            // Voice Emergency Button
                            Button(
                                onClick = { viewModel.setVoiceDialogVisible(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF880E4F)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🎙 VOICE EMERGENCY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 2. Emergency Services Section
            item {
                Text("Emergency Services", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
            }

            items(uiState.services.filter { !it.isPrimary }) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color(0xFF1E88E5).copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (service.phoneNumber) {
                                            "100" -> Icons.Default.LocalPolice
                                            "102" -> Icons.Default.MedicalServices
                                            "101" -> Icons.Default.LocalFireDepartment
                                            "181" -> Icons.Default.AdminPanelSettings
                                            "1098" -> Icons.Default.ChildCare
                                            else -> Icons.Default.Security
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF1E88E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(service.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(service.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Helpline: ${service.phoneNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D2B4E))
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!service.websiteUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(service.websiteUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Portal", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.showCallConfirmation(service) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Nearby Emergency Help Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Nearby Emergency Help", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
            }

            val nearbyPlaces = listOf(
                Pair("Nearest Hospital", "Find medical care & hospitals"),
                Pair("Police Station", "Find nearest police station"),
                Pair("Fire Station", "Find nearest fire rescue unit"),
                Pair("Blood Bank", "Find blood banks & stock")
            )

            items(nearbyPlaces) { place ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { GoogleMapsLauncher.openNearbySearch(context, place.first) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                when (place.first) {
                                    "Nearest Hospital" -> Icons.Default.LocalHospital
                                    "Police Station" -> Icons.Default.LocalPolice
                                    "Fire Station" -> Icons.Default.LocalFireDepartment
                                    else -> Icons.Default.Bloodtype
                                },
                                contentDescription = null,
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(place.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(place.second, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.Directions, contentDescription = "Directions", tint = Color(0xFF1E88E5))
                    }
                }
            }

            // 4. Share My Location Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Location Sharing", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                            Column {
                                Text("Share My Location", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Quickly share your GPS coordinates via Android share sheet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(
                            onClick = { viewModel.shareLocation(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Location via Apps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 5. My Emergency Contacts Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Emergency Contacts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
                    TextButton(onClick = {
                        contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Contact", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (uiState.contacts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No emergency contacts added yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+ Pick from Phone Contacts")
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.contacts.forEach { contact ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                            Surface(
                                                color = Color(0xFF0D2B4E).copy(alpha = 0.1f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0D2B4E), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Column {
                                                Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(contact.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        IconButton(onClick = { viewModel.removeContact(contact.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Contact", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // Per-contact quick actions [ WhatsApp ] [ SMS ] [ Call ]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.sendWhatsAppTextAlert(context, contact.phone) },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            WhatsAppIcon(modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.sendSmsAlert(context, contact.phone) },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                                        }

                                        Button(
                                            onClick = { EmergencyCallLauncher.launchEmergencyCall(context, contact.phone) },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Emergency Tips
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Emergency Safety Tips", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TipItem("For immediate danger or life-threatening emergencies, call 112.")
                        TipItem("Move to a safe, well-lit location if possible before calling.")
                        TipItem("Keep your location details and emergency contacts updated.")
                        TipItem("Follow official instructions from police and disaster responders.")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // "I NEED HELP" Decision Helper Dialog
    if (uiState.showNeedHelpDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setNeedHelpDialogVisible(false) },
            title = {
                Text("What happened?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select your emergency situation to open the Emergency Capsule:", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    EmergencyType.entries.forEach { etype ->
                        Surface(
                            onClick = { viewModel.selectEmergencyType(etype) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    when (etype) {
                                        EmergencyType.MEDICAL -> Icons.Default.MedicalServices
                                        EmergencyType.POLICE -> Icons.Default.LocalPolice
                                        EmergencyType.FIRE -> Icons.Default.LocalFireDepartment
                                        EmergencyType.ACCIDENT -> Icons.Default.CarCrash
                                        EmergencyType.DISASTER -> Icons.Default.Warning
                                        else -> Icons.Default.Emergency
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(etype.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(etype.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setNeedHelpDialogVisible(false) }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Voice Emergency Dialog / Recorder
    if (uiState.showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setVoiceDialogVisible(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF880E4F))
                    Text("Voice Emergency", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Tell us what happened in your own words (Hindi, English, or Hinglish).", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Text("Example: \"Mera accident ho gaya hai aur mujhe hospital chahiye.\"", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.DarkGray, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                }
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Voice recognition is unavailable on this device.", Toast.LENGTH_SHORT).show()
                                viewModel.setVoiceDialogVisible(false)
                            }
                        },
                        modifier = Modifier
                            .size(90.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF880E4F))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Mic, contentDescription = "Tap to speak", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    Text("🔴 Tap to Speak", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF880E4F))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setVoiceDialogVisible(false) }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Send Alert Preview / Confirmation Dialog (WhatsApp vs SMS separation)
    if (uiState.showPreviewDialog && uiState.selectedEmergencyType != null) {
        val etype = uiState.selectedEmergencyType!!
        val previewMsg = EmergencyMessageBuilder.buildMessage(etype, if (uiState.isLocationAvailable) uiState.latitude else null, if (uiState.isLocationAvailable) uiState.longitude else null, uiState.recognizedVoiceText)

        AlertDialog(
            onDismissRequest = { viewModel.setShowPreviewDialog(false) },
            title = {
                Text("Send Emergency Alert?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Situation: ${etype.displayName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB71C1C))
                    Text("Location: ${if (uiState.isLocationAvailable) "✓ Current location ready" else "⚠ Location unavailable"}", fontSize = 12.sp)
                    Text("Contacts: ✓ ${uiState.contacts.size} emergency contacts ready", fontSize = 12.sp)
                    if (!uiState.recognizedVoiceText.isNullOrBlank()) {
                        Text("Voice: ✓ Transcript ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Message Preview:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = previewMsg,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // WhatsApp Button (Text + Location)
                    Button(
                        onClick = {
                            viewModel.setShowPreviewDialog(false)
                            viewModel.sendWhatsAppTextAlert(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WhatsAppIcon(modifier = Modifier.size(20.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WhatsApp — ${uiState.contacts.size} Contacts", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = Color.White)
                                Text("Voice + Text + Location", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SMS Button (Text + Location)
                    Button(
                        onClick = {
                            viewModel.setShowPreviewDialog(false)
                            viewModel.sendSmsAlert(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SMS — ${uiState.contacts.size} Contacts", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = Color.White)
                                Text("Text + Location", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Completely separate Cancel action
                    TextButton(
                        onClick = { viewModel.setShowPreviewDialog(false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Exit Confirmation Dialog
    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowExitConfirmation(false) },
            title = {
                Text("Exit Emergency Session?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Your current emergency capsule session will be closed.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.exitCapsule() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Exit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.setShowExitConfirmation(false) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Continue Session")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Call Confirmation Dialog (Accidental Tap Protection)
    if (uiState.showCallConfirmationDialog && uiState.selectedServiceForCall != null) {
        val service = uiState.selectedServiceForCall!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissCallConfirmation() },
            title = {
                Text("Emergency Call Confirmation", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You are about to call ${service.title} (${service.phoneNumber}).")
                    if (service.phoneNumber == "112") {
                        Text("112 is India's integrated emergency assistance number for police, fire, and health.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissCallConfirmation()
                        EmergencyCallLauncher.launchEmergencyCall(context, service.phoneNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call ${service.phoneNumber}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissCallConfirmation() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun WhatsAppIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_whatsapp),
        contentDescription = "WhatsApp",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun TipItem(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF0D2B4E))
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}
