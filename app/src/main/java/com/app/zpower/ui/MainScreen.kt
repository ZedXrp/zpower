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
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
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
import com.app.zpower.ui.theme.BackgroundStyle
import com.app.zpower.ui.theme.LocalBackgroundStyle
import com.app.zpower.ui.theme.LocalGlassTextColor

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
    val isTelegramUploading by viewModel.isTelegramUploading.collectAsState()
    val isTelegramFetching by viewModel.isTelegramFetching.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val searchPathColorInt by viewModel.searchPathColor.collectAsState()
    val glassTextColorInt by viewModel.glassTextColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val glassTextColor = Color(glassTextColorInt)
    val blurIntensity by viewModel.blurIntensity.collectAsState()
    val blurEnabled by viewModel.blurEnabled.collectAsState()
    val rootTitle by viewModel.rootTitle.collectAsState()

    val bgGradientColor1 by viewModel.bgGradientColor1.collectAsState()
    val bgGradientColor2 by viewModel.bgGradientColor2.collectAsState()
    val bgGradientColor3 by viewModel.bgGradientColor3.collectAsState()
    val bgImageUri by viewModel.bgImageUri.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val customBgFile = remember(bgImageUri) {
        java.io.File(viewModel.repository.getDataDir(context), "background.webp")
    }

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(bgGradientColor1),
            Color(bgGradientColor2),
            Color(bgGradientColor3)
        )
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                if (isTelegramUploading || isTelegramFetching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = accentColor,
                        trackColor = Color.Transparent
                    )
                }
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
        CompositionLocalProvider(
            LocalBackgroundStyle provides backgroundStyle,
            LocalGlassTextColor provides glassTextColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            // Background Layer
            val wallpaperDim by viewModel.wallpaperDim.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (blurEnabled && backgroundStyle != BackgroundStyle.LIQUID_GLASS) {
                            Modifier.blur(blurIntensity.dp)
                        } else Modifier
                    )
            ) {
                val showCustomImage = bgImageUri.isNotEmpty() && customBgFile.exists()
                
                if (showCustomImage) {
                    coil.compose.AsyncImage(
                        model = customBgFile,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val colors = when(backgroundStyle) {
                        BackgroundStyle.LIQUID_GRADIENT -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                        else -> listOf(Color(bgGradientColor1), Color(bgGradientColor2), Color(bgGradientColor3))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(colors))
                    )
                }
                
                // Overlay effects based on style
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            when(backgroundStyle) {
                                BackgroundStyle.DIGITAL_GRID -> {
                                    val gridSize = 40.dp.toPx()
                                    val color = Color.Cyan.copy(alpha = 0.05f)
                                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                                        drawLine(color, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height), strokeWidth = 1f)
                                    }
                                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                                        drawLine(color, start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()), strokeWidth = 1f)
                                    }
                                }
                                BackgroundStyle.BRUSHED_METAL -> {
                                    val spacing = 4.dp.toPx()
                                    val color = Color.White.copy(alpha = 0.02f)
                                    for (i in -size.height.toInt()..size.width.toInt() step spacing.toInt()) {
                                        drawLine(
                                            color = color,
                                            start = Offset(i.toFloat(), 0f),
                                            end = Offset(i.toFloat() + size.height, size.height),
                                            strokeWidth = 1f
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                )
            }

            // Wallpaper Dimming for Liquid Glass
            if (backgroundStyle == BackgroundStyle.LIQUID_GLASS) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = wallpaperDim))
                )
            }

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
}
