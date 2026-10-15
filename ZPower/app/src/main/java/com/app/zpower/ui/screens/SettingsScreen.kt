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
import com.app.zpower.ui.theme.GlassBlue
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.GlassPurple

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb

import androidx.compose.material.icons.rounded.Refresh

@Composable
fun SettingsScreen(viewModel: NavigationViewModel) {
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val rootTitleFlow by viewModel.rootTitle.collectAsState()
    var rootTitleText by remember { mutableStateOf(rootTitleFlow) }
    val blurIntensity by viewModel.blurIntensity.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()

    // Sync local state with external changes (like Reset or DataStore switch)
    LaunchedEffect(rootTitleFlow) {
        if (rootTitleText != rootTitleFlow) {
            rootTitleText = rootTitleFlow
        }
    }
    
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
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
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            GlassCard(accentColor = accentColor) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Customization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Root Level Title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = rootTitleText,
                    onValueChange = { 
                        rootTitleText = it
                        viewModel.updateRootTitle(it) 
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                    color = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurface
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
                val blurEnabled by viewModel.blurEnabled.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Background Blur", modifier = Modifier.weight(1f))
                    Switch(
                        checked = blurEnabled,
                        onCheckedChange = { viewModel.updateBlurEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                if (blurEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Blur Intensity: ${blurIntensity.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = blurIntensity,
                        onValueChange = { viewModel.updateBlurIntensity(it) },
                        valueRange = 0f..40f,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                val cardSize by viewModel.cardSize.collectAsState()
                Text(
                    text = "Card Size",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CardSizeOption("Small", cardSize == "Small", accentColor) { viewModel.updateCardSize("Small") }
                    CardSizeOption("Medium", cardSize == "Medium", accentColor) { viewModel.updateCardSize("Medium") }
                    CardSizeOption("Large", cardSize == "Large", accentColor) { viewModel.updateCardSize("Large") }
                }
            }
        }

        item {
            GlassCard(accentColor = accentColor) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Background Style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                BackgroundOption(
                    label = "Liquid Glass Gradient",
                    isSelected = backgroundStyle == "glass_gradient",
                    onClick = { viewModel.updateBackgroundStyle("glass_gradient") },
                    previewBrush = Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                BackgroundOption(
                    label = "Brushed Metal Dark",
                    isSelected = backgroundStyle == "brushed_metal",
                    onClick = { viewModel.updateBackgroundStyle("brushed_metal") },
                    previewBrush = Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D))),
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                BackgroundOption(
                    label = "Digital Grid",
                    isSelected = backgroundStyle == "digital_grid",
                    onClick = { viewModel.updateBackgroundStyle("digital_grid") },
                    previewBrush = Brush.radialGradient(listOf(Color(0xFF1B2735), Color(0xFF090A0F))),
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                BackgroundOption(
                    label = "Industrial Border Glow",
                    isSelected = backgroundStyle == "industrial_border_glow",
                    onClick = { viewModel.updateBackgroundStyle("industrial_border_glow") },
                    previewBrush = Brush.radialGradient(
                        colors = listOf(Color.Black, accentColor.copy(alpha = 0.3f)),
                        center = Offset.Unspecified,
                        radius = Float.POSITIVE_INFINITY
                    ),
                    accentColor = accentColor
                )
            }
        }

        item {
            GlassCard(accentColor = GlassPurple) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = GlassPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Credits", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Developer: @zedxrp",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ZPower v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Default")
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
