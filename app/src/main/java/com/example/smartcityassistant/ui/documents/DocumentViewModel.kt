package com.example.smartcityassistant.ui.documents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartcityassistant.data.documents.DocumentItem
import com.example.smartcityassistant.data.documents.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DocumentSortOption(val displayName: String) {
    RECENTLY_ADDED("Recently Added"),
    NAME_AZ("Name A-Z"),
    NAME_ZA("Name Z-A"),
    FILE_SIZE("File Size"),
    EXPIRY_DATE("Expiry Date")
}

enum class DocumentFilterOption(val displayName: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    PDF("PDFs"),
    IMAGE("Images")
}

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository(application)
    private val _documents = MutableStateFlow<List<DocumentItem>>(repository.getDocuments())
    val documents: StateFlow<List<DocumentItem>> = _documents.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(DocumentSortOption.RECENTLY_ADDED)
    val sortOption: StateFlow<DocumentSortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(DocumentFilterOption.ALL)
    val filterOption: StateFlow<DocumentFilterOption> = _filterOption.asStateFlow()

    private val _selectedDocumentForDetail = MutableStateFlow<DocumentItem?>(null)
    val selectedDocumentForDetail: StateFlow<DocumentItem?> = _selectedDocumentForDetail.asStateFlow()

    fun addDocument(doc: DocumentItem) {
        repository.saveDocument(doc)
        _documents.value = repository.getDocuments()
    }

    fun updateDocument(doc: DocumentItem) {
        repository.updateDocument(doc)
        _documents.value = repository.getDocuments()
        if (_selectedDocumentForDetail.value?.id == doc.id) {
            _selectedDocumentForDetail.value = doc
        }
    }

    fun deleteDocument(id: String) {
        repository.deleteDocument(id)
        _documents.value = repository.getDocuments()
        if (_selectedDocumentForDetail.value?.id == id) {
            _selectedDocumentForDetail.value = null
        }
    }

    fun toggleFavorite(doc: DocumentItem) {
        val updated = doc.copy(isFavorite = !doc.isFavorite)
        updateDocument(updated)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSortOption(sort: DocumentSortOption) {
        _sortOption.value = sort
    }

    fun setFilterOption(filter: DocumentFilterOption) {
        _filterOption.value = filter
    }

    fun selectDocumentForDetail(doc: DocumentItem?) {
        _selectedDocumentForDetail.value = doc
    }

    fun refresh() {
        _documents.value = repository.getDocuments()
    }
}
