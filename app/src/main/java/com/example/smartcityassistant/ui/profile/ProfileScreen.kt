package com.example.smartcityassistant.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File

// Light theme color constants matching Home screen
private val ProfileBackground = Color(0xFFF5F5F5)
private val PrimaryNavy = Color(0xFF0D2B4E)
private val MainText = Color(0xFF1A1A1A)
private val SecondaryText = Color(0xFF666666)
private val DividerColor = Color(0xFFEEEEEE)
private val AccentBlue = Color(0xFF1E88E5)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPhotoOptions by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.updateAvatar(tempPhotoUri.toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "profile_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.updateAvatar(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryNavy,
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
                    Text("My Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { /* Settings action */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryNavy)
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.Red, fontSize = 14.sp)
                }
            }
            is ProfileUiState.Success -> {
                val profile = state.profile

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryNavy)
                                        .clickable { showPhotoOptions = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile.avatarUri != null) {
                                        AsyncImage(
                                            model = profile.avatarUri,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = "Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryNavy)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(profile.email, fontSize = 13.sp, color = SecondaryText)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.setEditing(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit Profile", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showPhotoOptions = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Change Photo", color = PrimaryNavy, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Personal Information Section
                    item {
                        ProfileSectionHeader("Personal Information")
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileRow(label = "Full Name", value = profile.name, icon = Icons.Default.Badge)
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileRow(label = "Email", value = profile.email, icon = Icons.Default.Email)
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileRow(label = "Phone Number", value = profile.phoneNumber, icon = Icons.Default.Phone)
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileRow(label = "City", value = profile.city, icon = Icons.Default.LocationCity)
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileRow(label = "State", value = profile.state, icon = Icons.Default.Map)
                            }
                        }
                    }

                    // City Preferences Section
                    item {
                        ProfileSectionHeader("City Preferences")
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileRow(label = "Preferred City", value = profile.preferredCity, icon = Icons.Default.LocationOn)
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileNavigationRow(
                                    label = "Language (${profile.language})",
                                    icon = Icons.Default.Language,
                                    onClick = { viewModel.setActiveDialog(ProfileDialogType.LANGUAGE) }
                                )
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Push Notifications", fontSize = 14.sp, color = MainText, fontWeight = FontWeight.Medium)
                                    }
                                    Switch(
                                        checked = profile.notificationsEnabled,
                                        onCheckedChange = { checked ->
                                            viewModel.updatePreference(checked, profile.locationAccessEnabled, profile.language, profile.preferredCity)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryNavy)
                                    )
                                }
                            }
                        }
                    }

                    // App Settings Section
                    item {
                        ProfileSectionHeader("App Settings")
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileToggleRow(
                                    label = "Location Access",
                                    checked = profile.locationAccessEnabled,
                                    icon = Icons.Default.MyLocation,
                                    onChanged = { checked ->
                                        viewModel.updatePreference(profile.notificationsEnabled, checked, profile.language, profile.preferredCity)
                                    }
                                )
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileNavigationRow(
                                    label = "Privacy Policy",
                                    icon = Icons.Default.Lock,
                                    onClick = { viewModel.setActiveDialog(ProfileDialogType.PRIVACY) }
                                )
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileNavigationRow(
                                    label = "About Smart City Assistant",
                                    icon = Icons.Default.Info,
                                    onClick = { viewModel.setActiveDialog(ProfileDialogType.ABOUT) }
                                )
                            }
                        }
                    }

                    // Emergency & Safety Section
                    item {
                        ProfileSectionHeader("Emergency & Safety")
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileNavigationRow(
                                    label = "Emergency Contacts (${profile.emergencyContacts.size})",
                                    icon = Icons.Default.ContactPhone,
                                    onClick = { viewModel.setActiveDialog(ProfileDialogType.EMERGENCY_CONTACTS) }
                                )
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                ProfileNavigationRow(
                                    label = "Safety Guidelines",
                                    icon = Icons.Default.Security,
                                    onClick = { viewModel.setActiveDialog(ProfileDialogType.SAFETY_GUIDELINES) }
                                )
                            }
                        }
                    }

                    // Logout Button
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.setShowLogoutDialog(true) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (showPhotoOptions) {
                    AlertDialog(
                        onDismissRequest = { showPhotoOptions = false },
                        title = { Text("Profile Photo", fontWeight = FontWeight.Bold, color = MainText) },
                        text = { Text("Choose how you would like to set your profile picture.", color = SecondaryText) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPhotoOptions = false
                                    galleryLauncher.launch("image/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                            ) {
                                Text("Choose from Gallery")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showPhotoOptions = false
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            ) {
                                Text("Take Photo", color = PrimaryNavy)
                            }
                        }
                    )
                }

                if (state.isEditing) {
                    EditProfileDialog(
                        profile = profile,
                        onDismiss = { viewModel.setEditing(false) },
                        onSave = { updated -> viewModel.updateProfile(updated) },
                    )
                }

                if (state.showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.setShowLogoutDialog(false) },
                        title = { Text("Log Out", fontWeight = FontWeight.Bold, color = MainText) },
                        text = { Text("Are you sure you want to log out of Smart City Assistant?", color = SecondaryText) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.setShowLogoutDialog(false)
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Text("Log Out")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.setShowLogoutDialog(false) }) {
                                Text("Cancel", color = SecondaryText)
                            }
                        }
                    )
                }

                // Active Dialogs handler
                when (state.activeDialog) {
                    ProfileDialogType.LANGUAGE -> LanguageSelectionDialog(
                        currentLanguage = profile.language,
                        onDismiss = { viewModel.setActiveDialog(null) },
                        onSelect = { lang -> viewModel.updateLanguage(lang) }
                    )
                    ProfileDialogType.PRIVACY -> PrivacyPolicyDialog(
                        onDismiss = { viewModel.setActiveDialog(null) }
                    )
                    ProfileDialogType.ABOUT -> AboutDialog(
                        onDismiss = { viewModel.setActiveDialog(null) }
                    )
                    ProfileDialogType.EMERGENCY_CONTACTS -> EmergencyContactsDialog(
                        context = context,
                        contacts = profile.emergencyContacts,
                        onDismiss = { viewModel.setActiveDialog(null) },
                        onAdd = { name, phone -> viewModel.addEmergencyContact(name, phone) },
                        onRemove = { id -> viewModel.removeEmergencyContact(id) }
                    )
                    ProfileDialogType.SAFETY_GUIDELINES -> SafetyGuidelinesDialog(
                        onDismiss = { viewModel.setActiveDialog(null) }
                    )
                    null -> {}
                }
            }
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = PrimaryNavy,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun ProfileRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = SecondaryText)
        }
        Text(value, fontSize = 14.sp, color = MainText, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileToggleRow(label: String, checked: Boolean, icon: ImageVector, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = MainText, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryNavy)
        )
    }
}

@Composable
fun ProfileNavigationRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = MainText, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phoneNumber) }
    var city by remember { mutableStateOf(profile.city) }
    var state by remember { mutableStateOf(profile.state) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(profile.copy(name = name, email = email, phoneNumber = phone, city = city, state = state, preferredCity = city))
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "English", "Hindi (हिन्दी)", "Maithili (मैथिली)", "Urdu (اردو)", 
        "Bengali (বাংলা)", "Telugu (తెలుగు)", "Marathi (मराठी)", "Tamil (தமிழ்)", 
        "Gujarati (ગુજરાતી)", "Kannada (ಕನ್ನಡ)", "Malayalam (മലയാളം)", "Odia (ଓଡ଼ିଆ)", 
        "Punjabi (ਪੰਜਾਬੀ)", "Assamese (অসমীয়া)", "Spanish", "French", "German"
    )
    var selected by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language", fontWeight = FontWeight.Bold, color = MainText) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = lang }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == lang,
                            onClick = { selected = lang },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryNavy)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lang, fontSize = 14.sp, color = MainText, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy — Smart City Assistant", fontWeight = FontWeight.Bold, color = MainText) },
        text = {
            Column(modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Data Collection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                Text("Smart City Assistant collects user-provided municipal issue descriptions, GPS location data for local reporting, transit route queries, and AQI monitoring coordinates solely to deliver municipal and civic utility services.", fontSize = 12.sp, color = MainText)

                Text("2. Security & Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                Text("All profile data, preferences, and emergency contacts are securely stored locally on your device via encrypted local storage and are never sold or shared with commercial third parties.", fontSize = 12.sp, color = MainText)

                Text("3. Third-Party Services", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                Text("Transit routing and air quality indices are retrieved via secure HTTPS proxy calls to verified public APIs (Google Maps & AQICN/RailKit).", fontSize = 12.sp, color = MainText)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Smart City Assistant", fontWeight = FontWeight.Bold, color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Smart City Assistant v1.0 (Production)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                Text(
                    "Smart City Assistant is your all-in-one municipal utility and civic companion. Key features include:",
                    fontSize = 13.sp,
                    color = MainText
                )
                Text("• Municipal Issue Reporting & Tracking\n• Real-Time Public Transport (Bus & Railway)\n• Live Air Quality Index (AQI) Monitoring\n• Emergency SOS & Safety Dispatch", fontSize = 12.sp, color = MainText, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EmergencyContactsDialog(
    context: Context,
    contacts: List<EmergencyContact>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val contactName = if (nameIdx >= 0) it.getString(nameIdx) else "Contact"
                        val contactNumber = if (numberIdx >= 0) it.getString(numberIdx) else ""
                        onAdd(contactName, contactNumber)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Contacts", fontWeight = FontWeight.Bold, color = MainText) },
        text = {
            Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                if (contacts.isEmpty()) {
                    Text("No emergency contacts added yet.", fontSize = 13.sp, color = SecondaryText)
                } else {
                    contacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MainText)
                                Text(contact.phone, fontSize = 12.sp, color = SecondaryText)
                            }
                            IconButton(onClick = { onRemove(contact.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Manual", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            contactPickerLauncher.launch(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Contact", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)) {
                Text("Close")
            }
        }
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Contact", fontWeight = FontWeight.Bold, color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MainText,
                            unfocusedTextColor = MainText,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MainText,
                            unfocusedTextColor = MainText,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onAdd(name, phone)
                            name = ""
                            phone = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
fun SafetyGuidelinesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Safety Guidelines — Smart City Assistant", fontWeight = FontWeight.Bold, color = MainText) },
        text = {
            Column(modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Emergency Preparedness", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                Text("Save primary emergency contacts (Police 112, Ambulance 108, Fire 101) directly inside your Smart City Assistant profile for quick dispatch.", fontSize = 12.sp, color = MainText)

                Text("2. Public Transport & Railway", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                Text("Always verify train schedules, live running status, and PNR confirmation status before embarking on journeys.", fontSize = 12.sp, color = MainText)

                Text("3. Civic Health & AQI", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                Text("Monitor real-time Air Quality Index (AQI) updates on the Home screen. Adhere to health advisories during periods of elevated pollution.", fontSize = 12.sp, color = MainText)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)) {
                Text("Close")
            }
        }
    )
}
