package com.japanesewalking.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    accentTint: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == BackgroundDark || isSystemInDarkTheme()

    val baseColor = when {
        accentTint != null -> accentTint.copy(alpha = if (isDark) 0.24f else 0.16f)
        isDark -> Color(0x331E293B) // Frosted deep slate on dark
        else -> Color(0xCCFFFFFF) // 80% translucent white on light
    }

    val defaultContentColor = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onSurface

    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.10f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.40f)
            )
        }
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderBrush, shape),
        shape = shape,
        color = baseColor,
        contentColor = defaultContentColor,
        shadowElevation = if (isDark) 0.dp else 4.dp
    ) {
        content()
    }
}

/**
 * Ambient background brush that gives frosted glass cards realistic optical depth
 */
@Composable
fun getAmbientAppBackgroundBrush(isDark: Boolean): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F1A15), // Deep emerald midnight
                Color(0xFF12161A), // Dark slate
                Color(0xFF141215)  // Warm ambient shadow
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8F5E9), // Gentle matcha morning glow
                Color(0xFFF1F5F2), // Mist white
                Color(0xFFF8F9FA)  // Crisp clean bottom
            )
        )
    }
}
