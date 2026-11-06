package com.example.eva.data

import com.example.eva.model.AppService
import com.example.eva.model.ServiceWebProfile
import com.example.eva.model.UserAgentType
import java.net.URI

/**
 * Central registry of hardened, service-specific web profiles.
 * Enforces least-privilege hardware permissions and origin boundaries.
 */
object ServiceWebProfileRegistry {

    private val profiles = mapOf(
        "instagram" to ServiceWebProfile(
            id = "instagram",
            name = "Instagram",
            baseUrl = "https://www.instagram.com",
            allowedOrigins = setOf(
                "instagram.com",
                "*.instagram.com",
                "cdninstagram.com",
                "*.cdninstagram.com",
                "facebook.com",
                "*.facebook.com",
                "fbcdn.net",
                "*.fbcdn.net"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = true,
            allowMicrophone = true,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = false,
            isAudioService = false
        ),
        "youtube" to ServiceWebProfile(
            id = "youtube",
            name = "YouTube",
            baseUrl = "https://m.youtube.com",
            allowedOrigins = setOf(
                "youtube.com",
                "*.youtube.com",
                "m.youtube.com",
                "youtu.be",
                "accounts.google.com",
                "googlevideo.com",
                "*.googlevideo.com",
                "gstatic.com",
                "*.gstatic.com",
                "google.com",
                "*.google.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = false,
            allowMicrophone = true, // Voice search
            allowDownloads = true,
            allowPopups = false,
            isVideoService = true,
            isAudioService = false
        ),
        "ytmusic" to ServiceWebProfile(
            id = "ytmusic",
            name = "YT Music",
            baseUrl = "https://music.youtube.com",
            allowedOrigins = setOf(
                "music.youtube.com",
                "youtube.com",
                "*.youtube.com",
                "accounts.google.com",
                "googlevideo.com",
                "*.googlevideo.com",
                "gstatic.com",
                "*.gstatic.com",
                "google.com",
                "*.google.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = false,
            allowMicrophone = true,
            allowDownloads = false,
            allowPopups = false,
            isVideoService = true,
            isAudioService = true
        ),
        "spotify" to ServiceWebProfile(
            id = "spotify",
            name = "Spotify",
            baseUrl = "https://open.spotify.com",
            allowedOrigins = setOf(
                "spotify.com",
                "*.spotify.com",
                "open.spotify.com",
                "accounts.spotify.com",
                "scdn.co",
                "*.scdn.co",
                "spotifycdn.com",
                "*.spotifycdn.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = false,
            allowMicrophone = false,
            allowDownloads = false,
            allowPopups = false,
            isVideoService = false,
            isAudioService = true
        ),
        "linkedin" to ServiceWebProfile(
            id = "linkedin",
            name = "LinkedIn",
            baseUrl = "https://www.linkedin.com",
            allowedOrigins = setOf(
                "linkedin.com",
                "*.linkedin.com",
                "licdn.com",
                "*.licdn.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = true,
            allowMicrophone = false,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = false,
            isAudioService = false
        ),
        "facebook" to ServiceWebProfile(
            id = "facebook",
            name = "Facebook",
            baseUrl = "https://m.facebook.com",
            allowedOrigins = setOf(
                "facebook.com",
                "*.facebook.com",
                "m.facebook.com",
                "fbcdn.net",
                "*.fbcdn.net",
                "messenger.com",
                "*.messenger.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = true,
            allowMicrophone = true,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = false,
            isAudioService = false
        ),
        "twitter" to ServiceWebProfile(
            id = "twitter",
            name = "X (Twitter)",
            baseUrl = "https://x.com",
            allowedOrigins = setOf(
                "x.com",
                "*.x.com",
                "twitter.com",
                "*.twitter.com",
                "twimg.com",
                "*.twimg.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = true,
            allowMicrophone = true,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = false,
            isAudioService = false
        ),
        "reddit" to ServiceWebProfile(
            id = "reddit",
            name = "Reddit",
            baseUrl = "https://www.reddit.com",
            allowedOrigins = setOf(
                "reddit.com",
                "*.reddit.com",
                "redd.it",
                "*.redd.it",
                "redditstatic.com",
                "*.redditstatic.com"
            ),
            userAgentType = UserAgentType.CHROME_MOBILE,
            allowCamera = false,
            allowMicrophone = false,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = false,
            isAudioService = false
        ),
        "whatsapp" to ServiceWebProfile(
            id = "whatsapp",
            name = "WhatsApp Web",
            baseUrl = "https://web.whatsapp.com",
            allowedOrigins = setOf(
                "whatsapp.com",
                "*.whatsapp.com",
                "web.whatsapp.com",
                "whatsapp.net",
                "*.whatsapp.net"
            ),
            userAgentType = UserAgentType.DESKTOP_CHROME,
            allowCamera = true,
            allowMicrophone = true,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = false,
            isAudioService = false
        )
    )

    fun getProfile(serviceId: String): ServiceWebProfile? {
        return profiles[serviceId.lowercase()]
    }

    fun getProfile(serviceId: String, fallbackUrl: String): ServiceWebProfile {
        val existing = profiles[serviceId.lowercase()]
        if (existing != null) return existing
        return getProfileForService(
            AppService(
                id = serviceId,
                name = serviceId.replaceFirstChar { it.uppercase() },
                url = if (fallbackUrl.isNotBlank()) fallbackUrl else "https://$serviceId.com",
                category = "Web",
                primaryColor = androidx.compose.ui.graphics.Color(0xFF6C5CE7),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFA29BFE)
            )
        )
    }

    /**
     * Resolves a profile for any AppService, deriving an origin allowlist from its URL if custom.
     */
    fun getProfileForService(service: AppService): ServiceWebProfile {
        val existing = profiles[service.id.lowercase()]
        if (existing != null) return existing

        val host = try {
            URI(service.url).host ?: service.url
        } catch (_: Exception) {
            "google.com"
        }

        return ServiceWebProfile(
            id = service.id,
            name = service.name,
            baseUrl = service.url,
            allowedOrigins = setOf(host, "*.$host"),
            userAgentType = if (service.isDesktopUserAgent) UserAgentType.DESKTOP_CHROME else UserAgentType.CHROME_MOBILE,
            allowJavaScript = true,
            allowDomStorage = true,
            allowThirdPartyCookies = true,
            allowCamera = false,
            allowMicrophone = false,
            allowDownloads = true,
            allowPopups = false,
            isVideoService = service.category.equals("Video", ignoreCase = true),
            isAudioService = service.category.equals("Music", ignoreCase = true) || service.category.equals("Audio", ignoreCase = true)
        )
    }
}
