package com.example.eva.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.eva.BuildConfig
import com.example.eva.data.AppServiceCatalog
import com.example.eva.data.EvaCloudStorageManager
import com.example.eva.data.EvaHybridStorageEngine
import com.example.eva.data.EvaPlayStoreBridgeManager
import com.example.eva.data.EvaUpdateManager
import com.example.eva.data.UserSessionManager
import com.example.eva.diagnostics.EvaDiagnosticsManager
import com.example.eva.model.AppService
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.example.eva.ui.components.BentoHeroCard
import com.example.eva.ui.components.BentoServiceCard
import com.example.eva.ui.components.CrystalBubble
import com.example.eva.ui.components.EvaUpdateDialog
import com.example.eva.ui.components.EvaWebViewPool
import com.example.eva.ui.theme.EvaColors
import java.util.Locale

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
    var manualUpdate by remember { mutableStateOf<EvaUpdateManager.UpdateInfo?>(null) }

    val username = remember { UserSessionManager.getUsername(context) }
    val email = remember { UserSessionManager.getEmail(context) }
    val daysRemaining = remember { UserSessionManager.daysRemainingForUsernameChange(context) }

    var cloudFreeGb by remember { mutableStateOf(UserSessionManager.getCloudStorageFreeGb(context)) }
    var cloudUsedGb by remember { mutableStateOf(UserSessionManager.getCloudStorageUsedGb(context)) }
    var cloudTotalGb by remember { mutableStateOf(UserSessionManager.getCloudStorageTotalGb(context)) }
    var isSyncingQuota by remember { mutableStateOf(false) }

    var selectedDialogTab by remember { mutableStateOf(0) }
    var isPlayStoreBridgeEnabled by remember { mutableStateOf(UserSessionManager.isPlayStoreBridgeConnected(context)) }
    var isHybridSyncing by remember { mutableStateOf(false) }
    var localStorageUsedMb by remember { mutableStateOf(UserSessionManager.getLocalStorageUsedMb(context)) }
    var hardwareSpaceSavedMb by remember { mutableStateOf(UserSessionManager.getHardwareSpaceSavedMb(context)) }

    fun refreshCloudQuota() {
        isSyncingQuota = true
        EvaCloudStorageManager.checkQuotaAndProvision(context, email) { _ ->
            isSyncingQuota = false
            cloudFreeGb = UserSessionManager.getCloudStorageFreeGb(context)
            cloudUsedGb = UserSessionManager.getCloudStorageUsedGb(context)
            cloudTotalGb = UserSessionManager.getCloudStorageTotalGb(context)
            Toast.makeText(context, "Google Cloud Space Synced: ${String.format(Locale.US, "%.1f", cloudFreeGb)} GB Free", Toast.LENGTH_SHORT).show()
        }
    }

    fun runHybridSync() {
        isHybridSyncing = true
        EvaHybridStorageEngine.syncNow(context) { metrics ->
            isHybridSyncing = false
            localStorageUsedMb = metrics.localUsedMb
            hardwareSpaceSavedMb = metrics.hardwareSavedMb
            cloudUsedGb = metrics.cloudUsedGb
            cloudFreeGb = UserSessionManager.getCloudStorageFreeGb(context)
            Toast.makeText(context, "Hybrid Sync Complete: ${metrics.filesSynced} files synced to /EVA_CloudSpace/", Toast.LENGTH_SHORT).show()
        }
    }

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
                                    .clickable { showCloudDetailsDialog = true }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", cloudFreeGb)} GB FREE",
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
                // Hybrid Native Engine Card (Clickable -> Opens Diagnostics Tab)
                val activeCount = EvaDiagnosticsManager.activeWebViewCount
                val lastLoadMs = EvaDiagnosticsManager.lastPageLoadDurationMs
                val engineSubtitle = if (activeCount > 0 && lastLoadMs > 0) "${lastLoadMs}ms • $activeCount Active" else "Hardware Accelerated"

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(
                            1.dp,
                            EvaColors.NeonCyan.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            selectedDialogTab = 2
                            showCloudDetailsDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Engine Diagnostics",
                        tint = EvaColors.NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Hybrid Native Engine",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = engineSubtitle,
                            color = EvaColors.NeonCyan,
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
                            text = "${String.format(Locale.US, "%.1f", cloudFreeGb)} GB Free • 9+ GB Ready",
                            color = EvaColors.NeonCyan,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Modern Cybernetic Omnibox
            OutlinedTextField(
                value = omniQuery,
                onValueChange = { omniQuery = it },
                placeholder = {
                    Text(
                        text = "Search 10+ apps or enter URL...",
                        color = EvaColors.TextMuted,
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(EvaColors.NeonCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web",
                            tint = EvaColors.NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                trailingIcon = {
                    if (omniQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(EvaColors.NeonCyan, EvaColors.NeonPurple)
                                    )
                                )
                                .clickable { executeOmniSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { executeOmniSearch() }),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = EvaColors.BentoCardBgElevated,
                    unfocusedContainerColor = EvaColors.BentoCardBg,
                    focusedBorderColor = EvaColors.NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
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

            // 5. Modern Cyber Bento Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedCategory == "All" && omniQuery.isBlank()) {
                    val featured = allServices.firstOrNull { it.id == "youtube" } ?: allServices.first()
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BentoHeroCard(
                            featuredService = featured,
                            onLaunch = { onSelectService(featured) }
                        )
                    }
                }

                itemsIndexed(filteredServices) { index, service ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 20 * (index + 1) })
                    ) {
                        BentoServiceCard(
                            service = service,
                            onClick = {
                                onSelectService(service)
                            }
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
                    Text("EVA Cloud & Hybrid Vault", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Identity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Account: $email",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "Username: $username",
                                color = EvaColors.NeonCyan,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (daysRemaining > 0) {
                            Text(
                                text = "🔒 $daysRemaining d",
                                color = Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "✅ Verified",
                                color = EvaColors.NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tab Selector: Cloud Space vs Play Store & Hybrid Sync vs Engine Diagnostics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedDialogTab == 0) EvaColors.NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedDialogTab = 0 }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "15 GB Cloud",
                                color = if (selectedDialogTab == 0) EvaColors.NeonCyan else Color.White.copy(alpha = 0.7f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedDialogTab == 1) EvaColors.NeonPurple.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedDialogTab = 1 }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Play Store",
                                color = if (selectedDialogTab == 1) EvaColors.NeonPurple else Color.White.copy(alpha = 0.7f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedDialogTab == 2) Color(0xFF00E676).copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedDialogTab = 2 }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Diagnostics",
                                color = if (selectedDialogTab == 2) Color(0xFF00E676) else Color.White.copy(alpha = 0.7f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (selectedDialogTab == 0) {
                        // TAB 0: 15 GB Cloud Space Vault
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Storage Quota: ${String.format(Locale.US, "%.1f", cloudUsedGb)} GB / ${String.format(Locale.US, "%.1f", cloudTotalGb)} GB",
                                color = EvaColors.TextSecondary,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EvaColors.NeonCyan.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "9+ GB READY",
                                    color = EvaColors.NeonCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { if (cloudTotalGb > 0) (cloudUsedGb / cloudTotalGb).coerceIn(0f, 1f) else 0.16f },
                            color = EvaColors.NeonCyan,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Min 9.0 GB Required Free Space",
                                color = EvaColors.TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", cloudFreeGb)} GB Free Available",
                                color = EvaColors.NeonCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Vault Folder Architecture Breakdown
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("📁 Provisioned Vault Structure (/EVA_CloudSpace/):", color = Color.White.copy(alpha = 0.9f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text("• /Sessions/ (Multi-tab cookie persistence & tab state)", color = EvaColors.TextSecondary, fontSize = 9.sp)
                            Text("• /Media/ (120 FPS playback cache & offline streams)", color = EvaColors.TextSecondary, fontSize = 9.sp)
                            Text("• /Archives/ (Encrypted bookmarks & settings backup)", color = EvaColors.TextSecondary, fontSize = 9.sp)
                        }

                        // Sync Live Quota Button
                        androidx.compose.material3.Button(
                            onClick = { refreshCloudQuota() },
                            enabled = !isSyncingQuota,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EvaColors.NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EvaColors.NeonCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = EvaColors.NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncingQuota) "Syncing Drive Quota..." else "Sync Google Drive Quota",
                                color = EvaColors.NeonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // TAB 1: All-in-One Super-App Architecture & Hardware Savings
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EvaColors.NeonCyan.copy(alpha = 0.12f))
                                .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "All-in-One Super-Engine",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "EVA replaces 10+ battery-draining apps. You only install EVA—all apps run natively inside EVA at 120 FPS without requiring standalone app installs.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // 1. Google Play Store Bridge Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, EvaColors.NeonPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Play Store Bridge",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPlayStoreBridgeEnabled) "Connected with Google Account" else "Bridge Disconnected",
                                    color = if (isPlayStoreBridgeEnabled) EvaColors.NeonPurple else EvaColors.TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Switch(
                                checked = isPlayStoreBridgeEnabled,
                                onCheckedChange = { enabled ->
                                    isPlayStoreBridgeEnabled = enabled
                                    UserSessionManager.setPlayStoreBridgeConnected(context, enabled)
                                    Toast.makeText(context, if (enabled) "Play Store Bridge Linked" else "Play Store Bridge Unlinked", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EvaColors.NeonPurple,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        // 2. Hardware + Cloud Storage Savings Dashboard
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Device Flash Space Saved:",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "~${String.format(Locale.US, "%.1f", hardwareSpaceSavedMb / 1024f)} GB",
                                    color = EvaColors.NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "• Local Flash Cache: ${String.format(Locale.US, "%.1f", localStorageUsedMb)} MB (120 FPS high-speed buffers)",
                                color = EvaColors.TextSecondary,
                                fontSize = 9.5.sp
                            )
                            Text(
                                text = "• Cloud Offload: ${String.format(Locale.US, "%.1f", cloudUsedGb)} GB in /EVA_CloudSpace/",
                                color = EvaColors.TextSecondary,
                                fontSize = 9.5.sp
                            )
                            Text(
                                text = "• Hardware Efficiency: ~98% storage reduction vs heavy native APKs",
                                color = EvaColors.NeonCyan,
                                fontSize = 9.5.sp
                            )
                        }

                        // 3. Hybrid Sync Now Button
                        androidx.compose.material3.Button(
                            onClick = { runHybridSync() },
                            enabled = !isHybridSyncing,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EvaColors.NeonPurple.copy(alpha = 0.25f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EvaColors.NeonPurple.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = EvaColors.NeonPurple, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHybridSyncing) "Syncing Hardware & Cloud..." else "Sync Cloud & Hardware Now",
                                color = EvaColors.NeonPurple,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 4. Quick Play Store Bridge App Launcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { EvaPlayStoreBridgeManager.openInPlayStore(context, "com.instagram.android") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Instagram APK", color = Color.White, fontSize = 9.5.sp)
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { EvaPlayStoreBridgeManager.openInPlayStore(context, "com.google.android.youtube") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("YouTube APK", color = Color.White, fontSize = 9.5.sp)
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { EvaPlayStoreBridgeManager.openPlayStoreApp(context) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play Store", color = EvaColors.NeonCyan, fontSize = 9.5.sp)
                            }
                        }
                    }

                    if (selectedDialogTab == 2) {
                        // TAB 2: Real-time Chromium Engine 3.0 Performance Diagnostics
                        val memory = remember { EvaDiagnosticsManager.getMemoryDiagnostics(context) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ENGINE 3.0 TELEMETRY",
                                color = EvaColors.NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Active WebViews in LRU Pool:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.activeWebViewCount} / 3 Max", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Renderer Crash Recoveries:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.totalRendererCrashesRecovered} (Auto-Restored)", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last WebView Init Duration:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.lastInitDurationMs} ms", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Page Load Duration:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.lastPageLoadDurationMs} ms", color = EvaColors.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Service Switch Latency:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.lastServiceSwitchDurationMs} ms", color = EvaColors.NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Page / Network Failures:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.totalPageLoadFailures} page • ${EvaDiagnosticsManager.totalNetworkFailures} net", color = if (EvaDiagnosticsManager.totalPageLoadFailures > 0) Color(0xFFFFB74D) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Downloads Completed:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${EvaDiagnosticsManager.totalDownloadsCompleted}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("JVM Heap Usage:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${String.format(Locale.US, "%.1f", memory.usedHeapMb)} MB / ${String.format(Locale.US, "%.1f", memory.maxHeapMb)} MB", color = EvaColors.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("App Local Cache:", color = EvaColors.TextSecondary, fontSize = 10.sp)
                                Text("${String.format(Locale.US, "%.1f", memory.cacheDirSizeMb)} MB", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Open Source GDPR & Data Protection Action
                    androidx.compose.material3.Button(
                        onClick = {
                            EvaWebViewPool.clearAllDataAndCookies(context)
                            showCloudDetailsDialog = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1014)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Erase Browsing Data & Reset Cache", color = Color(0xFFFF8A80), fontSize = 11.5.sp)
                    }

                    // GitHub Auto-Update Checker Button
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showCloudDetailsDialog = false
                            EvaUpdateManager.checkForUpdates(context, force = true) { update ->
                                if (update != null) {
                                    manualUpdate = update
                                } else {
                                    Toast.makeText(context, "EVA is up-to-date! (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check for GitHub Updates", color = EvaColors.NeonCyan, fontSize = 11.5.sp)
                    }
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

    if (manualUpdate != null) {
        EvaUpdateDialog(
            updateInfo = manualUpdate!!,
            onDismiss = { manualUpdate = null }
        )
    }
}
