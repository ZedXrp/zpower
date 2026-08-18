package com.app.zpower.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.zpower.ui.navigation.ZPowerRoute
import com.app.zpower.ui.theme.BackgroundStyle
import com.app.zpower.ui.theme.GlassCyan
import com.app.zpower.ui.theme.LocalBackgroundStyle
import com.app.zpower.ui.theme.LocalGlassTextColor

@Composable
fun Breadcrumbs(
    backStack: List<ZPowerRoute>,
    rootTitle: String,
    onLevelClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Auto-scroll to end when backstack changes
    LaunchedEffect(backStack.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        backStack.forEachIndexed { index, route ->
            val isLast = index == backStack.lastIndex
            val backgroundStyle = LocalBackgroundStyle.current
            val glassTextColor = LocalGlassTextColor.current
            val isLiquidGlass = backgroundStyle == BackgroundStyle.LIQUID_GLASS
            
            BreadcrumbItem(
                label = route.getBreadcrumbLabel(rootTitle).uppercase(),
                isLast = isLast,
                isLiquidGlass = isLiquidGlass,
                glassTextColor = glassTextColor,
                onClick = { onLevelClick(index) }
            )
            
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    tint = if (isLiquidGlass) glassTextColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbItem(
    label: String,
    isLast: Boolean,
    isLiquidGlass: Boolean,
    glassTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        enabled = !isLast,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isLast) {
                if (isLiquidGlass) glassTextColor else GlassCyan
            } else {
                if (isLiquidGlass) glassTextColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
            }
        )
    }
}

private fun ZPowerRoute.getBreadcrumbLabel(rootTitle: String): String {
    return when (this) {
        is ZPowerRoute.ThermalAreaList -> rootTitle
        is ZPowerRoute.RoomList -> thermalAreaName
        is ZPowerRoute.PanelList -> roomName
        is ZPowerRoute.RelayList -> panelName
        is ZPowerRoute.ChildProcessList -> relayName
        is ZPowerRoute.ChildProcessDetail -> childProcessName
        is ZPowerRoute.SubProcessList -> parentName
        is ZPowerRoute.SubProcessDetail -> subProcessName
        is ZPowerRoute.Settings -> "Settings"
    }
}
