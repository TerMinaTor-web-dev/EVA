package com.example.eva

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.eva.security.SecurityGuard
import com.example.eva.ui.theme.EVATheme

class MainActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    private var securityStatus by mutableStateOf<SecurityGuard.SecurityStatus?>(null)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission granted or denied for background playback controls
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Enterprise Anti-Tapjacking and Overlay Window Blocker
        SecurityGuard.applyAntiTapjacking(this)

        // 2. Perform Environment Integrity Audit
        securityStatus = SecurityGuard.performSecurityAudit(this)

        // 3. Request Notification Permission for Android 13+ (Required for Foreground Audio Service)
        requestNotificationPermissionIfNeeded()

        setContent {
            EVATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090A10)
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Native Picture-in-Picture Trigger when user presses Home or switches apps
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {
                // Device does not support PiP or ratio was rejected
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onPause() {
        super.onPause()
        // Ensure all login tokens and session cookies are flushed to disk permanently
        try {
            android.webkit.CookieManager.getInstance().flush()
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            android.webkit.CookieManager.getInstance().flush()
        } catch (_: Exception) {}
    }
}
