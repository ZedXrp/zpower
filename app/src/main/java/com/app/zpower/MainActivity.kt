package com.app.zpower

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.zpower.ui.MainScreen
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.ZPowerTheme

class MainActivity : ComponentActivity() {
    private lateinit var navigationViewModel: NavigationViewModel
    private val showPermissionDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            navigationViewModel = viewModel()
            val accentColorInt by navigationViewModel.accentColor.collectAsState()
            
            if (showPermissionDialog.value) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog.value = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(vertical = 24.dp),
                    title = { Text("Permission Required") },
                    text = { Text("ZPower needs \"All Files Access\" to manage the \"Gold Knowledge\" folder in your Documents. This allows the app to sync your data.json and images automatically.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showPermissionDialog.value = false
                            requestAllFilesAccess()
                        }) {
                            Text("Grant Permission")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog.value = false }) {
                            Text("Later")
                        }
                    }
                )
            }

            ZPowerTheme(accentColor = Color(accentColorInt)) {
                MainScreen(viewModel = navigationViewModel)
            }
        }
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog.value = true
            } else {
                showPermissionDialog.value = false
                if (::navigationViewModel.isInitialized) {
                    navigationViewModel.notifyPermissionChanged()
                    navigationViewModel.syncExternalChanges()
                }
            }
        } else {
            if (::navigationViewModel.isInitialized) {
                navigationViewModel.syncExternalChanges()
            }
        }
    }
}
