package com.example.smartcityassistant.railway

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RailwayJsonDataSource {
    private val gson = Gson()

    fun loadStations(context: Context): List<RailwayStation> {
        return try {
            val inputStream = context.assets.open("railway/stations.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            val listType = object : TypeToken<List<RailwayStation>>() {}.type
            gson.fromJson<List<RailwayStation>>(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadTrains(context: Context): List<Train> {
        return try {
            val inputStream = context.assets.open("railway/trains.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            val listType = object : TypeToken<List<Train>>() {}.type
            gson.fromJson<List<Train>>(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
