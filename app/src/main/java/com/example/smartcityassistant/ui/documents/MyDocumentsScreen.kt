package com.example.smartcityassistant.ui.documents

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartcityassistant.data.documents.DocumentItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyDocumentsScreen(
    viewModel: DocumentViewModel,
    onNavigateAdd: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf(DocumentSortOption.RECENTLY_ADDED) }
    var filterOption by remember { mutableStateOf(DocumentFilterOption.ALL) }
    
    var docToDelete by remember { mutableStateOf<DocumentItem?>(null) }
    var docForDetails by remember { mutableStateOf<DocumentItem?>(null) }
    
    // Dialog states for quick actions
    var showRenameDialog by remember { mutableStateOf<DocumentItem?>(null) }
    var newNameInput by remember { mutableStateOf("") }
    
    var showCategoryDialog by remember { mutableStateOf<DocumentItem?>(null) }
    var showExpiryDialog by remember { mutableStateOf<DocumentItem?>(null) }
    var expiryInput by remember { mutableStateOf("") }
    
    var showNotesDialog by remember { mutableStateOf<DocumentItem?>(null) }
    var notesInput by remember { mutableStateOf("") }

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val categories = listOf("All", "Identity", "Education", "Government", "Employment", "Vehicle", "Health", "Financial", "Personal", "Other")

    // Filter & Sort logic
    val now = System.currentTimeMillis()
    val filteredDocs = documents.filter { doc ->
        val matchesSearch = searchQuery.isBlank() ||
            doc.name.contains(searchQuery, true) ||
            doc.category.contains(searchQuery, true) ||
            doc.fileType.contains(searchQuery, true) ||
            (doc.notes != null && doc.notes.contains(searchQuery, true))

        val matchesCategory = selectedCategory == "All" || doc.category.equals(selectedCategory, true)

        val matchesFilter = when (filterOption) {
            DocumentFilterOption.ALL -> true
            DocumentFilterOption.FAVORITES -> doc.isFavorite
            DocumentFilterOption.EXPIRING_SOON -> doc.expiryDate != null && doc.expiryDate > now && (doc.expiryDate - now) <= 30L * 24 * 60 * 60 * 1000
            DocumentFilterOption.EXPIRED -> doc.expiryDate != null && doc.expiryDate < now
            DocumentFilterOption.PDF -> doc.fileType == "PDF"
            DocumentFilterOption.IMAGE -> doc.fileType == "IMAGE" || doc.fileType == "SCAN"
        }

        matchesSearch && matchesCategory && matchesFilter
    }.sortedWith(
        when (sortOption) {
            DocumentSortOption.RECENTLY_ADDED -> compareByDescending { it.createdAt }
            DocumentSortOption.NAME_AZ -> compareBy { it.name.lowercase() }
            DocumentSortOption.NAME_ZA -> compareByDescending { it.name.lowercase() }
            DocumentSortOption.FILE_SIZE -> compareByDescending { it.fileSize }
            DocumentSortOption.EXPIRY_DATE -> compareBy { it.expiryDate ?: Long.MAX_VALUE }
        }
    )

    fun openDocument(doc: DocumentItem) {
        try {
            val file = File(doc.uriString)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (doc.fileType == "PDF") "application/pdf" else "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Document file is unavailable.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No viewer available to open this document.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDocument(doc: DocumentItem) {
        try {
            val file = File(doc.uriString)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (doc.fileType == "PDF") "application/pdf" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Document"))
            } else {
                Toast.makeText(context, "Document file is unavailable.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share this document.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D2B4E),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text("My Documents", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Your secure document vault", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onNavigateAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add Document", tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Card
            DocumentSummaryCard(documents = documents)

            // Search, Filter & Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search documents, notes...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Filter Button
                Box {
                    OutlinedButton(
                        onClick = { showFilterMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        DocumentFilterOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.displayName, fontSize = 13.sp, fontWeight = if (filterOption == opt) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    filterOption = opt
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // Sort Button
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DocumentSortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.displayName, fontSize = 13.sp, fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    sortOption = opt
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Category Navigation Row
            DocumentCategoryRow(
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = { selectedCategory = it },
                enabled = searchQuery.isBlank()
            )

            // Unified Quick Action: [ + Add Document ] ONLY
            Button(
                onClick = onNavigateAdd,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Document", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Document List / Empty States
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (documents.isEmpty()) "No documents yet" else "No matching documents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (documents.isEmpty()) "Keep your important documents securely in one place." else "Try a different search query, category, or filter.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        if (documents.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateAdd,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Document")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredDocs) { doc ->
                        DocumentCard(
                            doc = doc,
                            onClick = { docForDetails = doc },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) },
                            onShare = { shareDocument(doc) },
                            onRename = {
                                newNameInput = doc.name
                                showRenameDialog = doc
                            },
                            onChangeCategory = { showCategoryDialog = doc },
                            onSetExpiry = {
                                expiryInput = if (doc.expiryDate != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(doc.expiryDate)) else ""
                                showExpiryDialog = doc
                            },
                            onEditNotes = {
                                notesInput = doc.notes ?: ""
                                showNotesDialog = doc
                            },
                            onDelete = { docToDelete = doc }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Details / Preview Dialog
    docForDetails?.let { doc ->
        DocumentDetailsDialog(
            doc = doc,
            onDismiss = { docForDetails = null },
            onOpen = { openDocument(doc) },
            onShare = { shareDocument(doc) },
            onToggleFavorite = { viewModel.toggleFavorite(doc) },
            onUpdateDetails = { newName, newCategory, newNotes, newExpiry ->
                val updated = doc.copy(
                    name = newName,
                    category = newCategory,
                    notes = newNotes,
                    expiryDate = newExpiry
                )
                viewModel.updateDocument(updated)
                docForDetails = updated
            },
            onDelete = {
                docForDetails = null
                docToDelete = doc
            }
        )
    }

    // Rename Dialog
    showRenameDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Document", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newNameInput.isNotBlank()) {
                        viewModel.updateDocument(doc.copy(name = newNameInput))
                    }
                    showRenameDialog = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Change Category Dialog
    showCategoryDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showCategoryDialog = null },
            title = { Text("Change Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.filter { it != "All" }.forEach { cat ->
                        TextButton(
                            onClick = {
                                viewModel.updateDocument(doc.copy(category = cat))
                                showCategoryDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cat, fontWeight = if (doc.category == cat) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set Expiry Dialog
    showExpiryDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showExpiryDialog = null },
            title = { Text("Set Expiry Date", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiryInput,
                        onValueChange = { expiryInput = it },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Leave blank to remove expiry tracking.", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    var expMillis: Long? = null
                    if (expiryInput.isNotBlank()) {
                        try {
                            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryInput)
                            if (parsed != null) expMillis = parsed.time
                        } catch (e: Exception) {}
                    }
                    viewModel.updateDocument(doc.copy(expiryDate = expMillis))
                    showExpiryDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Edit Notes Dialog
    showNotesDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showNotesDialog = null },
            title = { Text("Edit Notes", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateDocument(doc.copy(notes = notesInput.ifBlank { null }))
                    showNotesDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Delete Document", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${doc.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc.id)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
