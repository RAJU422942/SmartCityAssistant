package com.example.smartcityassistant.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object GoogleMapsLauncher {
    fun openNearbySearch(context: Context, query: String) {
        try {
            val encodedQuery = Uri.encode(query)
            val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedQuery")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (webIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(webIntent)
                } else {
                    Toast.makeText(context, "Google Maps is not available on this device.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            try {
                val encodedQuery = Uri.encode(query)
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Google Maps is not available on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
