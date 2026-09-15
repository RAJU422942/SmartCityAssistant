package com.example.smartcityassistant.data.documents

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class SaveResult(
    val path: String,
    val size: Long
)

object SecureDocumentStorage {
    fun saveFileToPrivateStorage(context: Context, sourceUri: Uri, fileName: String): SaveResult? {
        return try {
            val documentsDir = File(context.filesDir, "secure_documents").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(documentsDir, "${System.currentTimeMillis()}_$fileName")
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destinationFile.exists()) {
                SaveResult(destinationFile.absolutePath, destinationFile.length())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 KB"
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%d KB", kb.toLong().coerceAtLeast(1L))
        }
    }
}
