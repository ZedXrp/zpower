package com.app.zpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Email
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.app.zpower.R
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.zpower.ui.components.GlassCard
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.BackgroundStyle
import com.app.zpower.ui.theme.GlassBlue
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.GlassPurple
import com.app.zpower.ui.theme.LightBrown
import com.app.zpower.ui.theme.LocalBackgroundStyle
import com.app.zpower.ui.theme.LocalGlassTextColor
import com.app.zpower.ui.components.GlassTextField

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.DialogProperties

import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Link
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.onFocusChanged
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.app.zpower.data.repository.TelegramBackupItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.*

@Composable
fun SettingsScreen(viewModel: NavigationViewModel) {
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    
    val rootTitleFlow by viewModel.rootTitle.collectAsState()
    var rootTitleText by remember { mutableStateOf(rootTitleFlow) }
    var isRootTitleFocused by remember { mutableStateOf(false) }
    
    val customBotTokenFlow by viewModel.customBotToken.collectAsState()
    var customBotTokenText by remember { mutableStateOf(customBotTokenFlow) }
    var isBotTokenFocused by remember { mutableStateOf(false) }
    
    val telegramChatIdFlow by viewModel.telegramChatId.collectAsState()
    var telegramChatIdText by remember { mutableStateOf(telegramChatIdFlow) }
    var isChatIdFocused by remember { mutableStateOf(false) }
    
    val telegramGroupIdFlow by viewModel.telegramGroupId.collectAsState()
    var telegramGroupIdText by remember { mutableStateOf(telegramGroupIdFlow) }
    var isGroupIdFocused by remember { mutableStateOf(false) }

    val recipientUserIdsFlow by viewModel.recipientUserIds.collectAsState()
    var recipientUserIdsText by remember { mutableStateOf(recipientUserIdsFlow) }
    var isRecipientIdsFocused by remember { mutableStateOf(false) }

    val blurIntensity by viewModel.blurIntensity.collectAsState()
    var localBlurIntensity by remember(blurIntensity) { mutableStateOf(blurIntensity) }

    val backgroundStyle by viewModel.backgroundStyle.collectAsState()
    val glassTextColorInt by viewModel.glassTextColor.collectAsState()
    val glassTextColor = Color(glassTextColorInt)

    val wallpaperDim by viewModel.wallpaperDim.collectAsState()
    var localWallpaperDim by remember(wallpaperDim) { mutableStateOf(wallpaperDim) }

    val bgGradientColor1 by viewModel.bgGradientColor1.collectAsState()
    val bgGradientColor2 by viewModel.bgGradientColor2.collectAsState()
    val bgGradientColor3 by viewModel.bgGradientColor3.collectAsState()
    val bgImageUri by viewModel.bgImageUri.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val dataDir = viewModel.repository.getDataDir(context)
                val destFile = java.io.File(dataDir, "background.webp")
                
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val bitmap = BitmapFactory.decodeStream(input)
                        if (bitmap != null) {
                            java.io.FileOutputStream(destFile).use { output ->
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, output)
                                } else {
                                    @Suppress("DEPRECATION")
                                    bitmap.compress(Bitmap.CompressFormat.WEBP, 80, output)
                                }
                            }
                            bitmap.recycle()
                        }
                    }
                }
                
                if (destFile.exists()) {
                    viewModel.updateBgImageUri(destFile.absolutePath)
                    viewModel.snackbarHostState.showSnackbar("Custom background applied")
                }
            }
        }
    }
    
    val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
    val contrastColor = if (isLiquidGlass) glassTextColor else accentColor

    // Sync local state with external changes (like Reset or DataStore switch) only if not focused
    LaunchedEffect(rootTitleFlow) { if (!isRootTitleFocused && rootTitleText != rootTitleFlow) rootTitleText = rootTitleFlow }
    LaunchedEffect(customBotTokenFlow) { if (!isBotTokenFocused && customBotTokenText != customBotTokenFlow) customBotTokenText = customBotTokenFlow }
    LaunchedEffect(telegramChatIdFlow) { if (!isChatIdFocused && telegramChatIdText != telegramChatIdFlow) telegramChatIdText = telegramChatIdFlow }
    LaunchedEffect(telegramGroupIdFlow) { if (!isGroupIdFocused && telegramGroupIdText != telegramGroupIdFlow) telegramGroupIdText = telegramGroupIdFlow }
    LaunchedEffect(recipientUserIdsFlow) { if (!isRecipientIdsFocused && recipientUserIdsText != recipientUserIdsFlow) recipientUserIdsText = recipientUserIdsFlow }
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showWipeDialogStep1 by remember { mutableStateOf(false) }
    var showWipeDialogStep2 by remember { mutableStateOf(false) }
    var showBackupListDialog by remember { mutableStateOf(false) }
    var selectedBackup by remember { mutableStateOf<TelegramBackupItem?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var includeImagesInRestore by remember { mutableStateOf(true) }
    
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var isBotTokenLocked by remember { mutableStateOf(true) }

    val telegramBackups by viewModel.telegramBackups.collectAsState()
    val isFetchingBackups by viewModel.isTelegramFetching.collectAsState()
    val showTelegramBackupDialogFlow by viewModel.showTelegramBackupDialog.collectAsState()
    val adminPasswordStored by viewModel.adminPassword.collectAsState()
    
    val uriHandler = LocalUriHandler.current

    var showCustomConfigConfirmDialog by remember { mutableStateOf(false) }
    var customConfigTimer by remember { mutableStateOf(4) }
    var isCustomConfigContinueEnabled by remember { mutableStateOf(false) }

    var wipeSafetyTimer by remember { mutableStateOf(5) }
    var isWipeSafetyContinueEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(showWipeDialogStep2) {
        if (showWipeDialogStep2) {
            wipeSafetyTimer = 5
            isWipeSafetyContinueEnabled = false
            for (i in 5 downTo 1) {
                wipeSafetyTimer = i
                delay(1000)
            }
            wipeSafetyTimer = 0
            isWipeSafetyContinueEnabled = true
        }
    }

    LaunchedEffect(showCustomConfigConfirmDialog) {
        if (showCustomConfigConfirmDialog) {
            customConfigTimer = 4
            isCustomConfigContinueEnabled = false
            for (i in 4 downTo 1) {
                customConfigTimer = i
                delay(1000)
            }
            isCustomConfigContinueEnabled = true
        }
    }

    LaunchedEffect(showTelegramBackupDialogFlow) {
        if (showTelegramBackupDialogFlow) {
            showBackupListDialog = true
            viewModel.updateShowTelegramBackupDialog(false)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings", color = contrastColor) },
            text = { Text("Are you sure you want to reset all settings to default? This will revert theme, titles, and card sizes.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSettings()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWipeDialogStep1) {
        AlertDialog(
            onDismissRequest = { showWipeDialogStep1 = false },
            title = { Text("Delete All Data?", color = contrastColor) },
            text = { Text("Are you sure you want to delete ALL data? This includes images, backups, and settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialogStep1 = false
                        showWipeDialogStep2 = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialogStep1 = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWipeDialogStep2) {
        AlertDialog(
            onDismissRequest = { showWipeDialogStep2 = false },
            title = { Text("PERMANENT ACTION", color = Color.Red) },
            text = { 
                Column {
                    Text("WARNING: This action is permanent. All files in the 'Gold Knowledge' folder will be wiped. Continue?")
                    if (!isWipeSafetyContinueEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please wait ${wipeSafetyTimer}s...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllData()
                        showWipeDialogStep2 = false
                    },
                    enabled = isWipeSafetyContinueEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isWipeSafetyContinueEnabled) "WIPE EVERYTHING" else "WIPE EVERYTHING (${wipeSafetyTimer}s)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialogStep2 = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomConfigConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCustomConfigConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Enable Custom Configuration", color = contrastColor) },
            text = {
                Column {
                    Text("Warning: Enabling custom settings allows you to modify Bot Tokens and Group IDs. Ensure you have the correct credentials.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (isCustomConfigContinueEnabled) "You can now proceed." else "Please wait ${customConfigTimer}s...",
                        style = MaterialTheme.typography.labelMedium,
                        color = contrastColor
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomConfigActive(true)
                        showCustomConfigConfirmDialog = false
                    },
                    enabled = isCustomConfigContinueEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = contrastColor)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomConfigConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackupListDialog) {
        AlertDialog(
            onDismissRequest = { showBackupListDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { 
                Text(
                    "Select Backup to Restore",
                    style = MaterialTheme.typography.titleLarge,
                    color = contrastColor
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    if (isFetchingBackups) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = contrastColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Fetching backups...")
                        }
                    } else if (telegramBackups.isEmpty()) {
                        Text(
                            "No backups found on Telegram. Make sure your Bot Token and Chat ID are correct.",
                            modifier = Modifier.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(telegramBackups.size) { index ->
                                val backup = telegramBackups[index]
                                val dateStr = remember(backup.date) {
                                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(backup.date))
                                }
                                val sizeStr = remember(backup.size) {
                                    if (backup.size > 1024 * 1024) "${String.format(Locale.getDefault(), "%.1f", backup.size / (1024f * 1024f))} MB"
                                    else "${backup.size / 1024} KB"
                                }
                                
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedBackup = backup
                                            showRestoreConfirmDialog = true
                                            showBackupListDialog = false
                                        },
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, contrastColor.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = backup.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = sizeStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = contrastColor.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackupListDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRestoreConfirmDialog && selectedBackup != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Confirm Restore") },
            text = {
                Column {
                    Text("Restore ${selectedBackup?.name}?")
                    Text(
                        "This will replace your current app data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { includeImagesInRestore = !includeImagesInRestore }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = includeImagesInRestore,
                            onCheckedChange = { includeImagesInRestore = it },
                            colors = CheckboxDefaults.colors(checkedColor = contrastColor)
                        )
                        Text(
                            "Include Images?",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importSpecificTelegramBackup(selectedBackup!!.fileId, includeImagesInRestore)
                        showRestoreConfirmDialog = false
                        selectedBackup = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = contrastColor)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRestoreConfirmDialog = false
                    showBackupListDialog = true // Go back to list
                }) {
                    Text("Back")
                }
            }
        )
    }

    if (viewModel.showImportApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.resolveImportApiKeyChoice(keepCurrent = true) },
            title = { Text("Import API Key?", color = contrastColor) },
            text = { Text("The backup contains a new Telegram Bot Token. Do you want to replace your current key or keep it?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.resolveImportApiKeyChoice(keepCurrent = false) },
                    colors = ButtonDefaults.buttonColors(containerColor = contrastColor)
                ) {
                    Text("Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resolveImportApiKeyChoice(keepCurrent = true) }) {
                    Text("Keep Current")
                }
            }
        )
    }
    
    if (viewModel.showConnectionError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConnectionError() },
            title = { Text("Connection Failed", color = Color.Red) },
            text = { Text(viewModel.connectionErrorMessage) },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.revertToDefaultBot()
                        viewModel.dismissConnectionError()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = contrastColor)
                ) {
                    Text("Restore to Default Telegram Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConnectionError() }) {
                    Text("Try Again")
                }
            }
        )
    }
    
    if (showAdminPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAdminPasswordDialog = false
                adminPasswordInput = ""
            },
            title = { Text("Admin Verification", color = contrastColor) },
            text = {
                Column {
                    Text("Enter Admin Password to unlock Bot Token settings.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = contrastColor,
                            cursorColor = contrastColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPasswordInput == adminPasswordStored) {
                            isBotTokenLocked = false
                            showAdminPasswordDialog = false
                            adminPasswordInput = ""
                        } else {
                            // Could show error
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = contrastColor)
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAdminPasswordDialog = false
                    adminPasswordInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            GlassCard(accentColor = accentColor, backgroundStyle = backgroundStyle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Customization", style = MaterialTheme.typography.titleMedium, color = accentColor)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Root Level Title",
                    style = MaterialTheme.typography.titleMedium,
                    color = contrastColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = rootTitleText,
                    onValueChange = { 
                        rootTitleText = it
                        viewModel.updateRootTitle(it) 
                    },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { isRootTitleFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = accentColor,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.titleMedium,
                    color = contrastColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                val colors = listOf(
                    Color(0xFF00E5FF), // Cyber Cyan
                    Color(0xFFD500F9), // Neon Purple
                    Color(0xFFFF6D00), // Industrial Orange
                    Color(0xFF00E676), // Acid Green
                    Color(0xFF2979FF), // Electric Blue
                    Color(0xFFFF1744), // Crimson Red
                    Color(0xFFFFC400), // Amber Gold
                    Color(0xFFF50057), // Hot Pink
                    Color(0xFF3D5AFE), // Cobalt Blue
                    Color(0xFF1DE9B6)  // Deep Teal
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.take(5).forEach { color ->
                            ColorOption(color, accentColorInt) { viewModel.updateAccentColor(it) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.drop(5).forEach { color ->
                            ColorOption(color, accentColorInt) { viewModel.updateAccentColor(it) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Search Path Color",
                    style = MaterialTheme.typography.titleMedium,
                    color = contrastColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                val searchPathColorInt by viewModel.searchPathColor.collectAsState()
                val pathColors = listOf(
                    Color(0xFF9E9E9E), // Gray
                    Color(0xFF00E5FF), // Cyber Cyan
                    Color(0xFFD500F9), // Neon Purple
                    Color(0xFFFF6D00), // Industrial Orange
                    Color(0xFF00E676), // Acid Green
                    Color(0xFF2979FF), // Electric Blue
                    Color(0xFFFF1744), // Crimson Red
                    Color(0xFFFFC400), // Amber Gold
                    Color(0xFFF50057), // Hot Pink
                    Color(0xFF3D5AFE)  // Cobalt Blue
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        pathColors.take(5).forEach { color ->
                            ColorOption(color, searchPathColorInt) { viewModel.updateSearchPathColor(it) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        pathColors.drop(5).forEach { color ->
                            ColorOption(color, searchPathColorInt) { viewModel.updateSearchPathColor(it) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Background Style",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BackgroundStyle.entries.forEach { style ->
                        val isSelected = backgroundStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateBackgroundStyle(style) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateBackgroundStyle(style) },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                val blurEnabled by viewModel.blurEnabled.collectAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isLiquidGlass) "Enable Glass Refraction" else "Enable Background Blur",
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = blurEnabled,
                        onCheckedChange = { viewModel.updateBlurEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                if (blurEnabled || isLiquidGlass) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLiquidGlass) "Refraction Intensity: ${localBlurIntensity.toInt()}" else "Blur Intensity: ${localBlurIntensity.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor
                    )
                    Slider(
                        value = localBlurIntensity,
                        onValueChange = { localBlurIntensity = it },
                        onValueChangeFinished = { viewModel.updateBlurIntensity(localBlurIntensity) },
                        valueRange = if (isLiquidGlass) 0f..100f else 0f..40f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )

                    if (isLiquidGlass) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Wallpaper Dim: ${(localWallpaperDim * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor
                        )
                        Slider(
                            value = localWallpaperDim,
                            onValueChange = { localWallpaperDim = it },
                            onValueChangeFinished = { viewModel.updateWallpaperDim(localWallpaperDim) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                val cardSize by viewModel.cardSize.collectAsState()
                Text(
                    text = "Card Size",
                    style = MaterialTheme.typography.titleMedium,
                    color = contrastColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CardSizeOption("Small", cardSize == "Small", accentColor) { viewModel.updateCardSize("Small") }
                    CardSizeOption("Medium", cardSize == "Medium", accentColor) { viewModel.updateCardSize("Medium") }
                    CardSizeOption("Large", cardSize == "Large", accentColor) { viewModel.updateCardSize("Large") }
                }

                if (isLiquidGlass) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Glass Text Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = contrastColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val glassTextColors = listOf(
                        Color(0xFF5D4037), // Original Brown
                        Color(0xFF3E2723), // Darker Brown
                        Color(0xFF000000), // Pure Black
                        Color(0xFF1A237E), // Deep Indigo
                        Color(0xFF1B5E20), // Dark Green
                        Color(0xFFE3F2FD), // Light Blue
                        Color(0xFFFFFFFF), // White
                        Color(0xFFFFEB3B), // Yellow
                        accentColor,       // Current Accent
                        Color.Gray         // Gray
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            glassTextColors.take(5).forEach { color ->
                                ColorOption(color, glassTextColorInt) { viewModel.updateGlassTextColor(it) }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            glassTextColors.drop(5).forEach { color ->
                                ColorOption(color, glassTextColorInt) { viewModel.updateGlassTextColor(it) }
                            }
                        }
                    }
                }
            }
        }

        if (isLiquidGlass) {
            item {
                GlassCard(accentColor = accentColor, backgroundStyle = backgroundStyle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Unified Background Engine", style = MaterialTheme.typography.titleMedium, color = contrastColor)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Multi-Point Gradient",
                        style = MaterialTheme.typography.titleMedium,
                        color = contrastColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val gradientColors = listOf(
                        Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364), // Midnight
                        Color(0xFF000000), Color(0xFF434343), Color(0xFF212121), // Onyx
                        Color(0xFF1a2a6c), Color(0xFFb21f1f), Color(0xFFfdbb2d), // Sunset Cyber
                        Color(0xFF232526), Color(0xFF414345), Color(0xFF000000), // Industrial
                        Color(0xFFC0C0C0), Color(0xFF808080), Color(0xFF404040), // Steel
                        Color(0xFF121212), Color(0xFF1F1F1F), Color(0xFF2C2C2C)  // Dark Grey
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Point 1", style = MaterialTheme.typography.labelSmall, color = contrastColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(bgGradientColor1))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { /* Picker logic if needed, but for now we show preset row */ }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Point 2", style = MaterialTheme.typography.labelSmall, color = contrastColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(bgGradientColor2))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Point 3", style = MaterialTheme.typography.labelSmall, color = contrastColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(bgGradientColor3))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Presets", style = MaterialTheme.typography.labelSmall, color = contrastColor.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        gradientColors.take(6).forEach { color ->
                            ColorOption(color, bgGradientColor1) { viewModel.updateBgGradientColor1(it) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        gradientColors.slice(6..11).forEach { color ->
                            ColorOption(color, bgGradientColor2) { viewModel.updateBgGradientColor2(it) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        gradientColors.drop(12).forEach { color ->
                            ColorOption(color, bgGradientColor3) { viewModel.updateBgGradientColor3(it) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = contrastColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Custom Background Image",
                        style = MaterialTheme.typography.titleMedium,
                        color = contrastColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, contrastColor.copy(alpha = 0.2f))
                    ) {
                        if (bgImageUri.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = bgImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = contrastColor.copy(alpha = 0.5f))
                                    Text("Choose Image (WebP Optimized)", color = contrastColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                    
                    if (bgImageUri.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.updateBgImageUri("") },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Remove Image", color = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        item {
            val isTelegramEnabled by viewModel.isTelegramEnabled.collectAsState()
            val isCustomConfigActive by viewModel.isCustomConfigActive.collectAsState()
            val shareBotApiInBackup by viewModel.shareBotApiInBackup.collectAsState()

            // 1. Telegram Sharing Section (Expandable)
            GlassCard(accentColor = contrastColor, backgroundStyle = backgroundStyle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Send, contentDescription = null, tint = contrastColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Telegram Sharing", style = MaterialTheme.typography.titleMedium, color = contrastColor)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enable Sharing",
                        style = MaterialTheme.typography.titleMedium,
                        color = contrastColor,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isTelegramEnabled,
                        onCheckedChange = { viewModel.updateTelegramEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = contrastColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { uriHandler.openUri("https://t.me/zedbackupbot") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = contrastColor.copy(alpha = 0.2f), contentColor = contrastColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Start Bot (@Zedbackupbot)")
                }

                androidx.compose.animation.AnimatedVisibility(visible = isTelegramEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassTextField(
                            value = telegramChatIdText,
                            onValueChange = { 
                                telegramChatIdText = it
                                viewModel.updateTelegramChatId(it) 
                            },
                            label = "Personal Chat ID (Numeric)",
                            modifier = Modifier.onFocusChanged { isChatIdFocused = it.isFocused },
                            accentColor = contrastColor
                        )
                        Text(
                            "Get your ID from @userinfobot or @missrose_bot (/id)",
                            style = MaterialTheme.typography.labelSmall,
                            color = contrastColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateShareBotApiInBackup(!shareBotApiInBackup) }
                                .padding(vertical = 8.dp)
                        ) {
                            Switch(
                                checked = shareBotApiInBackup,
                                onCheckedChange = { viewModel.updateShareBotApiInBackup(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = contrastColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Include API Key in Exports",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = contrastColor
                                )
                                Text(
                                    "Allows the Bot Token to travel with the .zip backup",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contrastColor.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Beta / Upcoming Section
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Beta / Upcoming Features", style = MaterialTheme.typography.titleSmall, color = contrastColor.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Import from Link (Coming Soon)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, contrastColor.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Import from Link", style = MaterialTheme.typography.bodyMedium, color = contrastColor.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Coming Soon", style = MaterialTheme.typography.labelSmall, color = accentColor.copy(alpha = 0.7f))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Multi-Recipient Forwarding
                        OutlinedTextField(
                            value = recipientUserIdsText,
                            onValueChange = { 
                                recipientUserIdsText = it
                                viewModel.updateRecipientUserIds(it)
                            },
                            label = { Text("Multi-Recipient IDs", color = contrastColor.copy(alpha = 0.7f)) },
                            placeholder = { Text("Comma separated IDs (e.g. 12345,67890)", color = contrastColor.copy(alpha = 0.3f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isRecipientIdsFocused = it.isFocused },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = contrastColor,
                                unfocusedBorderColor = contrastColor.copy(alpha = 0.3f),
                                focusedTextColor = contrastColor,
                                unfocusedTextColor = contrastColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        val isFetchingBackups by viewModel.isTelegramFetching.collectAsState()
                        Button(
                            onClick = { viewModel.loadTelegramBackups() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isFetchingBackups,
                            colors = ButtonDefaults.buttonColors(containerColor = contrastColor.copy(alpha = 0.8f), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFetchingBackups) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Recent Backups")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Custom Configuration Section (Expandable)
            GlassCard(accentColor = contrastColor, backgroundStyle = backgroundStyle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = contrastColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Custom Configuration", style = MaterialTheme.typography.titleMedium, color = contrastColor)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Custom Bot API",
                            style = MaterialTheme.typography.titleSmall,
                            color = contrastColor
                        )
                        Text(
                            text = "Use your own bot and community group",
                            style = MaterialTheme.typography.labelSmall,
                            color = contrastColor.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = isCustomConfigActive,
                        onCheckedChange = { 
                            if (it) showCustomConfigConfirmDialog = true
                            else viewModel.updateCustomConfigActive(false)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = contrastColor)
                    )
                }

                androidx.compose.animation.AnimatedVisibility(visible = isCustomConfigActive) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = customBotTokenText,
                            onValueChange = { 
                                customBotTokenText = it
                                viewModel.updateCustomBotToken(it) 
                            },
                            label = { Text("Custom Bot Token", color = contrastColor) },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { isBotTokenFocused = it.isFocused },
                            placeholder = { Text("123456:ABC-DEF...") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = contrastColor,
                                unfocusedBorderColor = contrastColor.copy(alpha = 0.3f),
                                focusedLabelColor = contrastColor
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassTextField(
                            value = telegramGroupIdText,
                            onValueChange = { 
                                telegramGroupIdText = it
                                viewModel.updateTelegramGroupId(it) 
                            },
                            label = "Community Group ID / @Username",
                            modifier = Modifier.onFocusChanged { isGroupIdFocused = it.isFocused },
                            accentColor = contrastColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.revertToDefaultBot() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = contrastColor.copy(alpha = 0.1f), contentColor = contrastColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Restore Default")
                            }
                            Button(
                                onClick = { viewModel.testTelegramConnection() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = contrastColor.copy(alpha = 0.1f), contentColor = contrastColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Test & Verify")
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(accentColor = contrastColor, backgroundStyle = backgroundStyle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = contrastColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Tutorials & Instructions", style = MaterialTheme.typography.titleMedium, color = contrastColor)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                TutorialSection(
                    title = "How Telegram Backup Works",
                    content = "ZPower synchronizes your industrial database directly to Telegram. Backups include all hierarchy data, images, and optional settings.",
                    accentColor = accentColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TutorialSection(
                    title = "Personal Backups",
                    content = "1. Press 'Start Bot' above to open @Zedbackupbot. 2. Press 'Start' in the bot chat. 3. Input your Personal ID (from @userinfobot) in settings. 4. Backups will now be sent directly to you.",
                    accentColor = accentColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TutorialSection(
                    title = "Troubleshooting",
                    content = "If your backup doesn't appear in Recent, please re-forward your ZIP file to the bot in Telegram. Important: You MUST press 'Start' on the bot to receive personal backups.",
                    accentColor = accentColor
                )
            }
        }

        item {
            GlassCard(accentColor = GlassPurple, backgroundStyle = backgroundStyle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = GlassPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Credits", style = MaterialTheme.typography.titleMedium, color = contrastColor)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.credit),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Developer: @zedxrp",
                            style = MaterialTheme.typography.bodyLarge,
                            color = contrastColor.copy(alpha = 0.9f)
                        )
                        Text(
                            "ZPower v1.0.0",
                            style = MaterialTheme.typography.labelMedium,
                            color = contrastColor.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { uriHandler.openUri("https://paypal.me/abdullahexplain") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contrastColor.copy(alpha = 0.1f),
                        contentColor = contrastColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, contrastColor.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Donate via PayPal", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { uriHandler.openUri("mailto:abdullahexpain@gmail.com") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Support", color = contrastColor.copy(alpha = 0.8f))
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Settings to Default")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showWipeDialogStep1 = true },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DELETE ALL DATA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewBrush: Brush,
    accentColor: Color = GlassBlue
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewBrush)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
        )
    }
}

@Composable
fun ColorOption(
    color: Color,
    selectedColorInt: Int,
    onSelect: (Color) -> Unit
) {
    val isSelected = color.toArgb() == selectedColorInt
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelect(color) }
    )
}

@Composable
fun TutorialSection(title: String, content: String, accentColor: Color) {
    val backgroundStyle = LocalBackgroundStyle.current
    val glassTextColor = LocalGlassTextColor.current
    val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
    val contrastColor = if (isLiquidGlass) glassTextColor else accentColor
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, contrastColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = contrastColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, style = MaterialTheme.typography.bodySmall, color = contrastColor.copy(alpha = 0.7f))
    }
}

@Composable
fun CardSizeOption(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(90.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
