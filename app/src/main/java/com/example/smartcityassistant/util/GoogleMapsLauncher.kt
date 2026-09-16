package com.example.smartcityassistant.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object GoogleMapsLauncher {
    fun launchGoogleMapsSearch(context: Context, query: String) {
        try {
            val encodedQuery = Uri.encode(query)
            val url = "https://www.google.com/maps/search/?api=1&query=$encodedQuery"
            val uri = Uri.parse(url)

            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No application found to open Google Maps.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Google Maps.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openNearbySearch(context: Context, query: String) {
        launchGoogleMapsSearch(context, query)
    }

    fun openLocation(context: Context, lat: Double, lon: Double, name: String) {
        launchGoogleMapsSearch(context, "$name near $lat,$lon")
    }
}
