package com.example.smartcityassistant.data.emergency

import java.util.UUID

data class EmergencyService(
    val id: String,
    val title: String,
    val subtitle: String,
    val phoneNumber: String,
    val category: String,
    val sourceName: String,
    val isPrimary: Boolean = false,
    val websiteUrl: String? = null
)

data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String
)
