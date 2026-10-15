package com.app.zpower.ui.components

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About ZPower") },
        text = {
            Column {
                Text("Version: 1.0 (Industrial Build)")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Developer: @zedxrp")
                Spacer(modifier = Modifier.height(16.dp))
                Text("System Information:", style = MaterialTheme.typography.titleSmall)
                Text("OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                Text("Brand: ${Build.BRAND}")
                Text("Hardware: ${Build.HARDWARE}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
