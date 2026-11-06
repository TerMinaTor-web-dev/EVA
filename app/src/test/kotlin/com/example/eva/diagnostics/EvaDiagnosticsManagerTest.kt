package com.example.eva.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaDiagnosticsManagerTest {

    @Test
    fun testActiveWebViewCountTracking() {
        EvaDiagnosticsManager.updateActiveWebViewCount(3)
        assertEquals(3, EvaDiagnosticsManager.activeWebViewCount)

        EvaDiagnosticsManager.updateActiveWebViewCount(1)
        assertEquals(1, EvaDiagnosticsManager.activeWebViewCount)
    }

    @Test
    fun testRendererCrashRecoveryTracking() {
        val initial = EvaDiagnosticsManager.totalRendererCrashesRecovered
        EvaDiagnosticsManager.recordRendererCrashRecovered("youtube")
        assertEquals(initial + 1, EvaDiagnosticsManager.totalRendererCrashesRecovered)
    }

    @Test
    fun testPageLoadErrorTracking() {
        val initialFailures = EvaDiagnosticsManager.totalPageLoadFailures
        val initialNetFailures = EvaDiagnosticsManager.totalNetworkFailures

        // Non-network error
        EvaDiagnosticsManager.recordPageLoadError("instagram", -1, "Generic error")
        assertEquals(initialFailures + 1, EvaDiagnosticsManager.totalPageLoadFailures)
        assertEquals(initialNetFailures, EvaDiagnosticsManager.totalNetworkFailures)

        // Network DNS error (-2)
        EvaDiagnosticsManager.recordPageLoadError("reddit", -2, "Host lookup failed")
        assertEquals(initialFailures + 2, EvaDiagnosticsManager.totalPageLoadFailures)
        assertEquals(initialNetFailures + 1, EvaDiagnosticsManager.totalNetworkFailures)
    }

    @Test
    fun testDownloadCompletedTracking() {
        val initial = EvaDiagnosticsManager.totalDownloadsCompleted
        EvaDiagnosticsManager.recordDownloadCompleted()
        assertEquals(initial + 1, EvaDiagnosticsManager.totalDownloadsCompleted)
    }
}
