package com.example.eva.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.model.AppService
import com.example.eva.ui.theme.EvaColors

/**
 * Modern Bento-style interactive service tile for EVA 3.0.
 * Combines obsidian glassmorphism, responsive spring tactile physics,
 * and branded glowing halos.
 */
@Composable
fun BentoServiceCard(
    service: AppService,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "bentoTileScale"
    )

    val tagline = when (service.id) {
        "youtube" -> "4K Theater & Fullscreen"
        "ytmusic" -> "Background Music Stream"
        "instagram" -> "Reels & Stories Decrapified"
        "spotify" -> "Zero-Idle Audio Engine"
        "whatsapp" -> "Desktop Web Messenger"
        "reddit" -> "Clean Feed, No App Nags"
        "linkedin" -> "Professional Network"
        "facebook" -> "Social Feed & Messages"
        "twitter" -> "Real-time Global Pulse"
        "flipkart" -> "Deals, Mobiles & SuperCoins"
        "amazon" -> "Prime Fast Delivery & Pay"
        "myntra" -> "Runway Trends & Styles"
        "instamart" -> "10-Min Instant Grocery Drop"
        "blinkit" -> "Instant Doorstep Essentials"
        else -> "Hardware Accelerated"
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 6.dp else 14.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = service.primaryColor.copy(alpha = 0.4f),
                ambientColor = Color.Black
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        EvaColors.BentoCardBgElevated,
                        EvaColors.BentoCardBg
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isPressed) 0.6f else 0.25f),
                        service.primaryColor.copy(alpha = if (isPressed) 0.8f else 0.4f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Brand Icon Container + Category Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glowing Icon Squircle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = service.primaryColor)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    service.primaryColor.copy(alpha = 0.35f),
                                    service.secondaryColor.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            service.primaryColor.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AppBrandIcon(serviceId = service.id, modifier = Modifier.size(26.dp))
                }

                // Category Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = service.category.uppercase(),
                        color = EvaColors.TextSecondary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App Name
            Text(
                text = service.name,
                color = EvaColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Subtitle Tagline
            Text(
                text = tagline,
                color = EvaColors.TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Micro-indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Launch",
                    color = EvaColors.NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = EvaColors.NeonCyan,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

/**
 * Expressive Hero Bento Card highlighting the primary entertainment or featured app.
 */
@Composable
fun BentoHeroCard(
    featuredService: AppService,
    onLaunch: (AppService) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "heroScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = EvaColors.NeonPurple.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF161B30),
                        Color(0xFF0F1322),
                        Color(0xFF1E1435)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        EvaColors.NeonCyan.copy(alpha = 0.7f),
                        EvaColors.NeonPurple.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onLaunch(featuredService) }
            )
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Live Active Indicator + Feature Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EvaColors.AuroraGreen)
                    )
                    Text(
                        text = "FEATURED HUB",
                        color = EvaColors.AuroraGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EvaColors.NeonCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "0ms Switch",
                        color = EvaColors.NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Large Icon + Title + Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = featuredService.primaryColor)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    featuredService.primaryColor.copy(alpha = 0.4f),
                                    featuredService.secondaryColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(1.2.dp, featuredService.primaryColor.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AppBrandIcon(serviceId = featuredService.id, modifier = Modifier.size(32.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = featuredService.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ad-free native playback • Fullscreen 4K landscape • Mini-player background audio keep-alive",
                        color = EvaColors.TextSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    EvaColors.NeonCyan.copy(alpha = 0.35f),
                                    EvaColors.NeonPurple.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            EvaColors.NeonCyan.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Launch Engine",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Launch",
                            tint = EvaColors.NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
