package com.example.eva.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object EvaColors {
    // Obsidian Deep Space Background
    val BackgroundTop = Color(0xFF060810)
    val BackgroundBottom = Color(0xFF0F1426)
    val BackgroundSurface = Color(0xFF0A0D18)
    
    // Ambient Glows
    val AmbientGlow1 = Color(0x3300F0FF)
    val AmbientGlow2 = Color(0x338B5CF6)
    val AmbientGlow3 = Color(0x2210B981)
    
    // Modern Glassmorphic Surfaces
    val GlassSurface = Color(0x14FFFFFF)
    val GlassSurfaceHover = Color(0x24FFFFFF)
    val GlassSurfaceSubtle = Color(0x0AFFFFFF)
    val GlassBorder = Color(0x26FFFFFF)
    val GlassBorderHighlight = Color(0x6600F0FF)
    
    // Bento Card Surfaces
    val BentoCardBg = Color(0xFF0E1322)
    val BentoCardBgElevated = Color(0xFF141A2E)
    val BentoCardBorder = Color(0x1EFFFFFF)
    
    // Text Hierarchy
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextMuted = Color(0xFF71717A)
    
    // Modern Aurora Accents
    val NeonCyan = Color(0xFF00F0FF)
    val NeonPurple = Color(0xFF8B5CF6)
    val NeonPink = Color(0xFFF43F5E)
    val AuroraGreen = Color(0xFF10B981)
    val AmberGold = Color(0xFFF59E0B)
    val ElectricBlue = Color(0xFF3B82F6)
    
    // Gradients
    val DualToneGradient = Brush.verticalGradient(
        colors = listOf(BackgroundTop, BackgroundBottom)
    )
    
    val AuroraHeroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x3300F0FF),
            Color(0x338B5CF6),
            Color(0x1110B981)
        )
    )
    
    val CrystalHighlightGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x4DFFFFFF),
            Color(0x1AFFFFFF),
            Color(0x05FFFFFF)
        )
    )
    
    val CyberPillGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00F0FF),
            Color(0xFF8B5CF6)
        )
    )
}
