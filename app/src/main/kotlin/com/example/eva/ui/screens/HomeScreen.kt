package com.example.eva.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.data.AppServiceCatalog
import com.example.eva.data.UserSessionManager
import com.example.eva.model.AppService
import com.example.eva.ui.components.CrystalBubble
import com.example.eva.ui.theme.EvaColors

@Composable
fun HomeScreen(
    onSelectService: (AppService) -> Unit,
    onOpenAiCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var omniQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCloudDetailsDialog by remember { mutableStateOf(false) }

    val username = remember { UserSessionManager.getUsername(context) }
    val email = remember { UserSessionManager.getEmail(context) }
    val daysRemaining = remember { UserSessionManager.daysRemainingForUsernameChange(context) }

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

    // Filter services based on category
    val allServices = AppServiceCatalog.services
    val filteredServices = remember(selectedCategory) {
        when (selectedCategory) {
            "Social" -> allServices.filter { it.category == "Social" || it.id in listOf("instagram", "facebook", "twitter") }
            "Media" -> allServices.filter { it.category in listOf("Video", "Music") || it.id in listOf("youtube", "ytmusic", "spotify") }
            "Community" -> allServices.filter { it.category in listOf("Community", "Professional") || it.id in listOf("reddit", "linkedin") }
            else -> allServices
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EvaColors.DualToneGradient)
    ) {
        // Ambient Radial Nebula Glows
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(EvaColors.AmbientGlow2, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(320.dp)
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
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Futuristic Profile & Cloud Storage Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Profile & Space Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(12.dp, CircleShape, spotColor = EvaColors.NeonCyan)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(EvaColors.NeonCyan, EvaColors.NeonPurple)
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            .clickable { showCloudDetailsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.drop(1).take(2).uppercase().ifBlank { "EV" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = username,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EvaColors.NeonCyan.copy(alpha = 0.2f))
                                    .border(0.5.dp, EvaColors.NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "15 GB CLOUD",
                                    color = EvaColors.NeonCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Unified Super-Engine • Web-Core Fidelity",
                            color = EvaColors.TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // AI Co-Pilot Summon Orb
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. High-Tech Performance & Cloud Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 120 FPS Engine Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "FPS",
                        tint = EvaColors.NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "120 FPS Engine",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zero-latency tabs",
                            color = EvaColors.TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Cloud Vault Card (Clickable)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { showCloudDetailsDialog = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Cloud Vault",
                        tint = EvaColors.NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "15 GB Cloud Vault",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap for vault details",
                            color = EvaColors.NeonCyan,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Universal Omnibox
            OutlinedTextField(
                value = omniQuery,
                onValueChange = { omniQuery = it },
                placeholder = {
                    Text(
                        text = "Search or enter any web URL...",
                        color = EvaColors.TextMuted,
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Web",
                        tint = EvaColors.NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (omniQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(EvaColors.NeonCyan.copy(alpha = 0.25f))
                                .clickable { executeOmniSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go",
                                tint = EvaColors.NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { executeOmniSearch() }),
                shape = RoundedCornerShape(16.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("All", "Social", "Media", "Community")
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val bgColor by animateColorAsState(
                        if (isSelected) EvaColors.NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                        label = "catBg"
                    )
                    val borderColor by animateColorAsState(
                        if (isSelected) EvaColors.NeonCyan else Color.White.copy(alpha = 0.12f),
                        label = "catBorder"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (cat == "All") "All (${allServices.size})" else cat,
                            color = if (isSelected) EvaColors.NeonCyan else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Floating Crystal Bubbles Grid with Authentic Brand Logos
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredServices) { index, service ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 * (index + 1) })
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

    // Cloud Details & Username Cooldown Dialog
    if (showCloudDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showCloudDetailsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Cloud",
                        tint = EvaColors.NeonCyan
                    )
                    Text("EVA Cloud Space Vault", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Linked Google Account: $email",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Username: $username",
                        color = EvaColors.NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (daysRemaining > 0) {
                        Text(
                            text = "🔒 Username edit locked for $daysRemaining more days to protect identity security.",
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "✅ Username can be edited.",
                            color = EvaColors.NeonCyan,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Storage Quota: 184 MB / 15.0 GB used",
                        color = EvaColors.TextSecondary,
                        fontSize = 11.sp
                    )
                    LinearProgressIndicator(
                        progress = { 0.02f },
                        color = EvaColors.NeonCyan,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudDetailsDialog = false }) {
                    Text("Close", color = EvaColors.NeonCyan)
                }
            },
            containerColor = Color(0xFF101320),
            shape = RoundedCornerShape(18.dp)
        )
    }
}
