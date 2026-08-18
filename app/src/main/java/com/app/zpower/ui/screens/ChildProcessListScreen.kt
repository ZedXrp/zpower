package com.app.zpower.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.zpower.ui.components.ChildProcessDialog
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassCyan
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import com.app.zpower.ui.components.SquareGlassCard

@Composable
fun ChildProcessListScreen(
    viewModel: NavigationViewModel,
    relayId: Long,
    relayName: String,
    onChildProcessClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val processes by viewModel.repository.getChildProcessDao().getChildProcessesForRelay(relayId).collectAsState(initial = emptyList())
    val childProcessSuggestions by viewModel.childProcessSuggestions.collectAsState()
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
                    text = "Processes in $relayName",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = accentColor
                )
            }
            items(processes) { process ->
                SquareGlassCard(
                    name = process.name,
                    description = process.description,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accentColor,
                    imagePath = process.imagePath,
                    repository = viewModel.repository,
                    cardSize = cardSize,
                    isEditMode = viewModel.isEditMode,
                    blurIntensity = blurIntensity,
                    blurEnabled = blurEnabled,
                    backgroundStyle = backgroundStyle,
                    onNavigate = { onChildProcessClick(process.id, process.name) },
                    onDelete = { viewModel.deleteChildProcess(process) },
                    onEdit = null, // ChildProcess edit dialog not implemented in the same way as others yet, or handled by detail screen
                    onLongPress = { viewModel.showPreview(process.name, process.description, process.imagePath) },
                    onImageClick = {
                        process.imagePath?.takeIf { it.isNotEmpty() }?.let { viewModel.showFullScreenImage(it) }
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
                Icon(Icons.Rounded.Add, contentDescription = "Add Process")
            }
        }
    }

    if (showAddDialog) {
        ChildProcessDialog(
            viewModel = viewModel,
            title = "Add Process to $relayName",
            relayId = relayId,
            suggestions = childProcessSuggestions,
            onDismiss = { showAddDialog = false },
            onConfirm = { items ->
                scope.launch {
                    items.forEach { process ->
                        val newId = viewModel.repository.getChildProcessDao().insert(process)
                        if (process.fullBranch && process.sourceId != null) {
                            viewModel.repository.duplicateHierarchyBranch(process.sourceId!!, "CHILD_PROCESS", newId)
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }
}
