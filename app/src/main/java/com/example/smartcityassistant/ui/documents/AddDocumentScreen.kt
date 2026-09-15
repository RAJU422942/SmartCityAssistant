package com.example.smartcityassistant.ui.documents

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcityassistant.data.documents.DocumentItem
import com.example.smartcityassistant.data.documents.SecureDocumentStorage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    viewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var docName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Identity") }
    var fileType by remember { mutableStateOf("PDF") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var expiryInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var showSourceSheet by remember { mutableStateOf(false) }

    // Scan preview states
    var scannedUri by remember { mutableStateOf<Uri?>(null) }
    var isScanningPreview by remember { mutableStateOf(false) }

    val categories = listOf("Identity", "Education", "Government", "Employment", "Vehicle", "Health", "Financial", "Personal", "Other")

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            fileUri = it
            fileType = "PDF"
            if (docName.isBlank()) {
                docName = "Document_${System.currentTimeMillis()}"
            }
            showSourceSheet = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            fileUri = it
            fileType = "IMAGE"
            if (docName.isBlank()) {
                docName = "Image_${System.currentTimeMillis()}"
            }
            showSourceSheet = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val os = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                os.flush()
                os.close()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                scannedUri = uri
                isScanningPreview = true
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to save scanned document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
        showSourceSheet = false
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Capture failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Add Document", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scan Preview UI if a scan was just captured
            if (isScanningPreview) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Scanned Document Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))
                        
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Captured Scan Image", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D2B4E))
                                    Text("Ready to use in document details", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isScanningPreview = false
                                    scannedUri = null
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake")
                            }

                            Button(
                                onClick = {
                                    fileUri = scannedUri
                                    fileType = "SCAN"
                                    isScanningPreview = false
                                    if (docName.isBlank()) {
                                        docName = "Scan_${System.currentTimeMillis()}"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Scan")
                            }
                        }
                    }
                }
            } else {
                // Source Section / Add Document Button Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Select Document Source", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E), modifier = Modifier.align(Alignment.Start))

                        if (fileUri == null) {
                            Button(
                                onClick = { showSourceSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Document", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(
                                "PDF, image, or scan a physical document",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            // Selected File Preview Card
                            Surface(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
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
                                        Icon(
                                            if (fileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = Color(0xFF1E88E5),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = docName.ifBlank { "Selected Document" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF0D2B4E),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = fileType,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { showSourceSheet = true }) {
                                            Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { fileUri = null }) {
                                            Text("Remove", fontSize = 12.sp, color = Color(0xFFD32F2F))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Document Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Document Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D2B4E))

                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        label = { Text("Document Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(selectedCategory)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Save Document Button
            Button(
                onClick = {
                    if (docName.isBlank()) {
                        Toast.makeText(context, "Please enter a document name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (fileUri == null) {
                        Toast.makeText(context, "Please select a PDF or image file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val extension = when (fileType) {
                        "PDF" -> "pdf"
                        "SCAN" -> "jpg"
                        else -> "jpg"
                    }
                    val saveResult = SecureDocumentStorage.saveFileToPrivateStorage(context, fileUri!!, "$docName.$extension")
                    if (saveResult != null) {
                        var expMillis: Long? = null
                        if (expiryInput.isNotBlank()) {
                            try {
                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryInput)
                                if (parsed != null) expMillis = parsed.time
                            } catch (e: Exception) {}
                        }

                        val newItem = DocumentItem(
                            name = docName,
                            category = selectedCategory,
                            fileType = fileType,
                            uriString = saveResult.path,
                            fileSize = saveResult.size,
                            expiryDate = expMillis,
                            notes = notesInput.ifBlank { null }
                        )
                        viewModel.addDocument(newItem)
                        Toast.makeText(context, "Document saved securely", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Failed to save document securely", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2B4E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Document", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
    }

    // ModalBottomSheet for Document Source Selection
    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Document", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D2B4E))
                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: Import PDF
                Surface(
                    onClick = { pdfPicker.launch("application/pdf") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Import PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Choose a PDF from your device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Option 2: Import Image
                Surface(
                    onClick = { imagePicker.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Import Image", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Choose an image from your device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Option 3: Scan Document
                Surface(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Camera launch failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Scan Document", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Capture a physical document", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
