package com.example.eva.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.eva.model.AppService
import com.example.eva.ui.components.EvaAiSheet
import com.example.eva.ui.components.EvaWebViewPool
import com.example.eva.ui.components.FloatingPillDock
import com.example.eva.ui.components.MultiTabWebViewContainer

@Composable
fun AppContainerScreen(
    initialService: AppService,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeService by remember(initialService) { mutableStateOf(initialService) }
    var currentTitle by remember { mutableStateOf(initialService.name) }
    var currentUrl by remember { mutableStateOf(initialService.url) }
    var showAiAssistant by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Multi-Tab In-Memory WebView Pool (Zero reload, zero latency)
        MultiTabWebViewContainer(
            activeService = activeService,
            modifier = Modifier.fillMaxSize(),
            onPageTitleChanged = { currentTitle = it },
            onUrlChanged = { currentUrl = it }
        )

        // Micro Floating Gesture Pill Bar & Expanding Taskbar
        FloatingPillDock(
            currentService = activeService,
            onSelectService = { selectedService ->
                activeService = selectedService
                currentTitle = selectedService.name
                currentUrl = selectedService.url
            },
            onNavigateHome = onNavigateHome,
            onReloadCurrent = {
                EvaWebViewPool.reloadCurrent(activeService.id)
            },
            onOpenAiAssistant = { showAiAssistant = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // EVA AI Co-Pilot Bottom Sheet
        if (showAiAssistant) {
            EvaAiSheet(
                currentPageTitle = currentTitle,
                currentUrl = currentUrl,
                onDismiss = { showAiAssistant = false }
            )
        }
    }
}
