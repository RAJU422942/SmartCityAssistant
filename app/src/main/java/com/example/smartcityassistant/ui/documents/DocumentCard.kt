package com.example.smartcityassistant.ui.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.smartcityassistant.data.documents.DocumentItem
import com.example.smartcityassistant.data.documents.SecureDocumentStorage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun DocumentCard(
    doc: DocumentItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onChangeCategory: () -> Unit,
    onSetExpiry: () -> Unit,
    onEditNotes: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val formattedSize = SecureDocumentStorage.formatFileSize(doc.fileSize)
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val addedDateStr = dateFormat.format(Date(doc.createdAt))

    // Expiry status calculation
    val expiryStatusText: String?
    val expiryStatusColor: Color
    val now = System.currentTimeMillis()
    if (doc.expiryDate != null) {
        val diffMillis = doc.expiryDate - now
        val daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis)
        when {
            daysLeft < 0 -> {
                expiryStatusText = "Expired"
                expiryStatusColor = Color(0xFFD32F2F) // Red
            }
            daysLeft <= 30 -> {
                expiryStatusText = "Expires in $daysLeft days"
                expiryStatusColor = Color(0xFFEF6C00) // Orange
            }
            else -> {
                expiryStatusText = "Expires in $daysLeft days"
                expiryStatusColor = Color(0xFF1E88E5) // Blue
            }
        }
    } else {
        expiryStatusText = null
        expiryStatusColor = Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFF1E88E5).copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (doc.fileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = doc.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (doc.isFavorite) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                            Text(doc.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                        }
                        Text("${doc.fileType} • $formattedSize", fontSize = 11.sp, color = Color.Gray)
                    }
                    if (expiryStatusText != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = expiryStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = expiryStatusColor
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open / View") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (doc.isFavorite) "Remove Favorite" else "Mark Favorite") },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        },
                        leadingIcon = { Icon(if (doc.isFavorite) Icons.Default.StarBorder else Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Change Category") },
                        onClick = {
                            showMenu = false
                            onChangeCategory()
                        },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Expiry Date") },
                        onClick = {
                            showMenu = false
                            onSetExpiry()
                        },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Notes") },
                        onClick = {
                            showMenu = false
                            onEditNotes()
                        },
                        leadingIcon = { Icon(Icons.Default.Note, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFD32F2F)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}
