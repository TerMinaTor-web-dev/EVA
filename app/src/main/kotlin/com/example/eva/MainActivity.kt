package com.example.eva

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
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
import com.example.eva.data.AppServiceCatalog
import com.example.eva.security.EvaPermissionManager
import com.example.eva.security.SecurityGuard
import com.example.eva.service.EvaMediaCoordinator
import com.example.eva.service.EvaPipDecisionEngine
import com.example.eva.ui.components.EvaWebViewPool
import com.example.eva.ui.theme.EVATheme

class MainActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    private var securityStatus by mutableStateOf<SecurityGuard.SecurityStatus?>(null)

    private val multiPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Permissions granted / updated by user
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Enterprise Anti-Tapjacking and Overlay Window Blocker
        SecurityGuard.applyAntiTapjacking(this)

        // 2. Perform Environment Integrity Audit
        securityStatus = SecurityGuard.performSecurityAudit(this)

        // 3. Proactively audit core permissions on first launch
        if (!EvaPermissionManager.hasCorePermissions(this)) {
            multiPermissionLauncher.launch(EvaPermissionManager.getRequiredPermissions())
        }

        // 4. Adaptive High Refresh Rate: Unlock up to 120 FPS smoothly based on device hardware support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
                try {
                    val modes = display?.supportedModes ?: emptyArray()
                    val maxRefreshMode = modes.maxByOrNull { it.refreshRate }
                    if (maxRefreshMode != null && maxRefreshMode.refreshRate > 60f) {
                        val params = window.attributes
                        params.preferredDisplayModeId = maxRefreshMode.modeId
                        window.attributes = params
                    }
                } catch (_: Exception) {}
            }
        }

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

    /**
     * Context-Aware Picture-in-Picture Trigger powered by EvaPipDecisionEngine.
     * Selectively enters PiP ONLY when active full-screen video is rendering.
     * Audio apps (YouTube Music, Spotify) and social feeds (Instagram, Reddit, LinkedIn)
     * are suppressed from PiP to prevent blank white box overlays on the user's home screen.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentService = AppServiceCatalog.getServiceById(EvaWebViewPool.activeServiceId)
            val isPlayingVideo = EvaWebViewPool.isFullscreenVideoActive ||
                    (EvaMediaCoordinator.activeAudioServiceId == currentService?.id)
            val decision = EvaPipDecisionEngine.evaluate(
                activeService = currentService,
                hasActiveVideo = isPlayingVideo
            )

            if (decision.allowPip) {
                try {
                    enterPictureInPictureMode(decision.buildParams())
                } catch (_: Exception) {
                    // Device rejected PiP params or ratio
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        EvaPipDecisionEngine.isInPipMode = isInPictureInPictureMode
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
