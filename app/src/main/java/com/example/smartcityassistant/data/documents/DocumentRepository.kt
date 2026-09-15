package com.example.smartcityassistant.data.documents

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class DocumentRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_city_secure_docs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DOCS = "document_items_list"
    }

    fun getDocuments(): List<DocumentItem> {
        val json = prefs.getString(KEY_DOCS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DocumentItem>>() {}.type
            val list = gson.fromJson<List<DocumentItem>>(json, type) ?: emptyList()
            // Ensure file sizes are accurate if 0
            list.map { doc ->
                if (doc.fileSize <= 0 && doc.uriString.isNotBlank()) {
                    val file = File(doc.uriString)
                    if (file.exists()) doc.copy(fileSize = file.length()) else doc
                } else {
                    doc
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDocument(doc: DocumentItem) {
        val current = getDocuments().toMutableList()
        current.removeAll { it.id == doc.id }
        current.add(0, doc)
        saveList(current)
    }

    fun updateDocument(updated: DocumentItem) {
        val current = getDocuments().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated.copy(updatedAt = System.currentTimeMillis())
            saveList(current)
        }
    }

    fun deleteDocument(id: String) {
        val current = getDocuments().toMutableList()
        val target = current.find { it.id == id }
        target?.let { SecureDocumentStorage.deleteFile(it.uriString) }
        current.removeAll { it.id == id }
        saveList(current)
    }

    private fun saveList(list: List<DocumentItem>) {
        try {
            val json = gson.toJson(list)
            prefs.edit().putString(KEY_DOCS, json).apply()
        } catch (e: Exception) {
        }
    }
}
