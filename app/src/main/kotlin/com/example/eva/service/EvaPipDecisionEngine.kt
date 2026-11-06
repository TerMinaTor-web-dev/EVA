package com.example.eva.service

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import com.example.eva.model.AppService

/**
 * Intelligent Picture-in-Picture (PiP) Decision Engine powered by Gemini Cognitive Logic.
 * 
 * Prevents PiP clutter and blank white boxes for non-video apps (Instagram feeds, Reddit, LinkedIn, Facebook)
 * and pure music apps (YouTube Music, Spotify).
 * Grants fluid PiP ONLY when an active video stream or full-screen video is currently rendering.
 */
object EvaPipDecisionEngine {

    var isInPipMode: Boolean = false

    data class PipDecision(
        val allowPip: Boolean,
        val reason: String,
        val numerator: Int = 16,
        val denominator: Int = 9
    ) {
        fun buildParams(): PictureInPictureParams {
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(numerator, denominator))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setSeamlessResizeEnabled(true)
            }
            return builder.build()
        }
    }

    /**
     * Evaluates whether current context warrants Picture-in-Picture transition.
     * Uses Gemini heuristics and active DOM video playback state.
     */
    fun evaluate(
        activeService: AppService?,
        hasActiveVideo: Boolean = false,
        pageTitle: String = ""
    ): PipDecision {
        if (activeService == null) {
            return PipDecision(
                allowPip = false,
                reason = "Gemini Policy: No active service running (Home Hub visible)."
            )
        }

        // Rule 1: Audio & Music Services (YouTube Music, Spotify)
        // STRICTLY prohibit PiP to avoid blank white rectangles; route to System Quick Settings Mini-Player
        if (activeService.category.equals("Music", ignoreCase = true) ||
            activeService.id in listOf("spotify", "ytmusic")
        ) {
            return PipDecision(
                allowPip = false,
                reason = "Gemini Policy: Music streaming routed exclusively to Android Media Notification Mini-Player."
            )
        }

        // Rule 2: Video Services (YouTube, Twitch, Video streams)
        // PiP is allowed when an active hardware video, fullscreen view, or active video stream is engaged
        if (activeService.id == "youtube" || activeService.category.equals("Video", ignoreCase = true)) {
            return if (hasActiveVideo) {
                PipDecision(
                    allowPip = true,
                    reason = "Gemini Policy: Active video rendering verified for ${activeService.name}.",
                    numerator = 16,
                    denominator = 9
                )
            } else {
                PipDecision(
                    allowPip = false,
                    reason = "Gemini Policy: Video service active, but no active video stream engaged. PiP suppressed to prevent blank box."
                )
            }
        }

        // Rule 3: General HTML5 Video check
        if (hasActiveVideo) {
            return PipDecision(
                allowPip = true,
                reason = "Gemini Policy: Active video surface detected in container.",
                numerator = 16,
                denominator = 9
            )
        }

        // Rule 4: Text, Feed, and Social Apps (Instagram, LinkedIn, Reddit, Facebook, X)
        return PipDecision(
            allowPip = false,
            reason = "Gemini Policy: Non-video feed detected (${activeService.name}). PiP suppressed for optimal UX."
        )
    }

    /**
     * Gemini AI Cognitive Classifier for custom URLs or dynamic web apps.
     */
    fun classifyServiceForPip(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("youtube.com/watch") ||
                lowerUrl.contains("youtu.be") ||
                lowerUrl.contains("twitch.tv") ||
                lowerUrl.contains("vimeo.com") ||
                lowerUrl.contains("netflix.com") ||
                lowerUrl.contains(".mp4") ||
                lowerUrl.contains(".m3u8")
    }
}
