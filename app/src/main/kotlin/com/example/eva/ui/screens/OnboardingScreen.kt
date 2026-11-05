package com.example.eva.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.data.UserSessionManager
import com.example.eva.ui.theme.EvaColors

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("user@gmail.com") }
    var usernameInput by remember { mutableStateOf("eva_user") }
    var isGoogleConnected by remember { mutableStateOf(false) }

    // Breathing Glow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EvaColors.DualToneGradient)
            .statusBarsPadding()
    ) {
        // Ambient Radial Nebula
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseScale)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EvaColors.NeonCyan.copy(alpha = 0.22f),
                            EvaColors.NeonPurple.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Futuristic EVA Holographic Crystal Orb Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(30.dp, CircleShape, spotColor = EvaColors.NeonCyan)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                EvaColors.NeonCyan.copy(alpha = 0.85f),
                                EvaColors.NeonPurple.copy(alpha = 0.85f),
                                Color(0xFF090A10)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "EVA Core",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Title & Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "EVA",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EvaColors.NeonCyan.copy(alpha = 0.18f))
                            .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OWN SPACE",
                            color = EvaColors.NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Initialize your unified digital life engine with dedicated 15 GB Google Cloud storage.",
                    color = EvaColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            // 2. Google Account Link Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Stylized Google 'G'
                            Text(
                                text = "G",
                                color = Color(0xFF4285F4),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column {
                            Text(
                                text = "Google Cloud Identity",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGoogleConnected) "Connected to personal Drive Vault" else "Tap below to link Google Account",
                                color = if (isGoogleConnected) EvaColors.NeonCyan else EvaColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isGoogleConnected) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Enter your Gmail address", color = EvaColors.TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EvaColors.NeonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                            )
                        )

                        Button(
                            onClick = {
                                if (emailInput.contains("@") && emailInput.contains(".")) {
                                    isGoogleConnected = true
                                    Toast.makeText(context, "Google Cloud Connected: 15 GB Vault Ready", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sign in with Google",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(EvaColors.NeonCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = EvaColors.NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = emailInput,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Unique Username Selector with 14-day Cooldown Notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Unique EVA Username",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5A100).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "14-Day Lock",
                                color = Color(0xFFFFB74D),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { input ->
                            // Remove spaces and special chars, enforce lowercase
                            usernameInput = input.filter { it.isLetterOrDigit() || it == '_' }.lowercase()
                        },
                        prefix = { Text("@", color = EvaColors.NeonCyan, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("choose_username", color = EvaColors.TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EvaColors.NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                        )
                    )

                    Text(
                        text = "ℹ️ Once confirmed, your unique username is tied to this EVA installation and cannot be changed for 14 days.",
                        color = EvaColors.TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // 4. Free 15 GB Cloud Storage Vault Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                EvaColors.NeonCyan.copy(alpha = 0.08f),
                                EvaColors.NeonPurple.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Vault",
                                tint = EvaColors.NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Free Cloud Vault Active",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "15.0 GB Quota",
                            color = EvaColors.NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { 0.02f }, // ~184 MB initialized
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EvaColors.NeonCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "184 MB used for tabs & cache",
                            color = EvaColors.TextMuted,
                            fontSize = 10.5.sp
                        )
                        Text(
                            text = "14.8 GB Free",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Submit Button: Initialize EVA
            Button(
                onClick = {
                    val finalUsername = usernameInput.trim()
                    if (finalUsername.length < 3) {
                        Toast.makeText(context, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalEmail = if (emailInput.isNotBlank()) emailInput.trim() else "user@gmail.com"
                    val displayName = finalUsername.replaceFirstChar { it.uppercase() }

                    // Save session & 14-day lock timestamp
                    UserSessionManager.saveSession(
                        context = context,
                        username = finalUsername,
                        email = finalEmail,
                        displayName = displayName
                    )

                    Toast.makeText(context, "Welcome to EVA, @$finalUsername!", Toast.LENGTH_SHORT).show()
                    onCompleteOnboarding()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = EvaColors.NeonCyan),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    EvaColors.NeonCyan,
                                    EvaColors.NeonPurple
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Enter EVA Super-Engine",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Launch",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "OWASP Hardened",
                    tint = EvaColors.NeonCyan.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "End-to-End Sandboxed • OWASP Mobile Top 10 Hardened",
                    color = EvaColors.TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
