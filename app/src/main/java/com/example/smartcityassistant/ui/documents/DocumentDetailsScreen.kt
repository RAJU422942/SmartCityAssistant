package com.example.smartcityassistant.ui.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartcityassistant.data.documents.DocumentItem
import com.example.smartcityassistant.data.documents.SecureDocumentStorage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DocumentDetailsDialog(
    doc: DocumentItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateDetails: (newName: String, newCategory: String, newNotes: String?, newExpiry: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(doc.name) }
    var category by remember { mutableStateOf(doc.category) }
    var notes by remember { mutableStateOf(doc.notes ?: "") }
    var expiryInput by remember { mutableStateOf(if (doc.expiryDate != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(doc.expiryDate)) else "") }

    val categories = listOf("Identity", "Education", "Government", "Employment", "Vehicle", "Health", "Financial", "Personal", "Other")
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val addedDateStr = dateFormat.format(Date(doc.createdAt))
    val formattedSize = SecureDocumentStorage.formatFileSize(doc.fileSize)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (doc.fileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Document Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFA000)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Document Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                Text(category)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = expiryInput,
                            onValueChange = { expiryInput = it },
                            label = { Text("Expiry Date (YYYY-MM-DD, optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Delete Document", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        DetailRow(label = "Name", value = doc.name)
                        DetailRow(label = "Category", value = doc.category)
                        DetailRow(label = "File Type", value = doc.fileType)
                        DetailRow(label = "File Size", value = formattedSize)
                        DetailRow(label = "Added On", value = addedDateStr)
                        if (doc.expiryDate != null) {
                            DetailRow(label = "Expiry Date", value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(doc.expiryDate)))
                        }
                        if (!doc.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Notes:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                            Text(text = doc.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: [ Edit Details ] [ Share ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isEditing) {
                                    var expMillis: Long? = null
                                    if (expiryInput.isNotBlank()) {
                                        try {
                                            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryInput)
                                            if (parsed != null) expMillis = parsed.time
                                        } catch (e: Exception) {}
                                    }
                                    onUpdateDetails(name, category, notes.ifBlank { null }, expMillis)
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isEditing) "Save" else "Edit Details",
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share",
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Row 2: [ Open ] [ Close ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onOpen,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open",
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Close",
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
