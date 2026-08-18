package com.app.zpower.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import android.graphics.BlurMaskFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.app.zpower.data.repository.DatabaseRepository
import com.app.zpower.ui.theme.GlassBackground
import com.app.zpower.ui.theme.GlassBorder
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.LightBrown
import com.app.zpower.ui.theme.BackgroundStyle
import com.app.zpower.ui.theme.LocalBackgroundStyle
import com.app.zpower.ui.theme.LocalGlassTextColor

import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.app.zpower.R

import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

@Composable
fun LiquidGlassLoading(
    modifier: Modifier = Modifier,
    accentColor: Color = GlassCyan,
    blurIntensity: Float = 15f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassLoading")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .blur(blurIntensity.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * scale
            
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.1f),
                        accentColor,
                        accentColor.copy(alpha = 0.1f)
                    ),
                    center = center
                ),
                radius = radius,
                center = center
            )
        }
    }
}



@Composable
fun SquareGlassCard(
    name: String,
    description: String,
    modifier: Modifier = Modifier,
    accentColor: Color = GlassCyan,
    imagePath: String? = null,
    repository: DatabaseRepository? = null,
    cardSize: String = "Medium",
    isEditMode: Boolean = false,
    blurIntensity: Float = 15f,
    blurEnabled: Boolean = true,
    backgroundStyle: BackgroundStyle = LocalBackgroundStyle.current,
    onNavigate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val minHeight = when (cardSize) {
        "Small" -> 160.dp
        "Large" -> 360.dp
        else -> 260.dp
    }

    val contentPadding = when (cardSize) {
        "Small" -> 6.dp
        "Large" -> 16.dp
        else -> 12.dp
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val isBorderGlow = backgroundStyle == BackgroundStyle.LIQUID_GRADIENT
    val glassTextColor = LocalGlassTextColor.current

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            itemName = name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete?.invoke()
            }
        )
    }

    val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
    val glassRadius = if (isLiquidGlass) 28.dp else 16.dp
    
    val liquidGlassBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.02f)
        )
    )
    
    val cardBackgroundBrush = if (isLiquidGlass) liquidGlassBrush else Brush.verticalGradient(listOf(GlassBackground, GlassBackground))

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(glassRadius))
    ) {
        // Layer 1: Backdrop Blur (Android 12+)
        if (isLiquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect.createBlurEffect(
                            blurIntensity, blurIntensity, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }

        // Layer 2: Tint, Highlights and Rims
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(cardBackgroundBrush)
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val glowRadius = 12.dp.toPx()
                    val shapeRadius = glassRadius.toPx()
                    
                    if (isLiquidGlass) {
                        // 1. Multi-tonal Glass Rim (Specular Highlights)
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.4f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            size = size,
                            cornerRadius = CornerRadius(shapeRadius),
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // 2. Inner Glow with Accent Color
                        inset(2.dp.toPx()) {
                            drawRoundRect(
                                color = accentColor.copy(alpha = 0.15f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(shapeRadius - 2.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    } else {
                        // Legacy Industrial Glow
                        val paint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = accentColor.copy(alpha = 0.2f).toArgb()
                            style = android.graphics.Paint.Style.STROKE
                            this.strokeWidth = strokeWidth * 2
                            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                        }
                        
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRoundRect(
                                0f, 0f, size.width, size.height,
                                shapeRadius, shapeRadius,
                                paint
                            )
                        }

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            size = size,
                            cornerRadius = CornerRadius(shapeRadius),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                .drawWithContent {
                    drawContent()
                    
                    // 5. Specular Highlights (Top-Left Glare)
                    val highlightBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                    
                    drawRoundRect(
                        brush = highlightBrush,
                        size = size,
                        cornerRadius = CornerRadius(glassRadius.toPx())
                    )
                }
        )

        // Layer 3: Sharp Content
        // Blurred Background Image (Conditional)
        if (!imagePath.isNullOrEmpty() && repository != null) {
            val file = repository.getLocalImageFile(context, imagePath)
            AsyncImage(
                model = file ?: imagePath,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .then(if (blurEnabled) Modifier.blur(blurIntensity.dp) else Modifier)
                    .alpha(if (blurEnabled) 0.15f else 0.3f),
                contentScale = ContentScale.Crop
            )
        }

        Column {
            // Top Image (Square Aspect Ratio) - This remains SHARP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(onImageClick, onLongPress) {
                        detectTapGestures(
                            onTap = { onImageClick() },
                            onLongPress = { onLongPress?.invoke() }
                        )
                    }
            ) {
                if (!imagePath.isNullOrEmpty() && repository != null) {
                    val file = repository.getLocalImageFile(context, imagePath)
                    AsyncImage(
                        model = file ?: imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(if (cardSize == "Small") 24.dp else 48.dp)
                        )
                    }
                }

                // Buttons Overlay
                if (isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Edit Button (Top-Left)
                        IconButton(
                            onClick = { onEdit?.invoke() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete Button (Top-Right)
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(
                        minHeight = when (cardSize) {
                            "Small" -> 90.dp
                            "Large" -> 180.dp
                            else -> 140.dp
                        }
                    )
                    .pointerInput(onNavigate, onLongPress) {
                        detectTapGestures(
                            onTap = { onNavigate?.invoke() },
                            onLongPress = { onLongPress?.invoke() }
                        )
                    }
                    .padding(contentPadding)
                    .then(
                        if ((!imagePath.isNullOrEmpty() || (blurEnabled && blurIntensity > 20f)) && !isLiquidGlass) {
                            Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.4f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        } else Modifier
                    )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = when (cardSize) {
                                "Small" -> MaterialTheme.typography.labelLarge
                                "Large" -> MaterialTheme.typography.titleLarge
                                else -> MaterialTheme.typography.titleMedium
                            },
                            color = if (isLiquidGlass) glassTextColor else accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (description.isNotEmpty()) {
                            var isOverflowing by remember { mutableStateOf(false) }

                            Text(
                                text = description,
                                style = when (cardSize) {
                                    "Small" -> MaterialTheme.typography.labelSmall
                                    "Large" -> MaterialTheme.typography.bodyMedium
                                    else -> MaterialTheme.typography.bodySmall
                                },
                                color = if (isLiquidGlass) glassTextColor.copy(alpha = 0.8f) else accentColor.copy(alpha = 0.8f),
                                maxLines = if (expanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult ->
                                    if (!expanded) {
                                        isOverflowing = textLayoutResult.hasVisualOverflow
                                    }
                                }
                            )

                            if (isOverflowing && !expanded) {
                                Text(
                                    text = "Read More...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLiquidGlass) glassTextColor else accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable { expanded = true }
                                )
                            } else if (expanded) {
                                Text(
                                    text = "Read Less",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLiquidGlass) glassTextColor else accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable { expanded = false }
                                )
                            }
                        }
                    }

                    if (onNavigate != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Navigate",
                            tint = if (isLiquidGlass) glassTextColor else accentColor,
                            modifier = Modifier
                                .size(if (cardSize == "Small") 16.dp else 24.dp)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 24.dp),
        title = { Text("Delete Confirmation") },
        text = { Text("Are you sure you want to delete '$itemName'? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = GlassCyan,
    imagePath: String? = null,
    repository: DatabaseRepository? = null,
    blurIntensity: Float = 15f,
    blurEnabled: Boolean = true,
    cardSize: String = "Medium",
    backgroundStyle: BackgroundStyle = LocalBackgroundStyle.current,
    onImageClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val padding = when(cardSize) {
        "Small" -> 8.dp
        "Large" -> 24.dp
        else -> 16.dp
    }
    
    val minHeight = when(cardSize) {
        "Small" -> 60.dp
        "Large" -> 120.dp
        else -> 80.dp
    }

    val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
    val glassRadius = if (isLiquidGlass) 28.dp else 8.dp
    
    val liquidGlassBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.02f)
        )
    )
    
    val cardBackgroundBrush = if (isLiquidGlass) liquidGlassBrush else Brush.verticalGradient(listOf(GlassBackground, GlassBackground))

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(glassRadius))
    ) {
        // Layer 1: Backdrop Blur (Android 12+)
        if (isLiquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect.createBlurEffect(
                            blurIntensity, blurIntensity, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }

        // Layer 2: Tint, Highlights and Rims
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(cardBackgroundBrush)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val glowRadius = 8.dp.toPx()
                    val shapeRadius = glassRadius.toPx()
                    
                    if (isLiquidGlass) {
                        // Multi-tonal Rim
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.3f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            size = size,
                            cornerRadius = CornerRadius(shapeRadius),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Inner Glow
                        inset(1.5.dp.toPx()) {
                            drawRoundRect(
                                color = accentColor.copy(alpha = 0.12f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(shapeRadius - 1.5.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    } else {
                        // 1. External Accent Glow
                        val paint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = accentColor.copy(alpha = 0.2f).toArgb()
                            style = android.graphics.Paint.Style.STROKE
                            this.strokeWidth = strokeWidth * 2
                            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                        }
                        
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRoundRect(
                                0f, 0f, size.width, size.height,
                                shapeRadius, shapeRadius,
                                paint
                            )
                        }

                        // 2. Rim - Outer Light
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.25f),
                            size = size,
                            cornerRadius = CornerRadius(shapeRadius),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // 3. Rim - Inner Dark
                        inset(1.dp.toPx()) {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.1f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(shapeRadius - 1.dp.toPx()),
                                style = Stroke(width = 0.5.dp.toPx())
                            )
                        }

                        // 4. Internal Glow
                        inset(1.5.dp.toPx()) {
                            drawRoundRect(
                                color = accentColor.copy(alpha = 0.08f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(shapeRadius - 1.5.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    
                    // 5. Specular Highlights
                    val highlightBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.35f, size.height * 0.35f)
                    )
                    
                    drawRoundRect(
                        brush = highlightBrush,
                        size = size,
                        cornerRadius = CornerRadius(glassRadius.toPx())
                    )
                }
        )

        // Layer 3: Content
        if (!imagePath.isNullOrEmpty() && repository != null) {
            val file = repository.getLocalImageFile(context, imagePath)
            AsyncImage(
                model = file ?: imagePath,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .then(if (blurEnabled) Modifier.blur(blurIntensity.dp) else Modifier)
                    .alpha(if (blurEnabled) 0.2f else 0.4f),
                contentScale = ContentScale.Crop
            )
            
            if (onImageClick != null) {
                IconButton(
                    onClick = onImageClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = "View Image",
                        tint = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Industrial Markings
        Canvas(modifier = Modifier.matchParentSize()) {
            val markSize = 12.dp.toPx()
            val stroke = 1.5.dp.toPx()
            
            // Top-right indicator
            if (onImageClick == null) {
                drawLine(
                    color = accentColor.copy(alpha = 0.4f),
                    start = Offset(size.width - markSize, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.4f),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, markSize),
                    strokeWidth = stroke
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .then(
                    if ((!imagePath.isNullOrEmpty() || (blurEnabled && blurIntensity > 20f)) && !isLiquidGlass) {
                        Modifier
                            .background(
                                Color.Black.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    } else Modifier
                ),
            content = content
        )
    }
}

@Composable
fun DetailSection(title: String, content: String, accentColor: Color = GlassCyan, cardSize: String = "Medium") {
    val backgroundStyle = LocalBackgroundStyle.current
    val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
    val glassTextColor = LocalGlassTextColor.current
    
    if (content.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = when(cardSize) {
                    "Small" -> MaterialTheme.typography.titleSmall
                    "Large" -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                },
                color = if (isLiquidGlass) glassTextColor else accentColor
            )
            Text(
                text = content,
                style = when(cardSize) {
                    "Small" -> MaterialTheme.typography.bodyMedium
                    "Large" -> MaterialTheme.typography.headlineSmall
                    else -> MaterialTheme.typography.bodyLarge
                },
                color = if (isLiquidGlass) glassTextColor.copy(alpha = 0.9f) else accentColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ImageSourceSelector(
    onDismiss: () -> Unit,
    onSourceSelected: (Boolean) -> Unit // true for camera, false for gallery
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 24.dp),
        title = { Text("Select Image Source") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSourceSelected(true) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Camera")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSourceSelected(false) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Gallery")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Cyan,
    text: String
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = 0.1f),
            contentColor = accentColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = GlassCyan,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = GlassBorder.copy(alpha = 0.5f),
            focusedLabelColor = accentColor,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = accentColor,
            disabledBorderColor = accentColor.copy(alpha = 0.1f),
            disabledLabelColor = accentColor.copy(alpha = 0.5f),
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    )
}
