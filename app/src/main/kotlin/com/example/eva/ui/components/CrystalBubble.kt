package com.example.eva.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.model.AppService
import com.example.eva.ui.theme.EvaColors

@Composable
fun CrystalBubble(
    service: AppService,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Interactive spring physics that only animates on user touch (0% idle CPU)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "bubblePressScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        // The Crystal Orb Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = service.primaryColor.copy(alpha = 0.5f),
                    ambientColor = service.secondaryColor.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            service.primaryColor.copy(alpha = 0.28f),
                            service.secondaryColor.copy(alpha = 0.18f),
                            EvaColors.GlassSurface
                        ),
                        center = Offset(70f, 60f)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            service.primaryColor.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.15f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(140f, 140f)
                    ),
                    shape = CircleShape
                )
        ) {
            // Glass Specular Highlight Arc
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, 80f)
                        )
                    )
            )

            // Inner Shadow Vignette
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            // Official Service Brand Vector Icon
            AppBrandIcon(
                serviceId = service.id,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Typography
        Text(
            text = service.name,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp
        )
    }
}
