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
 * - Real-Time Title & Artist Resolution: Extracts track names and artists natively from
 *   document title changes and HTML5 metadata.
 * - Zero Thermal Waste: Relinquishes locks and coordinates pause state across all WebViews.
 */
object EvaMediaCoordinator {

    // Current service that owns audio focus and is playing
    var activeAudioServiceId: String? = null
        private set

    // Service that was playing in background and got paused to yield to a newer media stream
    var pausedBackgroundServiceId: String? = null
        private set

    // Explicit track title from DOM/MediaSession (prevents broad webpage title from overwriting it)
    private var currentExplicitTrackTitle: String? = null

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
            if (title.isNotBlank() && title != "Media Playing") {
                currentExplicitTrackTitle = title
            }

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
     * Called by EvaMediaBridge when real-time track poller or mediaSession detects new track/artwork.
     */
    fun onTrackChanged(
        context: Context,
        serviceId: String,
        serviceName: String,
        artist: String,
        title: String,
        artworkUrl: String
    ) {
        currentExplicitTrackTitle = title
        activeAudioServiceId = serviceId

        mainHandler.post {
            EvaMediaPlaybackService.updateTrack(
                context = context,
                serviceId = serviceId,
                serviceName = serviceName,
                artist = artist,
                title = title,
                artworkUrl = artworkUrl
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
                currentExplicitTrackTitle = null
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
     * Called by native Android WebChromeClient when the document/window title updates.
     * Extracts exact song title and artist from all standard web player titles.
     */
    fun onDocumentTitleChanged(
        context: Context,
        serviceId: String,
        rawTitle: String
    ) {
        // If an explicit track title (e.g. "12 Saal") is already active for this service,
        // ignore broader webpage/album title changes
        if (activeAudioServiceId == serviceId && !currentExplicitTrackTitle.isNullOrBlank()) {
            return
        }

        val clean = rawTitle.trim()
        if (clean.isBlank()) return

        var title = clean
        var artist = ""

        // Strip web service title watermarks
        val suffixes = listOf(
            " - YouTube Music",
            " | Spotify",
            " - Spotify",
            " - YouTube",
            " • Spotify",
            " / X",
            " | LinkedIn",
            " - Reddit",
            " | Facebook"
        )
        for (suffix in suffixes) {
            if (title.endsWith(suffix, ignoreCase = true)) {
                title = title.dropLast(suffix.length).trim()
            }
        }

        // Parse "Title - Artist" or "Title • Artist"
        if (title.contains(" • ")) {
            val parts = title.split(" • ", limit = 2)
            title = parts[0].trim()
            artist = parts.getOrNull(1)?.trim() ?: ""
        } else if (title.contains(" - ")) {
            val parts = title.split(" - ", limit = 2)
            title = parts[0].trim()
            artist = parts.getOrNull(1)?.trim() ?: ""
        }

        val service = AppServiceCatalog.getServiceById(serviceId)
        val serviceName = service?.name ?: "EVA Media"
        val displayArtist = if (artist.isNotBlank()) artist else serviceName

        // Don't update if it's just the default app name like "YouTube Music" or "Spotify"
        if (title.isNotBlank() &&
            !title.equals("YouTube Music", ignoreCase = true) &&
            !title.equals("Spotify", ignoreCase = true) &&
            !title.equals("YouTube", ignoreCase = true)
        ) {
            mainHandler.post {
                EvaMediaPlaybackService.updateTrack(
                    context = context,
                    serviceId = serviceId,
                    serviceName = serviceName,
                    artist = displayArtist,
                    title = title
                )
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
        currentExplicitTrackTitle = null
        pausedBackgroundServiceId = null
        activeAudioServiceId = null
        EvaMediaPlaybackService.setPlayingState(context, false)
    }
}
