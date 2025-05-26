package com.example.insulincalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColorPalette = lightColors(
    primary = androidx.compose.ui.graphics.Color(0xFF1976D2),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF1565C0),
    secondary = androidx.compose.ui.graphics.Color(0xFF26A69A)
)

private val DarkColorPalette = darkColors(
    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF1976D2),
    secondary = androidx.compose.ui.graphics.Color(0xFF80CBC4)
)

@Composable
fun InsulinCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
} 