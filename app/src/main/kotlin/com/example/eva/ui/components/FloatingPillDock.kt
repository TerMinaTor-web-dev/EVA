package com.example.eva.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.data.AppServiceCatalog
import com.example.eva.model.AppService
import com.example.eva.ui.theme.EvaColors
import kotlinx.coroutines.delay

@Composable
fun FloatingPillDock(
    currentService: AppService,
    onSelectService: (AppService) -> Unit,
    onSwipeRecentBackward: () -> String?,
    onSwipeRecentForward: () -> String?,
    onNavigateHome: () -> Unit,
    onReloadCurrent: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    canGoBack: Boolean = false,
    onGoBack: () -> Unit = {},
    canGoForward: Boolean = false,
    onGoForward: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var gestureHudMessage by remember { mutableStateOf<String?>(null) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    // Auto-dismiss gesture switch HUD feedback
    LaunchedEffect(gestureHudMessage) {
        if (gestureHudMessage != null) {
            delay(1200)
            gestureHudMessage = null
        }
    }

    val services = AppServiceCatalog.services

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Gesture Switch HUD
        AnimatedVisibility(
            visible = gestureHudMessage != null,
            enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessHigh)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.padding(bottom = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD0D111E))
                    .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppBrandIcon(serviceId = currentService.id, modifier = Modifier.size(20.dp))
                    Text(
                        text = gestureHudMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. Collapsed State: Micro Pill Bar with Chronological Gestures
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .pointerInput(currentService.id) {
                        detectTapGestures(
                            onTap = { isExpanded = true },
                            onLongPress = { onOpenAiAssistant() }
                        )
                    }
                    .pointerInput(currentService.id) {
                        detectDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (dragAccumulator > 30f) {
                                    // Swipe Right -> Chronological Recent Apps
                                    val switched = onSwipeRecentBackward()
                                    gestureHudMessage = switched?.let { "Recent: $it" } ?: "No older recent apps"
                                } else if (dragAccumulator < -30f) {
                                    // Swipe Left -> Reverse Forward History
                                    val switched = onSwipeRecentForward()
                                    gestureHudMessage = switched?.let { "Forward: $it" } ?: "Already on latest app"
                                }
                                dragAccumulator = 0f
                            },
                            onDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount.x
                            }
                        )
                    }
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                // Sleek, minimal home pill bar (width: 50dp, height: 5dp)
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 5.dp)
                        .shadow(8.dp, RoundedCornerShape(3.dp), spotColor = currentService.primaryColor)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    currentService.primaryColor.copy(alpha = 0.9f),
                                    Color.White,
                                    currentService.secondaryColor.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Subtle swipe hint label
                Text(
                    text = "◂ Swipe recent • Tap for dock ▸",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // 2. Expanded State: Rectangular Glassmorphic Taskbar
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = EvaColors.NeonPurple)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF2101320),
                                Color(0xF21A1535)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.6f),
                                EvaColors.NeonCyan.copy(alpha = 0.5f),
                                EvaColors.NeonPurple.copy(alpha = 0.5f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(12.dp)
            ) {
                // Header of Dock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EVA TASKBAR",
                            color = EvaColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Instant 0ms Switcher",
                            color = EvaColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { isExpanded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dock",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Service Icons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    services.forEach { service ->
                        val isSelected = service.id == currentService.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) EvaColors.NeonCyan.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) EvaColors.NeonCyan else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSelectService(service)
                                    isExpanded = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            AppBrandIcon(serviceId = service.id, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = service.name,
                                color = if (isSelected) EvaColors.NeonCyan else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dock Controls Footer (Back, Forward, Home, Refresh, AI Assistant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation Pair: Back & Forward
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canGoBack) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                .clickable(enabled = canGoBack) { onGoBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) EvaColors.NeonCyan else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Forward
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canGoForward) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                .clickable(enabled = canGoForward) { onGoForward() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) EvaColors.NeonCyan else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Home Hub Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                isExpanded = false
                                onNavigateHome()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = EvaColors.NeonCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Hub",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Reload Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                isExpanded = false
                                onReloadCurrent()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = EvaColors.NeonPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Reload",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // EVA AI Assistant Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        EvaColors.NeonCyan.copy(alpha = 0.3f),
                                        EvaColors.NeonPurple.copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .clickable {
                                isExpanded = false
                                onOpenAiAssistant()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "EVA AI",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "EVA AI",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
