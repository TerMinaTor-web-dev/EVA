package com.example.eva.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.data.AppServiceCatalog
import com.example.eva.model.AppService
import com.example.eva.ui.components.CrystalBubble
import com.example.eva.ui.theme.EvaColors

@Composable
fun HomeScreen(
    onSelectService: (AppService) -> Unit,
    onOpenAiCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    var omniQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun executeOmniSearch() {
        val trimmed = omniQuery.trim()
        if (trimmed.isBlank()) return
        focusManager.clearFocus()

        val destinationUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }

        val customService = AppService(
            id = "custom_${System.currentTimeMillis()}",
            name = if (trimmed.length > 14) trimmed.take(12) + ".." else trimmed,
            url = destinationUrl,
            category = "Web",
            primaryColor = EvaColors.NeonCyan,
            secondaryColor = EvaColors.NeonPurple,
            description = destinationUrl
        )
        AppServiceCatalog.registerService(customService)
        onSelectService(customService)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EvaColors.DualToneGradient)
    ) {
        // Ambient Radial Nebula Glows
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EvaColors.AmbientGlow2, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EvaColors.AmbientGlow1, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Brand Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EVA",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EvaColors.NeonCyan.copy(alpha = 0.18f))
                                .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "SUPER ENGINE",
                                color = EvaColors.NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                    Text(
                        text = "Unified Digital Life • 120 FPS Zero-Lag",
                        color = EvaColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // AI Co-Pilot Summon Orb
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(16.dp, CircleShape, spotColor = EvaColors.NeonPurple)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(EvaColors.NeonCyan, EvaColors.NeonPurple)
                            )
                        )
                        .clickable(onClick = onOpenAiCopilot),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "EVA AI",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Universal Omnibox: Browse any URL, search web, or consult EVA
            OutlinedTextField(
                value = omniQuery,
                onValueChange = { omniQuery = it },
                placeholder = {
                    Text(
                        text = "Browse any URL or search web...",
                        color = EvaColors.TextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Web",
                        tint = EvaColors.NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (omniQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EvaColors.NeonCyan.copy(alpha = 0.25f))
                                .clickable { executeOmniSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go",
                                tint = EvaColors.NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { executeOmniSearch() }),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = EvaColors.GlassSurface,
                    unfocusedContainerColor = EvaColors.GlassSurface,
                    focusedBorderColor = EvaColors.NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = EvaColors.NeonCyan
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(26.dp))

            // 3-Column Floating Crystal Bubbles Grid with Authentic Brand Logos
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(AppServiceCatalog.services) { index, service ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 * (index + 1) })
                    ) {
                        CrystalBubble(
                            service = service,
                            index = index,
                            onClick = { onSelectService(service) }
                        )
                    }
                }
            }
        }
    }
}
