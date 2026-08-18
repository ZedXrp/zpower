package com.app.zpower.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.zpower.ui.navigation.NavigationViewModel

@Composable
fun CardPreviewOverlay(
    viewModel: NavigationViewModel,
    onDismiss: () -> Unit
) {
    val previewData = viewModel.previewData ?: return
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    val context = LocalContext.current

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() }
    ) {
        // Full screen blur effect on the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) { } // Prevent dismiss on image click
            ) {
                if (!previewData.imagePath.isNullOrEmpty()) {
                    val file = viewModel.repository.getLocalImageFile(context, previewData.imagePath)
                    AsyncImage(
                        model = file ?: previewData.imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "NO IMAGE AVAILABLE",
                            color = accentColor.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .clickable(enabled = false) { }
            ) {
                Text(
                    text = previewData.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = previewData.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Prominent Floating Close Button (Top Corner)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .border(2.dp, accentColor, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Preview",
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
