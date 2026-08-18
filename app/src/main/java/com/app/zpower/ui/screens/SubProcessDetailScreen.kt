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
import com.app.zpower.ui.components.DetailSection
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.components.SubProcessDialog
import com.app.zpower.ui.navigation.NavigationViewModel
import kotlinx.coroutines.launch
import com.app.zpower.ui.theme.BackgroundStyle
import com.app.zpower.ui.theme.LocalBackgroundStyle
import com.app.zpower.ui.theme.LocalGlassTextColor

@Composable
fun SubProcessDetailScreen(
    viewModel: NavigationViewModel,
    subProcessId: Long,
    subProcessName: String,
    onViewSubProcesses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSubProcesses by viewModel.repository.getSubProcessDao().getAllSubProcesses().collectAsState(emptyList())
    val subProcessSuggestions by viewModel.subProcessSuggestions.collectAsState()
    val currentSubProcess = allSubProcesses.find { it.id == subProcessId }
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
                    text = currentSubProcess?.name ?: subProcessName,
                    style = when(cardSize) {
                        "Small" -> MaterialTheme.typography.titleLarge
                        "Large" -> MaterialTheme.typography.headlineLarge
                        else -> MaterialTheme.typography.headlineMedium
                    },
                    color = if (isLiquidGlass) glassTextColor else accentColor
                )

                if (viewModel.isEditMode && currentSubProcess != null) {
                    Row {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = accentColor)
                        }
                        IconButton(onClick = { viewModel.deleteSubProcess(currentSubProcess) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentSubProcess != null) {
                if (currentSubProcess.imagePath.isNotEmpty()) {
                    val file = viewModel.repository.getLocalImageFile(androidx.compose.ui.platform.LocalContext.current, currentSubProcess.imagePath)
                    AsyncImage(
                        model = file ?: currentSubProcess.imagePath,
                        contentDescription = "Sub-Process Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { viewModel.showFullScreenImage(currentSubProcess.imagePath) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                DetailSection(title = "Description", content = currentSubProcess.description, accentColor = accentColor, cardSize = cardSize)
                DetailSection(title = "Work", content = currentSubProcess.work, accentColor = accentColor, cardSize = cardSize)
                DetailSection(title = "Uses", content = currentSubProcess.uses, accentColor = accentColor, cardSize = cardSize)

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

    if (showEditDialog && currentSubProcess != null) {
        SubProcessDialog(
            viewModel = viewModel,
            title = "Edit Sub-Process",
            initialSubProcess = currentSubProcess,
            suggestions = subProcessSuggestions,
            onDismiss = { showEditDialog = false },
            onConfirm = { items ->
                if (items.isNotEmpty()) {
                    val updated = items.first()
                    viewModel.updateSubProcess(updated, currentSubProcess)
                    
                    if (items.size > 1) {
                        scope.launch {
                            items.drop(1).forEach { sub ->
                                val newId = viewModel.repository.getSubProcessDao().insert(sub.copy(id = 0, parentId = currentSubProcess.parentId, parentType = currentSubProcess.parentType))
                                if (sub.fullBranch && sub.sourceId != null) {
                                    viewModel.repository.duplicateHierarchyBranch(sub.sourceId!!, "SUB_PROCESS", newId)
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
