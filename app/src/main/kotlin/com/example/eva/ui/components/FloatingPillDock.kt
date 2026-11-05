package com.example.eva.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.scale
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
    onNavigateHome: () -> Unit,
    onReloadCurrent: () -> Unit,
    onOpenAiAssistant: () -> Unit,
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
    val currentIndex = services.indexOfFirst { it.id == currentService.id }

    fun switchNext() {
        val nextIndex = (currentIndex + 1) % services.size
        val nextService = services[nextIndex]
        onSelectService(nextService)
        gestureHudMessage = nextService.name
    }

    fun switchPrev() {
        val prevIndex = if (currentIndex <= 0) services.size - 1 else currentIndex - 1
        val prevService = services[prevIndex]
        onSelectService(prevService)
        gestureHudMessage = prevService.name
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Gesture Switch HUD
        AnimatedVisibility(
            visible = gestureHudMessage != null,
            enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessHigh)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.padding(bottom = 44.dp)
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
                        text = "Switched to ${gestureHudMessage}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. Collapsed State: Micro Pill Bar with Gestures (Swipe to switch, Tap for Taskbar)
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
                                if (dragAccumulator > 35f) {
                                    switchPrev()
                                } else if (dragAccumulator < -35f) {
                                    switchNext()
                                }
                                dragAccumulator = 0f
                            },
                            onDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount.x
                            }
                        )
                    }
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                // Sleek, minimal home pill bar (width: 54dp, height: 5.5dp)
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 5.5.dp)
                        .shadow(10.dp, RoundedCornerShape(3.dp), spotColor = currentService.primaryColor)
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

                Spacer(modifier = Modifier.height(3.dp))

                // Subtle swipe hint label
                Text(
                    text = "◂ Swipe to switch • Tap for dock ▸",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { isExpanded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dock",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal App Carousel Dock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Hub Action
                    DockActionButton(
                        label = "Hub",
                        icon = Icons.Default.Home,
                        color = EvaColors.NeonCyan,
                        onClick = {
                            isExpanded = false
                            onNavigateHome()
                        }
                    )

                    // Reload Page Action
                    DockActionButton(
                        label = "Reload",
                        icon = Icons.Default.Refresh,
                        color = Color.White.copy(alpha = 0.8f),
                        onClick = {
                            isExpanded = false
                            onReloadCurrent()
                        }
                    )

                    // EVA AI Assistant Action
                    DockActionButton(
                        label = "AI",
                        icon = Icons.Default.AutoAwesome,
                        color = EvaColors.NeonPurple,
                        onClick = {
                            isExpanded = false
                            onOpenAiAssistant()
                        }
                    )

                    // App Services with Authentic Logos
                    services.forEach { service ->
                        val isSelected = service.id == currentService.id
                        DockAppItem(
                            service = service,
                            isSelected = isSelected,
                            onClick = {
                                isExpanded = false
                                if (!isSelected) {
                                    onSelectService(service)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = EvaColors.TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DockAppItem(
    service: AppService,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        label = "dockItemScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) service.primaryColor.copy(alpha = 0.35f)
                    else EvaColors.GlassSurface
                )
                .border(
                    width = if (isSelected) 1.8.dp else 1.dp,
                    color = if (isSelected) service.primaryColor else Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            AppBrandIcon(serviceId = service.id, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = service.name,
            color = if (isSelected) EvaColors.NeonCyan else EvaColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
