package com.app.zpower.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.app.zpower.data.entity.PanelEntity
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.components.HierarchyEntityDialog
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassCyan
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.app.zpower.ui.components.SquareGlassCard

@Composable
fun PanelListScreen(
    viewModel: NavigationViewModel,
    roomId: Long,
    roomName: String,
    onPanelClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val panels by viewModel.repository.getPanelDao().getPanelsForRoom(roomId).collectAsState(initial = emptyList())
    val panelSuggestions by viewModel.panelSuggestions.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val cardSize by viewModel.cardSize.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var panelToEdit by remember { mutableStateOf<PanelEntity?>(null) }

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
            items(panels) { panel ->
                SquareGlassCard(
                    name = panel.name,
                    description = panel.description,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accentColor,
                    imagePath = panel.imagePath,
                    repository = viewModel.repository,
                    cardSize = cardSize,
                    isEditMode = viewModel.isEditMode,
                    blurIntensity = blurIntensity,
                    blurEnabled = blurEnabled,
                    backgroundStyle = backgroundStyle,
                    onNavigate = { onPanelClick(panel.id, panel.name) },
                    onDelete = { viewModel.deletePanel(panel) },
                    onEdit = { panelToEdit = panel },
                    onLongPress = { viewModel.showPreview(panel.name, panel.description, panel.imagePath) },
                    onImageClick = {
                        panel.imagePath?.takeIf { it.isNotEmpty() }?.let { viewModel.showFullScreenImage(it) }
                    }
                )
            }
        }

        if (viewModel.isEditMode) {
            LargeFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = accentColor,
                contentColor = Color.Black
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Panel")
            }
        }
    }

    if (showAddDialog) {
        HierarchyEntityDialog(
            viewModel = viewModel,
            title = "Add Panel in $roomName",
            suggestions = panelSuggestions.map { com.app.zpower.ui.components.HierarchyItem(it.name, it.description, it.imagePath, sourceId = it.id) },
            onDismiss = { showAddDialog = false },
            onConfirm = { items ->
                scope.launch {
                    items.forEach { item ->
                        val newId = viewModel.repository.getPanelDao().insert(
                            PanelEntity(
                                roomId = roomId,
                                name = item.name,
                                description = item.description,
                                imagePath = item.imagePath
                            )
                        )
                        if (item.fullBranch && item.sourceId != null) {
                            viewModel.repository.duplicateHierarchyBranch(item.sourceId, "PANEL", newId)
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }

    panelToEdit?.let { panel ->
        HierarchyEntityDialog(
            viewModel = viewModel,
            title = "Edit Panel",
            initialName = panel.name,
            initialDescription = panel.description,
            initialImagePath = panel.imagePath,
            suggestions = panelSuggestions.map { com.app.zpower.ui.components.HierarchyItem(it.name, it.description, it.imagePath, sourceId = it.id) },
            onDismiss = { panelToEdit = null },
            onConfirm = { items ->
                if (items.isNotEmpty()) {
                    val first = items.first()
                    viewModel.updatePanel(
                        panel.copy(
                            name = first.name,
                            description = first.description,
                            imagePath = first.imagePath
                        ),
                        panel
                    )
                    
                    if (items.size > 1) {
                        scope.launch {
                            items.drop(1).forEach { item ->
                                val newId = viewModel.repository.getPanelDao().insert(
                                    PanelEntity(
                                        roomId = panel.roomId,
                                        name = item.name,
                                        description = item.description,
                                        imagePath = item.imagePath
                                    )
                                )
                                if (item.fullBranch && item.sourceId != null) {
                                    viewModel.repository.duplicateHierarchyBranch(item.sourceId, "PANEL", newId)
                                }
                            }
                        }
                    }
                }
                panelToEdit = null
            }
        )
    }
}
