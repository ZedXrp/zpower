package com.app.zpower.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.zpower.ui.components.ChildProcessDialog
import com.app.zpower.ui.components.DetailSection
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.navigation.NavigationViewModel
import kotlinx.coroutines.launch
import com.app.zpower.ui.theme.BackgroundStyle
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.LocalBackgroundStyle
import com.app.zpower.ui.theme.LocalGlassTextColor

@Composable
fun ChildProcessDetailScreen(
    viewModel: NavigationViewModel,
    childProcessId: Long,
    childProcessName: String,
    onViewSubProcesses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allProcesses by viewModel.repository.getChildProcessDao().getAllChildProcesses().collectAsState(emptyList())
    val childProcessSuggestions by viewModel.childProcessSuggestions.collectAsState()
    val currentProcess = allProcesses.find { it.id == childProcessId }
    var showEditDialog by remember { mutableStateOf(false) }
    
    val backgroundStyle = LocalBackgroundStyle.current
    val glassTextColor = LocalGlassTextColor.current
    val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
    
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val blurIntensity by viewModel.blurIntensity.collectAsState()
    val blurEnabled by viewModel.blurEnabled.collectAsState()
    val cardSize by viewModel.cardSize.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GlassCard(
            accentColor = accentColor,
            blurIntensity = blurIntensity,
            blurEnabled = blurEnabled,
            cardSize = cardSize
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentProcess?.name ?: childProcessName,
                    style = when(cardSize) {
                        "Small" -> MaterialTheme.typography.titleLarge
                        "Large" -> MaterialTheme.typography.headlineLarge
                        else -> MaterialTheme.typography.headlineMedium
                    },
                    color = if (isLiquidGlass) glassTextColor else accentColor
                )

                if (viewModel.isEditMode && currentProcess != null) {
                    Row {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = accentColor)
                        }
                        IconButton(onClick = { viewModel.deleteChildProcess(currentProcess) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentProcess != null) {
                if (currentProcess.imagePath.isNotEmpty()) {
                    val file = viewModel.repository.getLocalImageFile(androidx.compose.ui.platform.LocalContext.current, currentProcess.imagePath)
                    AsyncImage(
                        model = file ?: currentProcess.imagePath,
                        contentDescription = "Process Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { viewModel.showFullScreenImage(currentProcess.imagePath) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                DetailSection(title = "Description", content = currentProcess.description, accentColor = accentColor, cardSize = cardSize)
                DetailSection(title = "Work", content = currentProcess.work, accentColor = accentColor, cardSize = cardSize)
                DetailSection(title = "Uses", content = currentProcess.uses, accentColor = accentColor, cardSize = cardSize)

                currentProcess.sections.forEach { section ->
                    DetailSection(title = section.title, content = section.content, accentColor = accentColor, cardSize = cardSize)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onViewSubProcesses,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("VIEW SUB-PROCESSES", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Text(text = "Loading details...", color = Color.Gray)
            }
        }
    }

    if (showEditDialog && currentProcess != null) {
        ChildProcessDialog(
            viewModel = viewModel,
            title = "Edit Process",
            initialProcess = currentProcess,
            suggestions = childProcessSuggestions,
            onDismiss = { showEditDialog = false },
            onConfirm = { items ->
                if (items.isNotEmpty()) {
                    val updated = items.first()
                    viewModel.updateChildProcess(updated, currentProcess)
                    
                    if (items.size > 1) {
                        scope.launch {
                            items.drop(1).forEach { process ->
                                val newId = viewModel.repository.getChildProcessDao().insert(process.copy(id = 0, relayId = currentProcess.relayId))
                                if (process.fullBranch && process.sourceId != null) {
                                    viewModel.repository.duplicateHierarchyBranch(process.sourceId!!, "CHILD_PROCESS", newId)
                                }
                            }
                        }
                    }
                }
                showEditDialog = false
            }
        )
    }
}


