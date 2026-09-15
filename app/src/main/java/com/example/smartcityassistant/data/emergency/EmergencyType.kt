package com.example.smartcityassistant.data.emergency

enum class EmergencyType(val displayName: String, val description: String) {
    MEDICAL("Medical Emergency", "Ambulance, hospital, medical assistance"),
    POLICE("Police / Threat", "Police protection, crime or immediate threat"),
    FIRE("Fire", "Fire brigade and rescue operations"),
    ACCIDENT("Road Accident", "Accident response, medical & police aid"),
    DISASTER("Disaster", "Natural calamity, flood, earthquake help"),
    OTHER("Other Emergency", "General emergency assistance")
}
