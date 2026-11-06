package com.example.eva.diagnostics

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Real-time performance diagnostics and telemetry engine for EVA Web Experience 3.0.
 *
 * Measures actual operational metrics without collecting user data:
 * - Chromium WebView initialization latency
 * - Page load durations and navigation timings
 * - Memory allocation and cache utilization
 * - Renderer process crash recoveries
 * - Network and page load failure counters
 */
object EvaDiagnosticsManager {

    // Counters and state variables
    var activeWebViewCount by mutableIntStateOf(0)
        internal set

    fun updateActiveWebViewCount(count: Int) {
        activeWebViewCount = count
    }

    var totalRendererCrashesRecovered by mutableIntStateOf(0)
        internal set

    var totalPageLoadFailures by mutableIntStateOf(0)
        internal set

    var totalNetworkFailures by mutableIntStateOf(0)
        internal set

    var totalDownloadsCompleted by mutableIntStateOf(0)
        internal set

    var lastInitDurationMs by mutableLongStateOf(0L)
        internal set

    var lastPageLoadDurationMs by mutableLongStateOf(0L)
        internal set

    var lastServiceSwitchDurationMs by mutableLongStateOf(0L)
        internal set

    var activeServiceName by mutableStateOf("None")
        internal set

    // Service-specific page load tracking (serviceId -> start Timestamp)
    private val ongoingNavigations = mutableMapOf<String, Long>()

    // Service-specific average load duration in milliseconds
    val serviceAverageLoadTimeMs = mutableStateMapOf<String, Long>()

    fun recordWebViewInitStart(): Long {
        return SystemClock.elapsedRealtime()
    }

    fun recordWebViewInitEnd(startTimeMs: Long) {
        val duration = SystemClock.elapsedRealtime() - startTimeMs
        lastInitDurationMs = duration
    }

    fun recordPageLoadStarted(serviceId: String, serviceName: String) {
        activeServiceName = serviceName
        ongoingNavigations[serviceId] = SystemClock.elapsedRealtime()
    }

    fun recordPageLoadFinished(serviceId: String) {
        val startTime = ongoingNavigations.remove(serviceId) ?: return
        val duration = SystemClock.elapsedRealtime() - startTime
        lastPageLoadDurationMs = duration

        val previous = serviceAverageLoadTimeMs[serviceId]
        if (previous == null) {
            serviceAverageLoadTimeMs[serviceId] = duration
        } else {
            serviceAverageLoadTimeMs[serviceId] = (previous + duration) / 2
        }
    }

    fun recordPageLoadError(serviceId: String, errorCode: Int, description: String) {
        ongoingNavigations.remove(serviceId)
        totalPageLoadFailures++
        if (errorCode == -2 /* ERROR_HOST_LOOKUP */ || errorCode == -6 /* ERROR_CONNECT */) {
            totalNetworkFailures++
        }
    }

    fun recordRendererCrashRecovered(serviceId: String) {
        totalRendererCrashesRecovered++
    }

    fun recordServiceSwitchStart(): Long {
        return SystemClock.elapsedRealtime()
    }

    fun recordServiceSwitchEnd(startTimeMs: Long) {
        lastServiceSwitchDurationMs = SystemClock.elapsedRealtime() - startTimeMs
    }

    fun recordDownloadCompleted() {
        totalDownloadsCompleted++
    }

    data class MemoryDiagnostics(
        val usedHeapMb: Float,
        val freeHeapMb: Float,
        val totalHeapMb: Float,
        val maxHeapMb: Float,
        val cacheDirSizeMb: Float
    )

    fun getMemoryDiagnostics(context: Context): MemoryDiagnostics {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        var cacheSizeBytes = 0L
        try {
            cacheSizeBytes = getFolderSize(context.cacheDir)
        } catch (_: Exception) {}

        val mb = 1024f * 1024f
        return MemoryDiagnostics(
            usedHeapMb = usedMemory / mb,
            freeHeapMb = freeMemory / mb,
            totalHeapMb = totalMemory / mb,
            maxHeapMb = maxMemory / mb,
            cacheDirSizeMb = cacheSizeBytes / mb
        )
    }

    private fun getFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }
}
