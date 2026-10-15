package com.app.zpower.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.app.zpower.ui.components.AboutDialog
import com.app.zpower.ui.components.Breadcrumbs
import com.app.zpower.ui.components.CardPreviewOverlay
import com.app.zpower.ui.components.FullScreenImageViewer
import com.app.zpower.ui.components.LiquidGlassLoading
import com.app.zpower.ui.components.SearchResultsOverlay
import com.app.zpower.ui.components.ZPowerTopBar
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.navigation.ZPowerRoute
import com.app.zpower.ui.screens.*

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NavigationViewModel = viewModel()
) {
    if (viewModel.showAbout) {
        AboutDialog(onDismiss = { viewModel.showAbout = false })
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<ZPowerRoute>(directive = directive)
    val searchResults by viewModel.searchResults.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val blurIntensity by viewModel.blurIntensity.collectAsState()
    val blurEnabled by viewModel.blurEnabled.collectAsState()
    val rootTitle by viewModel.rootTitle.collectAsState()

    val backgroundBrush = when (backgroundStyle) {
        "brushed_metal" -> Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D)))
        "digital_grid" -> Brush.radialGradient(listOf(Color(0xFF1B2735), Color(0xFF090A0F)))
        "industrial_border_glow" -> Brush.verticalGradient(listOf(Color.Black, Color.Black)) // Deep black center
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364)
            )
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                ZPowerTopBar(viewModel = viewModel)
                Breadcrumbs(
                    backStack = viewModel.backStack,
                    rootTitle = rootTitle,
                    onLevelClick = { viewModel.navigateToLevel(it) }
                )
            }
        },
        snackbarHost = { SnackbarHost(viewModel.snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred Background Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurEnabled) Modifier.blur(blurIntensity.dp) else Modifier)
                    .background(backgroundBrush)
                    .drawBehind {
                        if (backgroundStyle == "digital_grid") {
                            val gridSize = 40.dp.toPx()
                            val color = Color.Cyan.copy(alpha = 0.05f)
                            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                                drawLine(color, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height), strokeWidth = 1f)
                            }
                            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()), strokeWidth = 1f)
                            }
                        } else if (backgroundStyle == "brushed_metal") {
                            // Subtle diagonal lines for brushed metal effect
                            val spacing = 4.dp.toPx()
                            val color = Color.White.copy(alpha = 0.02f)
                            for (i in -size.height.toInt()..size.width.toInt() step spacing.toInt()) {
                                drawLine(
                                    color = color,
                                    start = androidx.compose.ui.geometry.Offset(i.toFloat(), 0f),
                                    end = androidx.compose.ui.geometry.Offset(i.toFloat() + size.height, size.height),
                                    strokeWidth = 1f
                                )
                            }
                        } else if (backgroundStyle == "industrial_border_glow") {
                            val glowWidth = 120.dp.toPx()
                            val color = accentColor.copy(alpha = 0.4f)
                            
                            // Top Glow
                            drawRect(
                                brush = Brush.verticalGradient(listOf(color, Color.Transparent), endY = glowWidth),
                                size = size.copy(height = glowWidth)
                            )
                            // Bottom Glow
                            drawRect(
                                brush = Brush.verticalGradient(listOf(Color.Transparent, color), startY = size.height - glowWidth),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - glowWidth)
                            )
                            // Left Glow
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(color, Color.Transparent), endX = glowWidth),
                                size = size.copy(width = glowWidth)
                            )
                            // Right Glow
                            drawRect(
                                brush = Brush.horizontalGradient(listOf(Color.Transparent, color), startX = size.width - glowWidth),
                                topLeft = androidx.compose.ui.geometry.Offset(size.width - glowWidth, 0f)
                            )
                        }
                    }
            )

            // Content Layer (Sharp)
            val refreshTrigger by viewModel.refreshTrigger.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                key(refreshTrigger) {
                    NavDisplay(
                        backStack = viewModel.backStack,
                        onBack = { viewModel.pop() },
                        sceneStrategy = listDetailStrategy,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp), // Premium spacing from edges
                        entryProvider = entryProvider {
                            entry<ZPowerRoute.Settings>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) {
                                SettingsScreen(viewModel = viewModel)
                            }

                            entry<ZPowerRoute.ThermalAreaList>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "Select an area to see rooms",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                )
                            ) {
                                ThermalAreaListScreen(
                                    viewModel = viewModel,
                                    onAreaClick = { id, name ->
                                        viewModel.push(ZPowerRoute.RoomList(id, name))
                                    }
                                )
                            }
                            
                            entry<ZPowerRoute.RoomList>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "Select a room to see panels",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                )
                            ) { route ->
                                RoomListScreen(
                                    viewModel = viewModel,
                                    thermalAreaId = route.thermalAreaId,
                                    thermalAreaName = route.thermalAreaName,
                                    onRoomClick = { id, name ->
                                        viewModel.push(ZPowerRoute.PanelList(id, name, route.thermalAreaName))
                                    }
                                )
                            }

                            entry<ZPowerRoute.PanelList>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "Select a panel to see relays",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                )
                            ) { route ->
                                PanelListScreen(
                                    viewModel = viewModel,
                                    roomId = route.roomId,
                                    roomName = route.roomName,
                                    onPanelClick = { id, name ->
                                        viewModel.push(ZPowerRoute.RelayList(id, name, route.roomName, route.thermalAreaName))
                                    }
                                )
                            }

                            entry<ZPowerRoute.RelayList>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "Select a relay to see processes",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                )
                            ) { route ->
                                RelayListScreen(
                                    viewModel = viewModel,
                                    panelId = route.panelId,
                                    panelName = route.panelName,
                                    onRelayClick = { id, name ->
                                        viewModel.push(ZPowerRoute.ChildProcessList(id, name, route.panelName, route.roomName, route.thermalAreaName))
                                    }
                                )
                            }

                            entry<ZPowerRoute.ChildProcessList>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "Select a process to see details",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                )
                            ) { route ->
                                ChildProcessListScreen(
                                    viewModel = viewModel,
                                    relayId = route.relayId,
                                    relayName = route.relayName,
                                    onChildProcessClick = { id, name ->
                                        viewModel.push(ZPowerRoute.ChildProcessDetail(id, name, route.relayName, route.panelName, route.roomName, route.thermalAreaName))
                                    }
                                )
                            }

                            entry<ZPowerRoute.ChildProcessDetail>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) { route ->
                                ChildProcessDetailScreen(
                                    viewModel = viewModel,
                                    childProcessId = route.childProcessId,
                                    childProcessName = route.childProcessName,
                                    onViewSubProcesses = {
                                        viewModel.push(ZPowerRoute.SubProcessList(
                                            parentId = route.childProcessId,
                                            parentType = "CHILD",
                                            parentName = route.childProcessName,
                                            relayName = route.relayName,
                                            panelName = route.panelName,
                                            roomName = route.roomName,
                                            thermalAreaName = route.thermalAreaName
                                        ))
                                    }
                                )
                            }

                            entry<ZPowerRoute.SubProcessList>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "Select a sub-process to see details",
                                                    modifier = Modifier.padding(16.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                )
                            ) { route ->
                                SubProcessListScreen(
                                    viewModel = viewModel,
                                    parentId = route.parentId,
                                    parentType = route.parentType,
                                    parentName = route.parentName,
                                    onSubProcessClick = { id, name ->
                                        viewModel.push(ZPowerRoute.SubProcessDetail(
                                            subProcessId = id,
                                            subProcessName = name,
                                            parentName = route.parentName,
                                            relayName = route.relayName,
                                            panelName = route.panelName,
                                            roomName = route.roomName,
                                            thermalAreaName = route.thermalAreaName
                                        ))
                                    }
                                )
                            }

                            entry<ZPowerRoute.SubProcessDetail>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) { route ->
                                SubProcessDetailScreen(
                                    viewModel = viewModel,
                                    subProcessId = route.subProcessId,
                                    subProcessName = route.subProcessName,
                                    onViewSubProcesses = {
                                        viewModel.push(ZPowerRoute.SubProcessList(
                                            parentId = route.subProcessId,
                                            parentType = "SUB",
                                            parentName = route.subProcessName,
                                            relayName = route.relayName,
                                            panelName = route.panelName,
                                            roomName = route.roomName,
                                            thermalAreaName = route.thermalAreaName
                                        ))
                                    }
                                )
                            }
                        }
                    )
                }

                if (viewModel.isSearching && viewModel.searchQuery.isNotEmpty()) {
                    SearchResultsOverlay(
                        viewModel = viewModel,
                        results = searchResults,
                        onResultClick = { viewModel.navigateToSearchResult(it) }
                    )
                }

                viewModel.fullScreenImageUri?.let { uri ->
                    FullScreenImageViewer(
                        imagePath = uri,
                        repository = viewModel.repository,
                        onDismiss = { viewModel.dismissFullScreenImage() }
                    )
                }

                if (viewModel.previewData != null) {
                    CardPreviewOverlay(
                        viewModel = viewModel,
                        onDismiss = { viewModel.dismissPreview() }
                    )
                }

                if (viewModel.isZipProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassLoading(accentColor = accentColor, blurIntensity = blurIntensity)
                    }
                }
            }
        }
    }
}
