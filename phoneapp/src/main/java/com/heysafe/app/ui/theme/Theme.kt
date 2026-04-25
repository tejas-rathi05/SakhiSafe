package com.heysafe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HeyColors = lightColorScheme(
    primary = Primary,
    onPrimary = SurfaceWhite,
    secondary = Accent,
    background = SurfaceWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    error = ErrorRed,
)

@Composable
fun HeySafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HeyColors,
        typography = HeyTypography,
        shapes = HeyShapes,
        content = content,
    )
}
