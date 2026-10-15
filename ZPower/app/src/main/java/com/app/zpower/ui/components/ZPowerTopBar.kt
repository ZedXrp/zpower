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
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.navigation.ZPowerRoute
import com.app.zpower.data.repository.BackupType
import com.app.zpower.ui.theme.GlassCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZPowerTopBar(
    viewModel: NavigationViewModel,
    modifier: Modifier = Modifier
) {
    val rootTitle by viewModel.rootTitle.collectAsState()
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    
    var showBackupDialog by remember { mutableStateOf(false) }
    var selectedBackupType by remember { mutableStateOf(BackupType.DATA_ONLY) }

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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupDialog = false
                        exportLauncher.launch("ZPower_Export_${System.currentTimeMillis()}.zip")
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
}
