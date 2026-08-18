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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import android.os.Environment
import coil.compose.AsyncImage
import com.app.zpower.data.entity.SubProcess
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.LightBrown
import kotlinx.coroutines.delay
import java.io.File

import androidx.compose.ui.draw.alpha

data class SubProcessDialogState(
    val name: String = "",
    val description: String = "",
    val imagePath: String = "",
    val work: String = "",
    val uses: String = ""
)

@Composable
fun SubProcessDialog(
    viewModel: NavigationViewModel,
    title: String,
    initialSubProcess: SubProcess? = null,
    parentId: Long = 0,
    parentType: String = "CHILD",
    suggestions: List<SubProcess> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<SubProcess>) -> Unit
) {
    var name by remember { mutableStateOf(initialSubProcess?.name ?: "") }
    var description by remember { mutableStateOf(initialSubProcess?.description ?: "") }
    var imagePath by remember { mutableStateOf(initialSubProcess?.imagePath ?: "") }
    var work by remember { mutableStateOf(initialSubProcess?.work ?: "") }
    var uses by remember { mutableStateOf(initialSubProcess?.uses ?: "") }

    // Undo/Redo logic
    var history by remember { mutableStateOf(listOf(SubProcessDialogState(
        name, description, imagePath, work, uses
    ))) }
    var historyIndex by remember { mutableStateOf(0) }

    fun pushToHistory(state: SubProcessDialogState) {
        if (state != history[historyIndex]) {
            val newHistory = history.subList(0, historyIndex + 1) + state
            history = newHistory.takeLast(50)
            historyIndex = history.size - 1
        }
    }

    LaunchedEffect(name, description, imagePath, work, uses) {
        delay(1000)
        pushToHistory(SubProcessDialogState(name, description, imagePath, work, uses))
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val state = history[historyIndex]
            name = state.name
            description = state.description
            imagePath = state.imagePath
            work = state.work
            uses = state.uses
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val state = history[historyIndex]
            name = state.name
            description = state.description
            imagePath = state.imagePath
            work = state.work
            uses = state.uses
        }
    }

    var showSuggestions by remember { mutableStateOf(false) }
    
    val addedItems = remember { mutableStateListOf<SubProcess>() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isProcessingImage by viewModel::isProcessingImage
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val blurIntensity by viewModel.blurIntensity.collectAsState()

    var showImageSourceSelector by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    
    var pendingSuggestion by remember { mutableStateOf<SubProcess?>(null) }
    var showDeepAutofillConfirm by remember { mutableStateOf(false) }

    if (showDeepAutofillConfirm && pendingSuggestion != null) {
        AlertDialog(
            onDismissRequest = { showDeepAutofillConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Deep Autofill") },
            text = { Text("Include all nested items from \"${pendingSuggestion?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        name = pendingSuggestion!!.name
                        description = pendingSuggestion!!.description
                        imagePath = pendingSuggestion!!.imagePath
                        work = pendingSuggestion!!.work
                        uses = pendingSuggestion!!.uses
                        showSuggestions = false
                        showDeepAutofillConfirm = false
                        pushToHistory(SubProcessDialogState(name, description, imagePath, work, uses))
                        
                        val item = pendingSuggestion!!.copy(id = 0)
                        item.fullBranch = true
                        item.sourceId = pendingSuggestion!!.id
                        addedItems.add(item)
                        
                        name = ""
                        description = ""
                        imagePath = ""
                        work = ""
                        uses = ""
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
                        work = pendingSuggestion!!.work
                        uses = pendingSuggestion!!.uses
                        showSuggestions = false
                        showDeepAutofillConfirm = false
                        pushToHistory(SubProcessDialogState(name, description, imagePath, work, uses))
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
                pushToHistory(SubProcessDialogState(name, description, fileName, work, uses))
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
                android.util.Log.d("CameraFlow", "URI generated for SubProcess: $uri")
                viewModel.saveImage(uri) { fileName ->
                    imagePath = fileName
                    pushToHistory(SubProcessDialogState(name, description, fileName, work, uses))
                    tempPhotoFile?.delete()
                    tempPhotoFile = null
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraFlow", "Failed to process SubProcess captured image", e)
                tempPhotoFile?.delete()
                tempPhotoFile = null
            }
        } else {
            android.util.Log.w("CameraFlow", "SubProcess capture failed or cancelled")
            tempPhotoFile?.delete()
            tempPhotoFile = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("CameraFlow", "SubProcess Camera permission granted: $isGranted")
        if (isGranted) {
            try {
                val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val cameraTempDir = File(publicDocs, "Gold Knowledge/temp")
                if (!cameraTempDir.exists()) cameraTempDir.mkdirs()
                
                val file = File(cameraTempDir, "sp_capture_${System.currentTimeMillis()}.jpg")
                if (file.exists()) file.delete()
                file.createNewFile()
                file.setWritable(true, false)
                
                android.util.Log.d("CameraFlow", "SubProcess Temp file created: ${file.absolutePath}")
                tempPhotoFile = file
                
                val uri = FileProvider.getUriForFile(
                    context, 
                    "com.app.zpower.fileprovider", 
                    file
                )
                
                val cameraIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                cameraIntent.addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                
                val chooser = android.content.Intent.createChooser(cameraIntent, "Capture Image With...")
                cameraLauncher.launch(chooser)
            } catch (e: Exception) {
                android.util.Log.e("CameraFlow", "Failed to prepare SubProcess camera capture", e)
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
                        Text("Added Sub-Processes:", fontWeight = FontWeight.Bold)
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
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                    items(filteredSuggestions) { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isProcessingImage) {
                                                    pendingSuggestion = suggestion
                                                    showDeepAutofillConfirm = true
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (suggestion.imagePath.isNotEmpty()) {
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
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .clickable(enabled = !isProcessingImage) { showImageSourceSelector = true }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imagePath.isNotEmpty()) {
                            val file = viewModel.repository.getLocalImageFile(context, imagePath)
                            AsyncImage(
                                model = file ?: imagePath,
                                contentDescription = "Sub-Process Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomEnd) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = accentColor)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Text("Select Image", color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = work,
                        onValueChange = { work = it },
                        label = { Text("Work") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessingImage
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = uses,
                        onValueChange = { uses = it },
                        label = { Text("Uses") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessingImage
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                addedItems.add(
                                    SubProcess(
                                        parentId = parentId,
                                        parentType = parentType,
                                        name = name,
                                        description = description,
                                        imagePath = imagePath,
                                        work = work,
                                        uses = uses
                                    )
                                )
                                name = ""
                                description = ""
                                imagePath = ""
                                work = ""
                                uses = ""
                                pushToHistory(SubProcessDialogState())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = accentColor,
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
                            finalItems.add(
                                SubProcess(
                                    id = initialSubProcess?.id ?: 0,
                                    parentId = initialSubProcess?.parentId ?: parentId,
                                    parentType = initialSubProcess?.parentType ?: parentType,
                                    name = name,
                                    description = description,
                                    imagePath = imagePath,
                                    work = work,
                                    uses = uses
                                )
                            )
                        }
                        onConfirm(finalItems)
                    },
                    enabled = (name.isNotBlank() || addedItems.isNotEmpty()) && !isProcessingImage,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    val totalCount = addedItems.size + (if (name.isNotBlank()) 1 else 0)
                    Text("Save All ${if (totalCount > 0) "($totalCount)" else ""}")
                }
            }
        },
        dismissButton = null
    )
}

