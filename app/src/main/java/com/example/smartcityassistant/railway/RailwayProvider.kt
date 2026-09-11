package com.example.smartcityassistant.railway

import android.content.Context
import com.example.smartcityassistant.BuildConfig

object RailwayProvider {
    fun getRepository(context: Context): RailwayRepository {
        return if (BuildConfig.MAPS_API_KEY != "YOUR_API_KEY_HERE" && BuildConfig.MAPS_API_KEY.isNotEmpty()) {
            RealRailwayRepository(context)
        } else {
            DemoRailwayRepository
        }
    }
}
