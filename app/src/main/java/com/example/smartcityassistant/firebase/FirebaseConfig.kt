package com.example.smartcityassistant.firebase

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseConfig {
    fun initialize(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }
}
