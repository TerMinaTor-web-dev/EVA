package com.example.eva.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object EvaColors {
    val BackgroundTop = Color(0xFF090A10)
    val BackgroundBottom = Color(0xFF161028)
    
    val AmbientGlow1 = Color(0x3300E5FF)
    val AmbientGlow2 = Color(0x339D00FF)
    
    val GlassSurface = Color(0x18FFFFFF)
    val GlassSurfaceHover = Color(0x28FFFFFF)
    val GlassBorder = Color(0x38FFFFFF)
    val GlassBorderHighlight = Color(0x80FFFFFF)
    
    val TextPrimary = Color(0xFFF0F3F8)
    val TextSecondary = Color(0xFFA0A8B8)
    val TextMuted = Color(0xFF6B7280)
    
    val NeonCyan = Color(0xFF00E5FF)
    val NeonPurple = Color(0xFFA855F7)
    val NeonPink = Color(0xFFEC4899)
    
    val DualToneGradient = Brush.verticalGradient(
        colors = listOf(BackgroundTop, BackgroundBottom)
    )
    
    val CrystalHighlightGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x4DFFFFFF),
            Color(0x1AFFFFFF),
            Color(0x05FFFFFF)
        )
    )
}
