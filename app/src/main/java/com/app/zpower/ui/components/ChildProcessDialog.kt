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
import com.app.zpower.data.entity.ChildProcess
import com.app.zpower.data.entity.CustomSection
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.LightBrown
import kotlinx.coroutines.delay
import java.io.File

import androidx.compose.ui.draw.alpha

data class ChildProcessDialogState(
    val name: String = "",
    val description: String = "",
    val imagePath: String = "",
    val work: String = "",
    val uses: String = "",
    val sections: List<CustomSection> = emptyList()
)

@Composable
fun ChildProcessDialog(
    viewModel: NavigationViewModel,
    title: String,
    initialProcess: ChildProcess? = null,
    relayId: Long = 0,
    suggestions: List<ChildProcess> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<ChildProcess>) -> Unit
) {
    var name by remember { mutableStateOf(initialProcess?.name ?: "") }
    var description by remember { mutableStateOf(initialProcess?.description ?: "") }
    var imagePath by remember { mutableStateOf(initialProcess?.imagePath ?: "") }
    var work by remember { mutableStateOf(initialProcess?.work ?: "") }
    var uses by remember { mutableStateOf(initialProcess?.uses ?: "") }
    val sections = remember { mutableStateListOf<CustomSection>().apply { 
        initialProcess?.sections?.let { addAll(it) } 
    } }

    // Undo/Redo logic
    var history by remember { mutableStateOf(listOf(ChildProcessDialogState(
        name, description, imagePath, work, uses, sections.toList()
    ))) }
    var historyIndex by remember { mutableStateOf(0) }

    fun pushToHistory(state: ChildProcessDialogState) {
        if (state != history[historyIndex]) {
            val newHistory = history.subList(0, historyIndex + 1) + state
            history = newHistory.takeLast(50)
            historyIndex = history.size - 1
        }
    }

    LaunchedEffect(name, description, imagePath, work, uses, sections.size) {
        delay(1000)
        pushToHistory(ChildProcessDialogState(name, description, imagePath, work, uses, sections.toList()))
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
            sections.clear()
            sections.addAll(state.sections)
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
            sections.clear()
            sections.addAll(state.sections)
        }
    }

    var newSectionTitle by remember { mutableStateOf("") }
    var newSectionContent by remember { mutableStateOf("") }

    var showSuggestions by remember { mutableStateOf(false) }
    
    val addedItems = remember { mutableStateListOf<ChildProcess>() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isProcessingImage by viewModel::isProcessingImage
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val blurIntensity by viewModel.blurIntensity.collectAsState()

    var showImageSourceSelector by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    
    var pendingSuggestion by remember { mutableStateOf<ChildProcess?>(null) }
    var showDeepAutofillConfirm by remember { mutableStateOf(false) }

    if (showDeepAutofillConfirm && pendingSuggestion != null) {
        AlertDialog(
            onDismissRequest = { showDeepAutofillConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Deep Autofill") },
            text = { Text("Include all sub-processes from \"${pendingSuggestion?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        name = pendingSuggestion!!.name
                        description = pendingSuggestion!!.description
                        imagePath = pendingSuggestion!!.imagePath
                        work = pendingSuggestion!!.work
                        uses = pendingSuggestion!!.uses
                        sections.clear()
                        sections.addAll(pendingSuggestion!!.sections)
                        showSuggestions = false
                        showDeepAutofillConfirm = false
                        pushToHistory(ChildProcessDialogState(name, description, imagePath, work, uses, sections.toList()))
                        
                        val item = pendingSuggestion!!.copy(id = 0)
                        item.fullBranch = true
                        item.sourceId = pendingSuggestion!!.id
                        addedItems.add(item)
                        
                        name = ""
                        description = ""
                        imagePath = ""
                        work = ""
                        uses = ""
                        sections.clear()
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
                        sections.clear()
                        sections.addAll(pendingSuggestion!!.sections)
                        showSuggestions = false
                        showDeepAutofillConfirm = false
                        pushToHistory(ChildProcessDialogState(name, description, imagePath, work, uses, sections.toList()))
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
                pushToHistory(ChildProcessDialogState(name, description, fileName, work, uses, sections.toList()))
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
                android.util.Log.d("CameraFlow", "URI generated for ChildProcess: $uri")
                viewModel.saveImage(uri) { fileName ->
                    imagePath = fileName
                    pushToHistory(ChildProcessDialogState(name, description, fileName, work, uses, sections.toList()))
                    tempPhotoFile?.delete()
                    tempPhotoFile = null
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraFlow", "Failed to process ChildProcess captured image", e)
                tempPhotoFile?.delete()
                tempPhotoFile = null
            }
        } else {
            android.util.Log.w("CameraFlow", "ChildProcess capture failed or cancelled")
            tempPhotoFile?.delete()
            tempPhotoFile = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("CameraFlow", "ChildProcess Camera permission granted: $isGranted")
        if (isGranted) {
            try {
                val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val cameraTempDir = File(publicDocs, "Gold Knowledge/temp")
                if (!cameraTempDir.exists()) cameraTempDir.mkdirs()
                
                val file = File(cameraTempDir, "cp_capture_${System.currentTimeMillis()}.jpg")
                if (file.exists()) file.delete()
                file.createNewFile()
                file.setWritable(true, false)
                
                android.util.Log.d("CameraFlow", "ChildProcess Temp file created: ${file.absolutePath}")
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
                android.util.Log.e("CameraFlow", "Failed to prepare ChildProcess camera capture", e)
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
                        Text("Added Processes:", fontWeight = FontWeight.Bold)
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
                                contentDescription = "Process Image",
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
                    Text("Custom Sections", fontWeight = FontWeight.Bold)
                    sections.forEachIndexed { index, section ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(section.title, fontWeight = FontWeight.Bold, color = accentColor)
                                    IconButton(onClick = { sections.removeAt(index) }, enabled = !isProcessingImage) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Section", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                                Text(section.content, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            TextField(
                                value = newSectionTitle,
                                onValueChange = { newSectionTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessingImage
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextField(
                                value = newSectionContent,
                                onValueChange = { newSectionContent = it },
                                label = { Text("Content") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessingImage
                            )
                        }
                        IconButton(
                            onClick = {
                                if (newSectionTitle.isNotBlank()) {
                                    sections.add(CustomSection(newSectionTitle, newSectionContent))
                                    newSectionTitle = ""
                                    newSectionContent = ""
                                }
                            },
                            enabled = newSectionTitle.isNotBlank() && !isProcessingImage
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Section", tint = accentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                addedItems.add(
                                    ChildProcess(
                                        relayId = relayId,
                                        name = name,
                                        description = description,
                                        imagePath = imagePath,
                                        work = work,
                                        uses = uses,
                                        sections = sections.toList()
                                    )
                                )
                                name = ""
                                description = ""
                                imagePath = ""
                                work = ""
                                uses = ""
                                sections.clear()
                                pushToHistory(ChildProcessDialogState())
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
                                ChildProcess(
                                    id = initialProcess?.id ?: 0,
                                    relayId = initialProcess?.relayId ?: relayId,
                                    name = name,
                                    description = description,
                                    imagePath = imagePath,
                                    work = work,
                                    uses = uses,
                                    sections = sections.toList()
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

