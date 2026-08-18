package com.app.zpower.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

val LocalBackgroundStyle = staticCompositionLocalOf { BackgroundStyle.LIQUID_GRADIENT }
val LocalGlassTextColor = staticCompositionLocalOf { Color(0xFF5D4037) }

private val DarkColorScheme = darkColorScheme(
    primary = GlassCyan,
    secondary = GlassBlue,
    tertiary = GlassPurple,
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = LightBrown,
    onSurface = LightBrown,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = LightBrown
)

private val LightColorScheme = darkColorScheme( 
    primary = GlassBlue,
    secondary = GlassCyan,
    tertiary = GlassEmerald,
    background = Color(0xFFF0F0F0),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun ZPowerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (accentColor != null) {
        baseScheme.copy(primary = accentColor)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

