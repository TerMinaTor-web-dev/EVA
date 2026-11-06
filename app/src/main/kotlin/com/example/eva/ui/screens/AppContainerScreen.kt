package com.example.eva.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.eva.model.AppService
import com.example.eva.service.EvaPipDecisionEngine
import com.example.eva.ui.components.EvaAiSheet
import com.example.eva.ui.components.EvaWebViewPool
import com.example.eva.ui.components.FloatingPillDock
import com.example.eva.ui.components.MultiTabWebViewContainer
import com.example.eva.ui.theme.EvaColors

@Composable
fun AppContainerScreen(
    initialService: AppService,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Chronological Recent Apps History Stack (LRU)
    var recentHistory by remember(initialService) { mutableStateOf(listOf(initialService)) }
    var historyIndex by remember(initialService) { mutableIntStateOf(0) }

    var activeService by remember(initialService) { mutableStateOf(initialService) }
    var currentTitle by remember { mutableStateOf(initialService.name) }
    var currentUrl by remember { mutableStateOf(initialService.url) }
    var showAiAssistant by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
    var isOnline by remember { mutableStateOf(true) }

    DisposableEffect(connectivityManager) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }
            override fun onLost(network: Network) {
                isOnline = false
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback)
        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {}
        }
    }

    // Native Fullscreen Video Player State (e.g. YouTube 4K / Landscape Fullscreen)
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun enterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        (view.parent as? ViewGroup)?.removeView(view)
        customFullscreenView = view
        customViewCallback = callback
        EvaWebViewPool.isFullscreenVideoActive = true

        activity?.let { act ->
            // Auto-switch to 16:9 Landscape Video Theater Mode
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            act.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Bezel-to-bezel Immersive Sticky Mode (hide status & navigation bars)
            val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun exitFullscreen() {
        (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
        customViewCallback?.onCustomViewHidden()
        customFullscreenView = null
        customViewCallback = null
        EvaWebViewPool.isFullscreenVideoActive = false

        activity?.let { act ->
            // Restore normal screen orientation & clear keep-screen-on
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            act.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Restore status & navigation bars
            val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (EvaWebViewPool.isFullscreenVideoActive) {
                exitFullscreen()
            }
        }
    }

    // Hardware back navigation for Fullscreen Video
    BackHandler(enabled = customFullscreenView != null) {
        exitFullscreen()
    }

    // Direct app selection from taskbar or omnibar
    fun selectService(service: AppService) {
        if (service.id != activeService.id) {
            val updated = recentHistory.take(historyIndex + 1).toMutableList()
            updated.add(service)
            recentHistory = updated
            historyIndex = updated.lastIndex
            activeService = service
            currentTitle = service.name
            currentUrl = service.url
        }
    }

    // Swipe Right -> Backward into Recents (e.g. FB -> Insta -> YouTube)
    fun swipeRecentBackward(): String? {
        return if (historyIndex > 0) {
            historyIndex--
            val prev = recentHistory[historyIndex]
            activeService = prev
            currentTitle = prev.name
            currentUrl = prev.url
            prev.name
        } else {
            null
        }
    }

    // Swipe Left -> Reverse Forward in History (e.g. YouTube -> Insta -> FB)
    fun swipeRecentForward(): String? {
        return if (historyIndex < recentHistory.size - 1) {
            historyIndex++
            val next = recentHistory[historyIndex]
            activeService = next
            currentTitle = next.name
            currentUrl = next.url
            next.name
        } else {
            null
        }
    }

    if (EvaPipDecisionEngine.isInPipMode) {
        // Pure Unobstructed PiP Video View: No bars, no dock, 100% video surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MultiTabWebViewContainer(
                activeService = activeService,
                modifier = Modifier.fillMaxSize(),
                onPageTitleChanged = { currentTitle = it },
                onUrlChanged = { currentUrl = it }
            )
        }
    } else if (customFullscreenView != null) {
        // Fullscreen Custom View (100% immersive video surface)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = {
                    val view = customFullscreenView!!
                    (view.parent as? ViewGroup)?.removeView(view)
                    view
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090A10))
                .statusBarsPadding()
        ) {
            // Real-time Cybernetic Offline Notice Banner
            AnimatedVisibility(
                visible = !isOnline,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF8B0000).copy(alpha = 0.9f))
                        .clickable { EvaWebViewPool.reloadCurrent(activeService.id) }
                        .padding(vertical = 5.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ Connection offline • Tap to reload",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. Phone Ratio Canvas for Active App (100% unobstructed, zero overlay interference)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                MultiTabWebViewContainer(
                    activeService = activeService,
                    modifier = Modifier.fillMaxSize(),
                    onPageTitleChanged = { currentTitle = it },
                    onUrlChanged = { currentUrl = it },
                    onShowCustomView = { view, callback ->
                        enterFullscreen(view, callback)
                    },
                    onHideCustomView = {
                        exitFullscreen()
                    }
                )
            }

            // 2. Dedicated Non-Overlay Bottom Bar Area for the Gesture Pill Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0C14))
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                FloatingPillDock(
                    currentService = activeService,
                    onSelectService = ::selectService,
                    onSwipeRecentBackward = ::swipeRecentBackward,
                    onSwipeRecentForward = ::swipeRecentForward,
                    onNavigateHome = onNavigateHome,
                    onReloadCurrent = {
                        EvaWebViewPool.reloadCurrent(activeService.id)
                    },
                    onOpenAiAssistant = { showAiAssistant = true },
                    canGoBack = EvaWebViewPool.canGoBack(activeService.id),
                    onGoBack = { EvaWebViewPool.goBack(activeService.id) },
                    canGoForward = EvaWebViewPool.canGoForward(activeService.id),
                    onGoForward = { EvaWebViewPool.goForward(activeService.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // EVA AI Co-Pilot Bottom Sheet
    if (showAiAssistant) {
        EvaAiSheet(
            currentPageTitle = currentTitle,
            currentUrl = currentUrl,
            onDismiss = { showAiAssistant = false }
        )
    }
}
