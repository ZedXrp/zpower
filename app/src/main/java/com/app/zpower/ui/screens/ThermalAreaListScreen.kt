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
import com.app.zpower.data.entity.ThermalArea
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.components.HierarchyEntityDialog
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassEmerald
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.app.zpower.ui.components.SquareGlassCard

@Composable
fun ThermalAreaListScreen(
    viewModel: NavigationViewModel,
    onAreaClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val areas by viewModel.repository.getThermalAreaDao().getAllThermalAreas().collectAsState(initial = emptyList())
    val areaSuggestions by viewModel.thermalAreaSuggestions.collectAsState()
    val rootTitle by viewModel.rootTitle.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val cardSize by viewModel.cardSize.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val accentColor = Color(accentColorInt)
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var areaToEdit by remember { mutableStateOf<ThermalArea?>(null) }

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
            items(areas) { area ->
                SquareGlassCard(
                    name = area.name,
                    description = area.description,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accentColor,
                    imagePath = area.imagePath,
                    repository = viewModel.repository,
                    cardSize = cardSize,
                    isEditMode = viewModel.isEditMode,
                    blurIntensity = blurIntensity,
                    blurEnabled = blurEnabled,
                    backgroundStyle = backgroundStyle,
                    onNavigate = { onAreaClick(area.id, area.name) },
                    onDelete = { viewModel.deleteThermalArea(area) },
                    onEdit = { areaToEdit = area },
                    onLongPress = { viewModel.showPreview(area.name, area.description, area.imagePath) },
                    onImageClick = {
                        area.imagePath?.takeIf { it.isNotEmpty() }?.let { viewModel.showFullScreenImage(it) }
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
                Icon(Icons.Rounded.Add, contentDescription = "Add $rootTitle")
            }
        }
    }

    if (showAddDialog) {
        HierarchyEntityDialog(
            viewModel = viewModel,
            title = "Add $rootTitle",
            suggestions = areaSuggestions.map { com.app.zpower.ui.components.HierarchyItem(it.name, it.description, it.imagePath, sourceId = it.id) },
            onDismiss = { showAddDialog = false },
            onConfirm = { items ->
                scope.launch {
                    items.forEach { item ->
                        val newId = viewModel.repository.getThermalAreaDao().insert(
                            ThermalArea(
                                name = item.name,
                                description = item.description,
                                imagePath = item.imagePath
                            )
                        )
                        if (item.fullBranch && item.sourceId != null) {
                            viewModel.repository.duplicateHierarchyBranch(item.sourceId, "THERMAL_AREA", newId)
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }

    areaToEdit?.let { area ->
        HierarchyEntityDialog(
            viewModel = viewModel,
            title = "Edit $rootTitle",
            initialName = area.name,
            initialDescription = area.description,
            initialImagePath = area.imagePath,
            suggestions = areaSuggestions.map { com.app.zpower.ui.components.HierarchyItem(it.name, it.description, it.imagePath, sourceId = it.id) },
            onDismiss = { areaToEdit = null },
            onConfirm = { items ->
                if (items.isNotEmpty()) {
                    val first = items.first()
                    viewModel.updateThermalArea(
                        area.copy(
                            name = first.name,
                            description = first.description,
                            imagePath = first.imagePath
                        ),
                        area
                    )
                    
                    if (items.size > 1) {
                        scope.launch {
                            items.drop(1).forEach { item ->
                                val newId = viewModel.repository.getThermalAreaDao().insert(
                                    ThermalArea(
                                        name = item.name,
                                        description = item.description,
                                        imagePath = item.imagePath
                                    )
                                )
                                if (item.fullBranch && item.sourceId != null) {
                                    viewModel.repository.duplicateHierarchyBranch(item.sourceId, "THERMAL_AREA", newId)
                                }
                            }
                        }
                    }
                }
                areaToEdit = null
            }
        )
    }
}

