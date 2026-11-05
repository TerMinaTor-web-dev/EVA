package com.example.eva.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EvaDarkColorScheme = darkColorScheme(
    primary = EvaColors.NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = Color(0xFF70F5FF),
    secondary = EvaColors.NeonPurple,
    onSecondary = Color.White,
    background = EvaColors.BackgroundTop,
    onBackground = EvaColors.TextPrimary,
    surface = EvaColors.BackgroundBottom,
    onSurface = EvaColors.TextPrimary
)

@Composable
fun EVATheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EvaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
