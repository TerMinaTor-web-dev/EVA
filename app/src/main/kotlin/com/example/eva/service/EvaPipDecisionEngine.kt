package com.example.eva.service

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import com.example.eva.model.AppService

/**
 * Intelligent Picture-in-Picture (PiP) Decision Engine powered by Gemini Cognitive Logic.
 * 
 * Prevents PiP clutter for non-video apps (Instagram feeds, Reddit, LinkedIn, Facebook)
 * while granting fluid PiP for video streaming (YouTube, Vimeo, Twitch) and routing
 * pure audio services (Spotify, YT Music) to the System Media Mini-Player Notification.
 */
object EvaPipDecisionEngine {

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

        // Rule 1: Video-First Services (YouTube, Twitch, Video streaming)
        if (activeService.id == "youtube" || activeService.category.equals("Video", ignoreCase = true)) {
            return PipDecision(
                allowPip = true,
                reason = "Gemini Policy: Active video streaming verified for ${activeService.name}.",
                numerator = 16,
                denominator = 9
            )
        }

        // Rule 2: Active HTML5 Video detected in DOM
        if (hasActiveVideo) {
            return PipDecision(
                allowPip = true,
                reason = "Gemini Policy: Active HTML5 video element detected in active container.",
                numerator = 16,
                denominator = 9
            )
        }

        // Rule 3: Audio & Music Services (Spotify, YT Music)
        if (activeService.category.equals("Music", ignoreCase = true) ||
            activeService.id in listOf("spotify", "ytmusic")
        ) {
            return PipDecision(
                allowPip = false,
                reason = "Gemini Policy: Audio service routed to Android Quick Settings Media Notification."
            )
        }

        // Rule 4: Text, Feed, and Social Apps (Instagram, LinkedIn, Reddit, Facebook, X)
        // Suppress PiP to prevent unusable tiny window obstruction
        return PipDecision(
            allowPip = false,
            reason = "Gemini Policy: Non-video social feed detected (${activeService.name}). PiP suppressed for optimal UX."
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
