package com.app.zpower.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.navigation.ZPowerRoute
import com.app.zpower.data.repository.BackupType
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.components.LiquidGlassLoading
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZPowerTopBar(
    viewModel: NavigationViewModel,
    modifier: Modifier = Modifier
) {
    val rootTitle by viewModel.rootTitle.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    
    val isTelegramEnabled by viewModel.isTelegramEnabled.collectAsState()
    val hasSeenJoinPrompt by viewModel.hasSeenJoinPrompt.collectAsState()
    
    var showBackupDialog by remember { mutableStateOf(false) }
    var showJoinPromptDialog by remember { mutableStateOf(false) }
    var selectedBackupType by remember { mutableStateOf(BackupType.DATA_ONLY) }
    
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportDataToZip(it, selectedBackupType) {
                // Success callback
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importDataFromZip(it) {
                // Success callback
            }
        }
    }

    TopAppBar(
        title = {
            if (viewModel.isSearching) {
                TextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    placeholder = { Text("Search Industrial Database...", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { }),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateIsSearching(false) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Search", tint = Color.White)
                        }
                    }
                )
            } else {
                Text(
                    text = if (viewModel.isEditMode) "$rootTitle // EDIT" else rootTitle,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = accentColor,
            actionIconContentColor = accentColor
        ),
        actions = {
            if (!viewModel.isSearching) {
                IconButton(onClick = { viewModel.updateIsSearching(true) }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }

                var showMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (viewModel.isEditMode) "Exit Edit Mode" else "Enter Edit Mode") },
                        onClick = {
                            viewModel.updateEditMode(!viewModel.isEditMode)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            viewModel.push(ZPowerRoute.Settings)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            viewModel.showAbout = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh Data") },
                        onClick = {
                            viewModel.forceRefreshData()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export ZIP") },
                        onClick = {
                            showBackupDialog = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Output, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Import ZIP") },
                        onClick = {
                            importLauncher.launch(arrayOf("application/zip"))
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Input, contentDescription = null) }
                    )
                }
            }
        },
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.9f),
                    Color.Transparent
                )
            )
        )
    )

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Export Backup") },
            text = {
                Column {
                    Text("Choose backup type:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedBackupType = BackupType.COMPLETE }
                    ) {
                        RadioButton(selected = selectedBackupType == BackupType.COMPLETE, onClick = { selectedBackupType = BackupType.COMPLETE })
                        Text("Complete Backup (Data + Images + Settings)", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedBackupType = BackupType.DATA_ONLY }
                    ) {
                        RadioButton(selected = selectedBackupType == BackupType.DATA_ONLY, onClick = { selectedBackupType = BackupType.DATA_ONLY })
                        Text("Data & Images Only", modifier = Modifier.padding(start = 8.dp))
                    }

                    if (isTelegramEnabled) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cloud Options:", style = MaterialTheme.typography.labelLarge, color = accentColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showBackupDialog = false
                                if (!hasSeenJoinPrompt) {
                                    showJoinPromptDialog = true
                                } else {
                                    viewModel.sendBackupToTelegram(selectedBackupType)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Backup Now to Telegram",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupDialog = false
                        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                        // Remove spaces for cleaner filename
                        val safeHeader = rootTitle.replace(" ", "")
                        exportLauncher.launch("ZPower_${safeHeader}_$date.zip")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Export", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel", color = accentColor)
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = accentColor,
            textContentColor = Color.White
        )
    }

    if (showJoinPromptDialog) {
        AlertDialog(
            onDismissRequest = { showJoinPromptDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            title = { Text("Join Telegram Community", color = Color(0xFF0088CC)) },
            text = {
                Column {
                    Text(
                        "Before sending backups, consider joining our community for updates and support.",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://t.me/zpowerdata") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC).copy(alpha = 0.2f), contentColor = Color(0xFF0088CC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Public Group")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://t.me/zpowerdata") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC).copy(alpha = 0.2f), contentColor = Color(0xFF0088CC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Private Group")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showJoinPromptDialog = false
                        viewModel.updateHasSeenJoinPrompt(true)
                        viewModel.sendBackupToTelegram(selectedBackupType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Proceed to Upload", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinPromptDialog = false }) {
                    Text("Cancel", color = accentColor)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

}
