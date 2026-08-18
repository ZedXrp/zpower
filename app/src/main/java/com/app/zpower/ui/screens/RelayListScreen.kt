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
import com.app.zpower.data.entity.RelayEntity
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.components.HierarchyEntityDialog
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassPurple
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.app.zpower.ui.components.SquareGlassCard

@Composable
fun RelayListScreen(
    viewModel: NavigationViewModel,
    panelId: Long,
    panelName: String,
    onRelayClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val relays by viewModel.repository.getRelayDao().getRelaysForPanel(panelId).collectAsState(initial = emptyList())
    val relaySuggestions by viewModel.relaySuggestions.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val cardSize by viewModel.cardSize.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var relayToEdit by remember { mutableStateOf<RelayEntity?>(null) }

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
            items(relays) { relay ->
                SquareGlassCard(
                    name = relay.name,
                    description = relay.description,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accentColor,
                    imagePath = relay.imagePath,
                    repository = viewModel.repository,
                    cardSize = cardSize,
                    isEditMode = viewModel.isEditMode,
                    blurIntensity = blurIntensity,
                    blurEnabled = blurEnabled,
                    backgroundStyle = backgroundStyle,
                    onNavigate = { onRelayClick(relay.id, relay.name) },
                    onDelete = { viewModel.deleteRelay(relay) },
                    onEdit = { relayToEdit = relay },
                    onLongPress = { viewModel.showPreview(relay.name, relay.description, relay.imagePath) },
                    onImageClick = {
                        relay.imagePath?.takeIf { it.isNotEmpty() }?.let { viewModel.showFullScreenImage(it) }
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
                Icon(Icons.Rounded.Add, contentDescription = "Add Relay")
            }
        }
    }

    if (showAddDialog) {
        HierarchyEntityDialog(
            viewModel = viewModel,
            title = "Add Relay in $panelName",
            suggestions = relaySuggestions.map { com.app.zpower.ui.components.HierarchyItem(it.name, it.description, it.imagePath, sourceId = it.id) },
            onDismiss = { showAddDialog = false },
            onConfirm = { items ->
                scope.launch {
                    items.forEach { item ->
                        val newId = viewModel.repository.getRelayDao().insert(
                            RelayEntity(
                                panelId = panelId,
                                name = item.name,
                                description = item.description,
                                imagePath = item.imagePath
                            )
                        )
                        if (item.fullBranch && item.sourceId != null) {
                            viewModel.repository.duplicateHierarchyBranch(item.sourceId, "RELAY", newId)
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }

    relayToEdit?.let { relay ->
        HierarchyEntityDialog(
            viewModel = viewModel,
            title = "Edit Relay",
            initialName = relay.name,
            initialDescription = relay.description,
            initialImagePath = relay.imagePath,
            suggestions = relaySuggestions.map { com.app.zpower.ui.components.HierarchyItem(it.name, it.description, it.imagePath, sourceId = it.id) },
            onDismiss = { relayToEdit = null },
            onConfirm = { items ->
                if (items.isNotEmpty()) {
                    val first = items.first()
                    viewModel.updateRelay(
                        relay.copy(
                            name = first.name,
                            description = first.description,
                            imagePath = first.imagePath
                        ),
                        relay
                    )
                    
                    if (items.size > 1) {
                        scope.launch {
                            items.drop(1).forEach { item ->
                                val newId = viewModel.repository.getRelayDao().insert(
                                    RelayEntity(
                                        panelId = relay.panelId,
                                        name = item.name,
                                        description = item.description,
                                        imagePath = item.imagePath
                                    )
                                )
                                if (item.fullBranch && item.sourceId != null) {
                                    viewModel.repository.duplicateHierarchyBranch(item.sourceId, "RELAY", newId)
                                }
                            }
                        }
                    }
                }
                relayToEdit = null
            }
        )
    }
}
