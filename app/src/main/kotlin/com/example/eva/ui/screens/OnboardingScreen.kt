package com.example.eva.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eva.data.EvaCloudStorageManager
import com.example.eva.data.UserSessionManager
import com.example.eva.security.EvaPermissionManager
import com.example.eva.ui.theme.EvaColors

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var isGoogleConnected by remember { mutableStateOf(false) }
    var showManualEmailInput by remember { mutableStateOf(false) }

    // Google Drive 15 GB Quota & 9+ GB Free Threshold Verification State
    var isCheckingQuota by remember { mutableStateOf(false) }
    var cloudQuota by remember { mutableStateOf<EvaCloudStorageManager.StorageQuota?>(null) }
    var cloudVault by remember { mutableStateOf<EvaCloudStorageManager.VaultFolderHierarchy?>(null) }
    var quotaErrorMessage by remember { mutableStateOf<String?>(null) }
    var isQuotaEligible by remember { mutableStateOf(false) }

    fun verifyQuota(email: String) {
        val clean = email.trim()
        if (clean.isBlank() || !clean.contains("@") || !clean.contains(".")) {
            isGoogleConnected = false
            cloudQuota = null
            cloudVault = null
            isQuotaEligible = false
            quotaErrorMessage = null
            return
        }
        isCheckingQuota = true
        quotaErrorMessage = null
        EvaCloudStorageManager.checkQuotaAndProvision(context, clean) { result ->
            isCheckingQuota = false
            when (result) {
                is EvaCloudStorageManager.QuotaResult.Success -> {
                    cloudQuota = result.quota
                    cloudVault = result.vault
                    isQuotaEligible = true
                    quotaErrorMessage = null
                    isGoogleConnected = true
                    Toast.makeText(context, "Google Cloud Space Verified: ${result.quota.formatFreeGb()} GB Free", Toast.LENGTH_SHORT).show()
                }
                is EvaCloudStorageManager.QuotaResult.InsufficientSpace -> {
                    cloudQuota = result.quota
                    cloudVault = null
                    isQuotaEligible = false
                    quotaErrorMessage = result.message
                    isGoogleConnected = true
                    Toast.makeText(context, "Insufficient Drive Space: 9+ GB required", Toast.LENGTH_LONG).show()
                }
                is EvaCloudStorageManager.QuotaResult.Error -> {
                    cloudQuota = result.fallbackQuota
                    cloudVault = null
                    isQuotaEligible = false
                    quotaErrorMessage = result.reason
                    isGoogleConnected = true
                }
            }
        }
    }

    // Dynamic Permission Status State
    var permissionStatus by remember {
        mutableStateOf(EvaPermissionManager.getDetailedStatus(context))
    }

    // 1. Google Native Device Account Picker Launcher
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                emailInput = accountName
                showManualEmailInput = false

                // Auto-suggest username from email prefix
                val prefix = accountName.substringBefore("@")
                    .filter { it.isLetterOrDigit() || it == '_' }
                    .lowercase()
                if (prefix.isNotBlank() && usernameInput.isBlank()) {
                    usernameInput = prefix
                }
                verifyQuota(accountName)
            }
        }
    }

    // 2. Multi-Permission Request Launcher (Camera, Mic, Storage, Notifications)
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionStatus = EvaPermissionManager.getDetailedStatus(context)
        Toast.makeText(context, "Permissions Updated", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EvaColors.DualToneGradient)
            .statusBarsPadding()
    ) {
        // High-efficiency Static Ambient Radial Glow (0% idle CPU)
        Box(
            modifier = Modifier
                .size(320.dp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Holographic Crystal Orb Header
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .shadow(24.dp, CircleShape, spotColor = EvaColors.NeonCyan)
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
                    modifier = Modifier.size(40.dp)
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
                        fontSize = 30.sp,
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sign in with your Google account to initialize your personal 15 GB Cloud Storage Vault and unified web sandbox.",
                    color = EvaColors.TextSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }

            // 2. Google Native Device Account Picker Card
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
                                text = if (isGoogleConnected) "Linked to Google Vault" else "Tap below to link Google Account",
                                color = if (isGoogleConnected) EvaColors.NeonCyan else EvaColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isGoogleConnected) {
                        // Native Google Device Account Picker Button
                        Button(
                            onClick = {
                                try {
                                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        AccountManager.newChooseAccountIntent(
                                            null,
                                            null,
                                            arrayOf("com.google"),
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        AccountManager.newChooseAccountIntent(
                                            null, null, arrayOf("com.google"), false, null, null, null, null
                                        )
                                    }
                                    googleAccountPickerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Select your account or enter your Gmail below", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Choose Google Account from Device",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "— OR ENTER GMAIL MANUALLY —",
                                color = EvaColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                val clean = it.trim()
                                emailInput = clean
                                if (clean.contains("@") && clean.contains(".")) {
                                    val prefix = clean.substringBefore("@")
                                        .filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                                        .lowercase()
                                    if (prefix.isNotBlank() && usernameInput.isBlank()) {
                                        usernameInput = prefix
                                    }
                                    verifyQuota(clean)
                                } else {
                                    isGoogleConnected = false
                                    isQuotaEligible = false
                                    quotaErrorMessage = null
                                    cloudQuota = null
                                }
                            },
                            placeholder = { Text("yourname@gmail.com", color = EvaColors.TextMuted, fontSize = 12.sp) },
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
                    } else {
                        val isAlert = quotaErrorMessage != null && !isQuotaEligible
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAlert) Color(0xFF3B1014) else EvaColors.NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, if (isAlert) Color(0xFFFF5252).copy(alpha = 0.6f) else EvaColors.NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = if (isAlert) "Alert" else "Active",
                                    tint = if (isAlert) Color(0xFFFF5252) else EvaColors.NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = emailInput,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            isCheckingQuota -> "Checking Google Drive Quota..."
                                            isAlert -> "Quota Alert: ${cloudQuota?.formatFreeGb() ?: "0.0"} GB Free (< 9 GB)"
                                            isQuotaEligible -> "15 GB Personal Cloud Space • ${cloudQuota?.formatFreeGb() ?: "12.6"} GB Free"
                                            else -> "15 GB Personal Cloud Vault Linked"
                                        },
                                        color = if (isAlert) Color(0xFFFF8A80) else EvaColors.NeonCyan,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            Text(
                                text = "Change",
                                color = if (isAlert) Color(0xFFFF8A80) else EvaColors.NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        isGoogleConnected = false
                                        showManualEmailInput = true
                                        cloudQuota = null
                                        quotaErrorMessage = null
                                        isQuotaEligible = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Device Runtime Permissions Card
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
                            text = "Device Permissions",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (permissionStatus.allGranted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Granted",
                                    tint = EvaColors.NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Active",
                                    color = EvaColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Permission Badges Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PermissionBadge("Storage", Icons.Default.Folder, permissionStatus.storageGranted)
                        PermissionBadge("Mic", Icons.Default.Mic, permissionStatus.micGranted)
                        PermissionBadge("Camera", Icons.Default.CameraAlt, permissionStatus.cameraGranted)
                        PermissionBadge("Player", Icons.Default.Notifications, permissionStatus.notificationsGranted)
                    }

                    if (!permissionStatus.allGranted) {
                        Button(
                            onClick = {
                                multiPermissionLauncher.launch(EvaPermissionManager.getRequiredPermissions())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EvaColors.NeonCyan.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EvaColors.NeonCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grant Storage, Mic & Camera Permissions",
                                color = EvaColors.NeonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Unique Username Selector with 14-day Cooldown Notice
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

            // 5. Free 15 GB Cloud Storage Vault & 9+ GB Free Space Quota Enforcement Engine
            when {
                isCheckingQuota -> {
                    // Scanning state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, EvaColors.NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Scanning",
                                    tint = EvaColors.NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Analyzing Google Drive Storage Quota...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = EvaColors.NeonCyan,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "Evaluating free space (strictly requires 9.0+ GB free space)...",
                                color = EvaColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                quotaErrorMessage != null || (cloudQuota != null && !cloudQuota!!.isEligible) -> {
                    // Insufficient Space Warning Card (< 9 GB)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF260D10))
                            .border(1.5.dp, Color(0xFFFF5252).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Deficit",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Insufficient Google Drive Free Space",
                                        color = Color(0xFFFF8A80),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Minimum 9.0 GB Free Space Required",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            // Metrics Table
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Available", color = EvaColors.TextMuted, fontSize = 10.sp)
                                    Text(
                                        text = "${cloudQuota?.formatFreeGb() ?: "0.0"} GB",
                                        color = Color(0xFFFF8A80),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("Required", color = EvaColors.TextMuted, fontSize = 10.sp)
                                    Text("9.0 GB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Deficit to Clear", color = EvaColors.TextMuted, fontSize = 10.sp)
                                    Text(
                                        text = "${cloudQuota?.formatDeficitGb() ?: "0.0"} GB",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "⚠️ Your Google Drive does not have enough free space to mount EVA's unified cloud vault. Please clean up Drive/Gmail files or link another Google Account with at least 9 GB free space.",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { verifyQuota(emailInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-verify", color = Color.White, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        isGoogleConnected = false
                                        showManualEmailInput = true
                                        cloudQuota = null
                                        quotaErrorMessage = null
                                        isQuotaEligible = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Switch Account", color = Color(0xFFFF8A80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                isQuotaEligible && cloudQuota != null -> {
                    // Verified Eligible Cloud Space Card (>= 9 GB free)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        EvaColors.NeonCyan.copy(alpha = 0.1f),
                                        EvaColors.NeonPurple.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .border(1.2.dp, EvaColors.NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Personal Cloud Space Activated",
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Linked to $emailInput",
                                            color = EvaColors.TextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EvaColors.NeonCyan.copy(alpha = 0.2f))
                                        .border(0.5.dp, EvaColors.NeonCyan, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "9+ GB VERIFIED",
                                        color = EvaColors.NeonCyan,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { cloudQuota!!.usedRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EvaColors.NeonCyan,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${cloudQuota!!.formatUsedGb()} GB used of 15.0 GB",
                                    color = EvaColors.TextMuted,
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = "${cloudQuota!!.formatFreeGb()} GB Free",
                                    color = EvaColors.NeonCyan,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Cloud Vault Hierarchy Breakdown
                            Text(
                                text = "📁 Provisioned Cloud Vault Architecture:",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("• /EVA_CloudSpace/Sessions/ (Multi-tab cookie persistence & memory sync)", color = EvaColors.TextSecondary, fontSize = 9.5.sp)
                                Text("• /EVA_CloudSpace/Media/ (Stream caching & 120 FPS playback buffer)", color = EvaColors.TextSecondary, fontSize = 9.5.sp)
                                Text("• /EVA_CloudSpace/Archives/ (Encrypted bookmarks & settings backup)", color = EvaColors.TextSecondary, fontSize = 9.5.sp)
                            }
                        }
                    }
                }
                else -> {
                    // Default Informational Card
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
                                        text = "Personal Cloud Vault",
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
                                progress = { 0.16f },
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
                                    text = "Requires 9.0+ GB Free Space",
                                    color = EvaColors.TextMuted,
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = "15 GB Personal Vault",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "💡 Linking your Google account provisions a private 15 GB Cloud Space (/EVA_CloudSpace/). To guarantee high performance, EVA enforces a minimum of 9.0 GB free space.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 6. Enter EVA Super-Engine Button with Real Verification
            val isFormValid = emailInput.isNotBlank() && emailInput.contains("@") && emailInput.contains(".") && usernameInput.length >= 3 && isQuotaEligible && !isCheckingQuota

            Button(
                onClick = {
                    if (isCheckingQuota) {
                        Toast.makeText(context, "Verifying Google Drive storage quota...", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (!isQuotaEligible) {
                        if (quotaErrorMessage != null) {
                            Toast.makeText(context, "Insufficient Google Drive space. EVA requires at least 9 GB free space to continue.", Toast.LENGTH_LONG).show()
                        } else {
                            verifyQuota(emailInput)
                        }
                        return@Button
                    }

                    val finalUsername = usernameInput.trim()
                    val finalEmail = emailInput.trim()

                    if (finalEmail.isBlank() || !finalEmail.contains("@") || !finalEmail.contains(".")) {
                        Toast.makeText(context, "Please choose or enter your Google email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (finalUsername.length < 3) {
                        Toast.makeText(context, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val displayName = finalUsername.replaceFirstChar { it.uppercase() }

                    // Save session & 14-day lock timestamp
                    UserSessionManager.saveSession(
                        context = context,
                        username = finalUsername,
                        email = finalEmail,
                        displayName = displayName
                    )

                    Toast.makeText(context, "Welcome to EVA, @$finalUsername! 15 GB Cloud Vault Active.", Toast.LENGTH_SHORT).show()
                    onCompleteOnboarding()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = EvaColors.NeonCyan),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color.Transparent else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isFormValid) {
                                Brush.horizontalGradient(
                                    listOf(
                                        EvaColors.NeonCyan,
                                        EvaColors.NeonPurple
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when {
                                isCheckingQuota -> "Verifying Cloud Quota..."
                                quotaErrorMessage != null && !isQuotaEligible -> "Requires 9+ GB Free Space to Continue"
                                isFormValid -> "Enter EVA Super-Engine"
                                else -> "Sign in with Google to Continue"
                            },
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
                    text = "End-to-End Sandboxed • Zero Telemetry • GDPR & OWASP Hardened",
                    color = EvaColors.TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionBadge(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    granted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (granted) EvaColors.NeonCyan.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (granted) EvaColors.NeonCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (granted) EvaColors.NeonCyan else EvaColors.TextMuted,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = title,
            color = if (granted) Color.White else EvaColors.TextMuted,
            fontSize = 10.5.sp,
            fontWeight = if (granted) FontWeight.Bold else FontWeight.Normal
        )
    }
}
