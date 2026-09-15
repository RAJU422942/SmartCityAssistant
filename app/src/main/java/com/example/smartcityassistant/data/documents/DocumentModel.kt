package com.example.smartcityassistant.data.documents

data class DocumentItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val category: String, // Identity, Education, Government, Employment, Vehicle, Health, Financial, Personal, Other
    val fileType: String, // PDF, IMAGE, SCAN
    val uriString: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val fileSize: Long = 0L
)
