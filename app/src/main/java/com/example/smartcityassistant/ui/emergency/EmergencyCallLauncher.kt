package com.example.smartcityassistant.ui.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmergencyCallLauncher {
    fun launchEmergencyCall(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Phone calls are not supported on this device.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open the phone dialer.", Toast.LENGTH_SHORT).show()
        }
    }
}
