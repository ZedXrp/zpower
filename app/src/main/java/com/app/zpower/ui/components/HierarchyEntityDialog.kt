package com.app.zpower.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import android.os.Environment
import coil.compose.AsyncImage
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.LightBrown
import kotlinx.coroutines.delay
import java.io.File

data class HierarchyItem(
    val name: String,
    val description: String,
    val imagePath: String? = null,
    val sourceId: Long? = null,
    val fullBranch: Boolean = false
)

data class HierarchyDialogState(
    val name: String = "",
    val description: String = "",
    val imagePath: String? = null
)

@Composable
fun HierarchyEntityDialog(
    viewModel: NavigationViewModel,
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialImagePath: String? = null,
    suggestions: List<HierarchyItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<HierarchyItem>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var imagePath by remember { mutableStateOf(initialImagePath) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Undo/Redo logic
    var history by remember { mutableStateOf(listOf(HierarchyDialogState(initialName, initialDescription, initialImagePath))) }
    var historyIndex by remember { mutableStateOf(0) }

    fun pushToHistory(state: HierarchyDialogState) {
        if (state != history[historyIndex]) {
            val newHistory = history.subList(0, historyIndex + 1) + state
            history = newHistory.takeLast(50)
            historyIndex = history.size - 1
        }
    }

    LaunchedEffect(name, description, imagePath) {
        delay(1000) // Debounce typing changes
        pushToHistory(HierarchyDialogState(name, description, imagePath))
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val state = history[historyIndex]
            name = state.name
            description = state.description
            imagePath = state.imagePath
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val state = history[historyIndex]
            name = state.name
            description = state.description
            imagePath = state.imagePath
        }
    }
    
    val addedItems = remember { mutableStateListOf<HierarchyItem>() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val isProcessingImage by viewModel::isProcessingImage
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val blurIntensity by viewModel.blurIntensity.collectAsState()

    var showImageSourceSelector by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    
    var pendingSuggestion by remember { mutableStateOf<HierarchyItem?>(null) }
    var showDeepAutofillConfirm by remember { mutableStateOf(false) }

    if (showDeepAutofillConfirm && pendingSuggestion != null) {
        AlertDialog(
            onDismissRequest = { showDeepAutofillConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Deep Autofill") },
            text = { Text("Include all sub-items from \"${pendingSuggestion?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        name = pendingSuggestion!!.name
                        description = pendingSuggestion!!.description
                        imagePath = pendingSuggestion!!.imagePath
                        showSuggestions = false
                        showDeepAutofillConfirm = false
                        pushToHistory(HierarchyDialogState(name, description, imagePath))
                        
                        // Add to queue immediately for Deep Copy
                        addedItems.add(pendingSuggestion!!.copy(fullBranch = true))
                        name = ""
                        description = ""
                        imagePath = null
                        pendingSuggestion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Full Branch", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        name = pendingSuggestion!!.name
                        description = pendingSuggestion!!.description
                        imagePath = pendingSuggestion!!.imagePath
                        showSuggestions = false
                        showDeepAutofillConfirm = false
                        pushToHistory(HierarchyDialogState(name, description, imagePath))
                        pendingSuggestion = null
                    }
                ) {
                    Text("Data Only", color = LightBrown)
                }
            }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.saveImage(it) { fileName ->
                imagePath = fileName
                pushToHistory(HierarchyDialogState(name, description, fileName))
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && tempPhotoFile != null) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.app.zpower.fileprovider",
                    tempPhotoFile!!
                )
                android.util.Log.d("CameraFlow", "URI generated for saving: $uri")
                viewModel.saveImage(uri) { fileName ->
                    imagePath = fileName
                    pushToHistory(HierarchyDialogState(name, description, fileName))
                    tempPhotoFile?.delete()
                    tempPhotoFile = null
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraFlow", "Failed to process captured image", e)
                tempPhotoFile?.delete()
                tempPhotoFile = null
            }
        } else {
            android.util.Log.w("CameraFlow", "Capture failed or cancelled. result=${result.resultCode}, file=${tempPhotoFile?.absolutePath}")
            tempPhotoFile?.delete()
            tempPhotoFile = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("CameraFlow", "Camera permission granted: $isGranted")
        if (isGranted) {
            try {
                // Use Documents/Gold Knowledge/temp for camera capture
                val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val cameraTempDir = File(publicDocs, "Gold Knowledge/temp")
                if (!cameraTempDir.exists()) {
                    val created = cameraTempDir.mkdirs()
                    android.util.Log.d("CameraFlow", "Created temp dir: $created")
                }
                
                val file = File(cameraTempDir, "capture_${System.currentTimeMillis()}.jpg")
                if (file.exists()) file.delete()
                val fileCreated = file.createNewFile()
                
                // Ensure the file is writable by the camera app
                file.setWritable(true, false)
                
                android.util.Log.d("CameraFlow", "Temp file created: ${file.absolutePath}, success=$fileCreated")
                tempPhotoFile = file
                
                val uri = FileProvider.getUriForFile(
                    context, 
                    "com.app.zpower.fileprovider", 
                    file
                )
                android.util.Log.d("CameraFlow", "Launching camera with URI: $uri")
                
                val cameraIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                cameraIntent.addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                
                val chooser = android.content.Intent.createChooser(cameraIntent, "Capture Image With...")
                cameraLauncher.launch(chooser)
            } catch (e: Exception) {
                android.util.Log.e("CameraFlow", "Failed to prepare camera capture", e)
            }
        }
    }

    if (showImageSourceSelector) {
        ImageSourceSelector(
            onDismiss = { showImageSourceSelector = false },
            onSourceSelected = { isCamera ->
                showImageSourceSelector = false
                if (isCamera) {
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                } else {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = if (isProcessingImage) ({}) else onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 24.dp),
        title = { Text(text = title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 8.dp)
                        .alpha(if (isProcessingImage) 0.5f else 1f)
                ) {
                    if (addedItems.isNotEmpty()) {
                        Text("Added Items:", fontWeight = FontWeight.Bold)
                        addedItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${index + 1}. ${item.name}", modifier = Modifier.weight(1f))
                                IconButton(onClick = { addedItems.removeAt(index) }, enabled = !isProcessingImage) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = Color.Red)
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    TextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            showSuggestions = it.isNotEmpty()
                        },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessingImage
                    )

                    if (showSuggestions) {
                        val filteredSuggestions = suggestions.filter { 
                            it.name.contains(name, ignoreCase = true) && it.name != name 
                        }
                        if (filteredSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                    items(filteredSuggestions) { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isProcessingImage) {
                                                    if (suggestion.sourceId != null) {
                                                        pendingSuggestion = suggestion
                                                        showDeepAutofillConfirm = true
                                                    } else {
                                                        name = suggestion.name
                                                        description = suggestion.description
                                                        if (suggestion.imagePath != null) {
                                                            imagePath = suggestion.imagePath
                                                        }
                                                        showSuggestions = false
                                                        pushToHistory(HierarchyDialogState(name, description, imagePath))
                                                    }
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (suggestion.imagePath != null) {
                                                val file = viewModel.repository.getLocalImageFile(context, suggestion.imagePath)
                                                AsyncImage(
                                                    model = file ?: suggestion.imagePath,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Column {
                                                Text(text = suggestion.name, fontWeight = FontWeight.Bold)
                                                Text(text = suggestion.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessingImage
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Image Selection UI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .clickable(enabled = !isProcessingImage) { showImageSourceSelector = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imagePath != null) {
                            val file = viewModel.repository.getLocalImageFile(context, imagePath!!)
                            AsyncImage(
                                model = file ?: imagePath,
                                contentDescription = "Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomEnd) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = GlassCyan)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = Color.Gray)
                                Text("Select Image", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    GlassButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                addedItems.add(HierarchyItem(name, description, imagePath))
                                name = ""
                                description = ""
                                imagePath = null
                                pushToHistory(HierarchyDialogState())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = GlassCyan,
                        text = "Add to Queue (+)"
                    )
                }

                if (isProcessingImage) {
                    LiquidGlassLoading(accentColor = accentColor, blurIntensity = blurIntensity)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { undo() }, enabled = historyIndex > 0 && !isProcessingImage) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo, 
                        contentDescription = "Undo", 
                        tint = if (historyIndex > 0 && !isProcessingImage) accentColor else Color.Gray
                    )
                }
                IconButton(onClick = { redo() }, enabled = historyIndex < history.size - 1 && !isProcessingImage) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo, 
                        contentDescription = "Redo", 
                        tint = if (historyIndex < history.size - 1 && !isProcessingImage) accentColor else Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(onClick = onDismiss, enabled = !isProcessingImage) {
                    Text("Cancel", color = LightBrown)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { 
                        val finalItems = addedItems.toList().toMutableList()
                        if (name.isNotBlank()) {
                            finalItems.add(HierarchyItem(name, description, imagePath))
                        }
                        onConfirm(finalItems)
                    },
                    enabled = (name.isNotBlank() || addedItems.isNotEmpty()) && !isProcessingImage,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                )
 {
                    val totalCount = addedItems.size + (if (name.isNotBlank()) 1 else 0)
                    Text("Save All ${if (totalCount > 0) "($totalCount)" else ""}")
                }
            }
        },
        dismissButton = null
    )
}

