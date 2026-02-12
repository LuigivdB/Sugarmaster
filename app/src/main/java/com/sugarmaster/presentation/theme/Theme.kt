package com.sugarmaster.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val GlucoseGreen = Color(0xFF4CAF50)
val GlucoseYellow = Color(0xFFFFEB3B)
val GlucoseOrange = Color(0xFFFF9800)
val GlucoseRed = Color(0xFFF44336)

val WearColors = Colors(
    primary = Color(0xFF8ECAE6),
    primaryVariant = Color(0xFF219EBC),
    secondary = GlucoseGreen,
    secondaryVariant = Color(0xFF388E3C),
    error = GlucoseRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.White,
    surface = Color(0xFF1A1A2E),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
    background = Color.Black,
    onBackground = Color.White
)

@Composable
fun SugarmasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearColors,
        content = content
    )
}
