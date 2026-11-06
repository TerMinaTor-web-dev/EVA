package com.example.eva.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.eva.data.EvaPlayStoreBridgeManager
import com.example.eva.model.AppService
import com.example.eva.ui.theme.EvaColors

@Composable
fun EvaNativeApkInstallDialog(
    service: AppService,
    onDismiss: () -> Unit,
    onInstallApk: (AppService) -> Unit,
    onLaunchWebFallback: (AppService) -> Unit
) {
    val estimatedWeightMb = EvaPlayStoreBridgeManager.getNativeAppWeightMb(service.id)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = EvaColors.NeonCyan)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            EvaColors.NeonCyan.copy(alpha = 0.8f),
                            EvaColors.NeonPurple.copy(alpha = 0.5f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                ),
            color = Color(0xFF0F111A)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar with Brand Icon and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape, spotColor = service.primaryColor)
                                .clip(CircleShape)
                                .background(Color(0xFF141724))
                                .border(1.5.dp, service.primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AppBrandIcon(serviceId = service.id, modifier = Modifier.size(26.dp))
                        }

                        Column {
                            Text(
                                text = "Install ${service.name} APK",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Full Native Hardware Mode",
                                color = EvaColors.NeonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Core Value Prop Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EvaColors.NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "To run ${service.name} with 100% native efficiency, install the official Android APK. EVA's Hybrid Sync Engine offloads heavy media to your 15 GB Cloud Vault to keep device storage minimal.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature Highlights
                FeatureRow(
                    icon = Icons.Default.FlashOn,
                    iconTint = EvaColors.NeonCyan,
                    title = "120 FPS GPU Acceleration",
                    subtitle = "Direct Vulkan/OpenGL rendering for silky-smooth native gestures, camera & mic."
                )

                Spacer(modifier = Modifier.height(10.dp))

                FeatureRow(
                    icon = Icons.Default.CloudDone,
                    iconTint = EvaColors.NeonPurple,
                    title = "15 GB Cloud Auto-Sync",
                    subtitle = "Your app archives and downloaded media automatically sync to /EVA_CloudSpace/."
                )

                Spacer(modifier = Modifier.height(10.dp))

                FeatureRow(
                    icon = Icons.Default.Save,
                    iconTint = Color(0xFF00E676),
                    title = "Device Storage Saver (~$estimatedWeightMb MB)",
                    subtitle = "Local flash cache capped at 250 MB; heavy data safely preserved in cloud."
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Install Native APK
                Button(
                    onClick = { onInstallApk(service) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = EvaColors.NeonCyan),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(EvaColors.NeonCyan, EvaColors.NeonPurple)
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Install Official Native APK",
                                color = Color.Black,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Instant Web Fallback
                OutlinedButton(
                    onClick = { onLaunchWebFallback(service) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "Use Instant Web Mode for Now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = EvaColors.TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}
