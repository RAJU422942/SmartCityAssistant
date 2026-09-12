package com.example.smartcityassistant.railway

import android.content.Context

object RailwayProvider {
    fun getRepository(context: Context): RailwayRepository {
        return RealRailwayRepository(context)
    }
}
