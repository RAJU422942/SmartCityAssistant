package com.example.smartcityassistant.data.government

data class OfficialPortal(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val state: String? = null,
    val url: String,
    val sourceAuthority: String,
    val verificationStatus: String = "VERIFIED",
    val lastVerified: String = "2026-09",
    val keywords: List<String>
)
