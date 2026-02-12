package com.sugarmaster.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

// On Wear OS (Samsung Galaxy Watch 4+), the system sans-serif IS Google Sans / Product Sans.
val GoogleSansFamily = FontFamily.SansSerif

// Glucose status colors
val GlucoseWhite = Color.White           // In range / good
val GlucoseBlue = Color(0xFF92D4F0)      // Too low
val GlucoseRed = Color(0xFFEA7B7A)       // Too high
val GlucoseYellow = Color(0xFFFFEB3B)    // Graph threshold lines

val WearColors = Colors(
    primary = Color.White,
    primaryVariant = Color(0xFFB0B0B0),
    secondary = Color.White,
    secondaryVariant = Color(0xFF888888),
    error = GlucoseRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.White,
    surface = Color(0xFF111111),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF888888),
    background = Color.Black,
    onBackground = Color.White
)

@Composable
fun SugarmasterTheme(content: @Composable () -> Unit) {
    val typography = Typography(
        defaultFontFamily = GoogleSansFamily
    )

    MaterialTheme(
        colors = WearColors,
        typography = typography,
        content = content
    )
}
