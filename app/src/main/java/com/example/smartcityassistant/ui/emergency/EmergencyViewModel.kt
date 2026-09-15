package com.example.smartcityassistant.ui.emergency

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.emergency.EmergencyContact
import com.example.smartcityassistant.data.emergency.EmergencyMessageBuilder
import com.example.smartcityassistant.data.emergency.EmergencyService
import com.example.smartcityassistant.data.emergency.EmergencyServiceRepository
import com.example.smartcityassistant.data.emergency.EmergencyType
import com.example.smartcityassistant.data.emergency.EmergencyVoiceClassifier
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmergencyServiceRepository()
    private val prefs = application.getSharedPreferences("smart_city_emergency_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    companion object {
        private const val MAX_LOCATION_AGE_MS = 2 * 60 * 1000L // 2 minutes freshness threshold
    }

    private val _uiState = MutableStateFlow(
        EmergencyUiState(
            services = repository.getEmergencyServices(),
            contacts = loadContacts(),
            isOnline = checkConnectivity(application),
            latitude = null,
            longitude = null,
            isLocationAvailable = false,
            locationStatusText = "Getting current location..."
        )
    )
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    init {
        fetchFreshLocation(application)
    }

    fun isLocationFresh(location: Location): Boolean {
        val lat = location.latitude
        val lon = location.longitude
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return false
        if (lat == 0.0 && lon == 0.0) return false

        val age = System.currentTimeMillis() - location.time
        if (age > MAX_LOCATION_AGE_MS) return false
        return true
    }

    fun fetchFreshLocation(context: Context, onReady: ((lat: Double, lon: Double, accuracy: Float, isFresh: Boolean) -> Unit)? = null) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            _uiState.update { it.copy(isLocationAvailable = false, locationStatusText = "⚠ Location permission required") }
            return
        }

        _uiState.update { it.copy(locationStatusText = "Getting current location...") }

        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null && location.latitude >= -90.0 && location.latitude <= 90.0 && location.longitude >= -180.0 && location.longitude <= 180.0 && !(location.latitude == 0.0 && location.longitude == 0.0)) {
                        val lat = location.latitude
                        val lon = location.longitude
                        val acc = location.accuracy
                        val ageSec = (System.currentTimeMillis() - location.time) / 1000
                        val isFresh = ageSec <= 120 // 2 minutes

                        val statusText = if (isFresh) {
                            "✓ Current location ready • ±${acc.toInt()} m"
                        } else {
                            "⚠ Last known location • ${ageSec / 60} mins old"
                        }

                        _uiState.update {
                            it.copy(
                                latitude = lat,
                                longitude = lon,
                                isLocationAvailable = true,
                                locationStatusText = statusText
                            )
                        }
                        onReady?.invoke(lat, lon, acc, isFresh)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null && isLocationFresh(lastLoc)) {
                                val lat = lastLoc.latitude
                                val lon = lastLoc.longitude
                                val acc = lastLoc.accuracy

                                _uiState.update {
                                    it.copy(
                                        latitude = lat,
                                        longitude = lon,
                                        isLocationAvailable = true,
                                        locationStatusText = "✓ Current location ready • ±${acc.toInt()} m"
                                    )
                                }
                                onReady?.invoke(lat, lon, acc, true)
                            } else if (lastLoc != null) {
                                val lat = lastLoc.latitude
                                val lon = lastLoc.longitude
                                val acc = lastLoc.accuracy
                                val ageSec = (System.currentTimeMillis() - lastLoc.time) / 1000

                                _uiState.update {
                                    it.copy(
                                        latitude = lat,
                                        longitude = lon,
                                        isLocationAvailable = true,
                                        locationStatusText = "⚠ Last known location • ${ageSec / 60} mins old"
                                    )
                                }
                                onReady?.invoke(lat, lon, acc, false)
                            } else {
                                _uiState.update { it.copy(isLocationAvailable = false, locationStatusText = "⚠ Location unavailable") }
                            }
                        }.addOnFailureListener {
                            _uiState.update { it.copy(isLocationAvailable = false, locationStatusText = "⚠ Location unavailable") }
                        }
                    }
                }
                .addOnFailureListener {
                    _uiState.update { it.copy(isLocationAvailable = false, locationStatusText = "⚠ Location unavailable") }
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLocationAvailable = false, locationStatusText = "⚠ Location unavailable") }
        }
    }

    fun showCallConfirmation(service: EmergencyService) {
        _uiState.update { it.copy(selectedServiceForCall = service, showCallConfirmationDialog = true) }
    }

    fun dismissCallConfirmation() {
        _uiState.update { it.copy(selectedServiceForCall = null, showCallConfirmationDialog = false) }
    }

    fun setNeedHelpDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showNeedHelpDialog = visible) }
    }

    fun setVoiceDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showVoiceDialog = visible, recognizedVoiceText = null) }
    }

    fun selectEmergencyType(type: EmergencyType?) {
        fetchFreshLocation(getApplication())
        _uiState.update { it.copy(selectedEmergencyType = type, isCapsuleActive = type != null, showNeedHelpDialog = false, showVoiceDialog = false) }
    }

    fun processRecognizedSpeech(spokenText: String) {
        val classifiedType = EmergencyVoiceClassifier.classify(spokenText)
        fetchFreshLocation(getApplication())
        _uiState.update {
            it.copy(
                recognizedVoiceText = spokenText,
                selectedEmergencyType = classifiedType,
                isCapsuleActive = true,
                showVoiceDialog = false
            )
        }
    }

    fun exitCapsule() {
        _uiState.update { it.copy(selectedEmergencyType = null, isCapsuleActive = false, showExitConfirmation = false, recognizedVoiceText = null) }
    }

    fun setShowExitConfirmation(show: Boolean) {
        _uiState.update { it.copy(showExitConfirmation = show) }
    }

    fun setShowPreviewDialog(show: Boolean) {
        if (show) {
            fetchFreshLocation(getApplication())
        }
        _uiState.update { it.copy(showPreviewDialog = show) }
    }

    fun addContact(context: Context, name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) {
            Toast.makeText(context, "Invalid contact details", Toast.LENGTH_SHORT).show()
            return
        }
        val normalizedPhone = normalizePhoneNumber(phone)
        val currentList = _uiState.value.contacts.toMutableList()

        val exists = currentList.any { normalizePhoneNumber(it.phone) == normalizedPhone }
        if (exists) {
            Toast.makeText(context, "Contact already added.", Toast.LENGTH_SHORT).show()
            return
        }

        currentList.add(EmergencyContact(name = name, phone = phone))
        saveContacts(currentList)
        _uiState.update { it.copy(contacts = currentList) }
        Toast.makeText(context, "Contact added: $name", Toast.LENGTH_SHORT).show()
    }

    fun removeContact(id: String) {
        val currentList = _uiState.value.contacts.filterNot { it.id == id }
        saveContacts(currentList)
        _uiState.update { it.copy(contacts = currentList) }
    }

    fun normalizePhoneNumber(phone: String): String {
        val clean = phone.replace(Regex("[^0-9+]"), "")
        if (clean.startsWith("+")) return clean
        val digitsOnly = clean.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length == 10) {
            return "+91$digitsOnly"
        }
        return "+$digitsOnly"
    }

    fun shareLocation(context: Context) {
        fetchFreshLocation(context) { lat, lon, _, _ ->
            try {
                val mapsUrl = "https://maps.google.com/?q=$lat,$lon"
                val shareText = "🚨 EMERGENCY ALERT: I need assistance! My location: $mapsUrl"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Emergency Location Share")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share Location").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to share location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendWhatsAppTextAlert(context: Context, phoneNumber: String? = null) {
        fetchFreshLocation(context) { lat, lon, _, _ ->
            val state = _uiState.value
            val emergencyType = state.selectedEmergencyType ?: EmergencyType.OTHER
            val message = EmergencyMessageBuilder.buildMessage(emergencyType, lat, lon, state.recognizedVoiceText)

            val pm = context.packageManager
            val packagesToCheck = listOf("com.whatsapp", "com.whatsapp.w4b")
            var installedPackage: String? = null

            for (pkg in packagesToCheck) {
                val isInstalled = try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (e: Exception) {
                    false
                }
                if (isInstalled) {
                    installedPackage = pkg
                    break
                }
            }

            if (installedPackage == null) {
                Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
                sendSmsAlert(context, phoneNumber)
                return@fetchFreshLocation
            }

            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage(installedPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Could not open WhatsApp.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open WhatsApp.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendSmsAlert(context: Context, phoneNumber: String? = null) {
        fetchFreshLocation(context) { lat, lon, _, _ ->
            val state = _uiState.value
            val emergencyType = state.selectedEmergencyType ?: EmergencyType.OTHER
            val message = EmergencyMessageBuilder.buildMessage(emergencyType, lat, lon, state.recognizedVoiceText)

            val recipients = if (!phoneNumber.isNullOrBlank()) {
                normalizePhoneNumber(phoneNumber)
            } else if (state.contacts.isNotEmpty()) {
                state.contacts.map { normalizePhoneNumber(it.phone) }.joinToString(",")
            } else {
                ""
            }

            try {
                val uri = if (recipients.isNotBlank()) {
                    Uri.parse("smsto:$recipients")
                } else {
                    Uri.parse("smsto:")
                }
                val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "SMS application not available.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open messaging app.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkConnectivity(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadContacts(): List<EmergencyContact> {
        val json = prefs.getString("emergency_contacts_json", null) ?: return listOf(
            EmergencyContact(name = "Police Control Room", phone = "112"),
            EmergencyContact(name = "Ambulance Dispatch", phone = "102")
        )
        return try {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveContacts(contacts: List<EmergencyContact>) {
        try {
            val json = gson.toJson(contacts)
            prefs.edit().putString("emergency_contacts_json", json).apply()
        } catch (e: Exception) {}
    }
}

data class EmergencyUiState(
    val services: List<EmergencyService>,
    val contacts: List<EmergencyContact>,
    val selectedServiceForCall: EmergencyService? = null,
    val showCallConfirmationDialog: Boolean = false,
    val showNeedHelpDialog: Boolean = false,
    val showVoiceDialog: Boolean = false,
    val showPreviewDialog: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val selectedEmergencyType: EmergencyType? = null,
    val isCapsuleActive: Boolean = false,
    val isOnline: Boolean = true,
    val latitude: Double?,
    val longitude: Double?,
    val isLocationAvailable: Boolean,
    val locationStatusText: String,
    val recognizedVoiceText: String? = null
)
