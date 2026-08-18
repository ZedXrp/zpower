package com.app.zpower.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.zpower.ui.components.SquareGlassCard
import com.app.zpower.ui.components.SubProcessDialog
import com.app.zpower.ui.navigation.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun SubProcessListScreen(
    viewModel: NavigationViewModel,
    parentId: Long,
    parentType: String,
    parentName: String,
    onSubProcessClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subProcesses by viewModel.repository.getSubProcessDao().getSubProcessesByParent(parentId, parentType).collectAsState(initial = emptyList())
    val subProcessSuggestions by viewModel.subProcessSuggestions.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val cardSize by viewModel.cardSize.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    val blurIntensity by viewModel.blurIntensity.collectAsState()
    val blurEnabled by viewModel.blurEnabled.collectAsState()

    val columns = when (cardSize) {
        "Small" -> 3
        "Large" -> 1
        else -> 2
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = "Sub-Processes in $parentName",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = accentColor
                )
            }
            items(subProcesses) { subProcess ->
                SquareGlassCard(
                    name = subProcess.name,
                    description = subProcess.description,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accentColor,
                    imagePath = subProcess.imagePath,
                    repository = viewModel.repository,
                    cardSize = cardSize,
                    isEditMode = viewModel.isEditMode,
                    blurIntensity = blurIntensity,
                    blurEnabled = blurEnabled,
                    backgroundStyle = backgroundStyle,
                    onNavigate = { onSubProcessClick(subProcess.id, subProcess.name) },
                    onDelete = { viewModel.deleteSubProcess(subProcess) },
                    onEdit = null, 
                    onLongPress = { viewModel.showPreview(subProcess.name, subProcess.description, subProcess.imagePath) },
                    onImageClick = {
                        subProcess.imagePath.takeIf { it.isNotEmpty() }?.let { viewModel.showFullScreenImage(it) }
                    }
                )
            }
        }

        LargeFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = accentColor,
            contentColor = Color.Black
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Sub-Process")
        }
    }

    if (showAddDialog) {
        SubProcessDialog(
            viewModel = viewModel,
            title = "Add Sub-Process to $parentName",
            parentId = parentId,
            parentType = parentType,
            suggestions = subProcessSuggestions,
            onDismiss = { showAddDialog = false },
            onConfirm = { items ->
                scope.launch {
                    items.forEach { subProcess ->
                        val newId = viewModel.repository.getSubProcessDao().insert(subProcess)
                        if (subProcess.fullBranch && subProcess.sourceId != null) {
                            viewModel.repository.duplicateHierarchyBranch(subProcess.sourceId!!, "SUB_PROCESS", newId)
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }
}
