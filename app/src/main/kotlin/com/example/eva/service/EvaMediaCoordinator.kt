package com.example.eva.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.eva.data.AppServiceCatalog
import com.example.eva.ui.components.EvaWebViewPool

/**
 * Enterprise Audio Focus & Multi-App Concurrency Coordinator.
 * 
 * Features:
 * - Smart Multi-App Priority: If App A (e.g. YouTube Music) is playing in background,
 *   and App B (e.g. Instagram, YouTube, Reddit) starts playing an unmuted video/audio,
 *   App A automatically pauses and yields priority to App B.
 * - Auto-Resume History: When App B pauses, finishes, or is muted, App A automatically
 *   resumes playing in the background right where it left off!
 * - Zero Thermal Waste: Relinquishes locks and coordinates pause state across all WebViews.
 */
object EvaMediaCoordinator {

    // Current service that owns audio focus and is playing
    var activeAudioServiceId: String? = null
        private set

    // Service that was playing in background and got paused to yield to a newer media stream
    var pausedBackgroundServiceId: String? = null
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Called when a media element in any WebView starts playing unmuted audio.
     */
    fun onMediaPlayStarted(
        context: Context,
        serviceId: String,
        title: String,
        artist: String,
        artworkUrl: String,
        durationMs: Long,
        positionMs: Long
    ) {
        mainHandler.post {
            val previousServiceId = activeAudioServiceId
            if (previousServiceId != null && previousServiceId != serviceId) {
                // App A was playing, App B just started: pause App A and remember it!
                pausedBackgroundServiceId = previousServiceId
                EvaWebViewPool.setPlayPauseState(previousServiceId, false)
            }

            activeAudioServiceId = serviceId

            val service = AppServiceCatalog.getServiceById(serviceId)
            val serviceName = service?.name ?: "EVA Media"
            val displayTitle = if (title.isNotBlank()) title else "Media Playing"
            val displayArtist = if (artist.isNotBlank()) artist else serviceName

            EvaMediaPlaybackService.updateMediaState(
                context = context,
                serviceId = serviceId,
                serviceName = serviceName,
                title = displayTitle,
                artist = displayArtist,
                artworkUrl = artworkUrl,
                isPlaying = true,
                durationMs = durationMs,
                positionMs = positionMs
            )
        }
    }

    /**
     * Called when a media element in any WebView pauses or finishes playing.
     */
    fun onMediaPlayPaused(
        context: Context,
        serviceId: String,
        durationMs: Long,
        positionMs: Long
    ) {
        mainHandler.post {
            if (activeAudioServiceId == serviceId) {
                val toResume = pausedBackgroundServiceId
                if (toResume != null && toResume != serviceId) {
                    // App B paused/ended! Auto-resume the previous background music (App A)!
                    pausedBackgroundServiceId = null
                    activeAudioServiceId = toResume
                    EvaWebViewPool.setPlayPauseState(toResume, true)

                    val resumedService = AppServiceCatalog.getServiceById(toResume)
                    EvaMediaPlaybackService.updateMediaState(
                        context = context,
                        serviceId = toResume,
                        serviceName = resumedService?.name ?: "EVA Media",
                        title = "Resuming playback...",
                        artist = resumedService?.name ?: "",
                        artworkUrl = "",
                        isPlaying = true,
                        durationMs = 0L,
                        positionMs = 0L
                    )
                } else {
                    // No background app waiting to resume: mark paused
                    activeAudioServiceId = null
                    EvaMediaPlaybackService.setPlayingState(context, false, positionMs, durationMs)
                }
            }
        }
    }

    /**
     * Periodic/throttled position update from HTMLMediaElement.timeupdate
     */
    fun onPlaybackPosition(
        context: Context,
        serviceId: String,
        durationMs: Long,
        positionMs: Long
    ) {
        if (activeAudioServiceId == serviceId) {
            mainHandler.post {
                EvaMediaPlaybackService.updatePosition(context, positionMs, durationMs)
            }
        }
    }

    /**
     * Called when user explicitly taps Pause on the Notification Mini-Player or app controls.
     */
    fun onUserExplicitPause(context: Context) {
        pausedBackgroundServiceId = null
        activeAudioServiceId = null
        EvaMediaPlaybackService.setPlayingState(context, false)
    }
}
