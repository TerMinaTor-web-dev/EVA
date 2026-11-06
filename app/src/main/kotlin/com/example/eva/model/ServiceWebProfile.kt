package com.example.eva.model

import java.net.URI

enum class UserAgentType {
    CHROME_MOBILE,
    DESKTOP_CHROME
}

/**
 * Immutable per-service web profile defining security boundaries,
 * origin allowlists, device hardware permissions, and user-agent policy.
 */
data class ServiceWebProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val allowedOrigins: Set<String>,
    val userAgentType: UserAgentType = UserAgentType.CHROME_MOBILE,
    val allowJavaScript: Boolean = true,
    val allowDomStorage: Boolean = true,
    val allowCookies: Boolean = true,
    val allowThirdPartyCookies: Boolean = true,
    val allowCamera: Boolean = false,
    val allowMicrophone: Boolean = false,
    val allowDownloads: Boolean = true,
    val allowFileUploads: Boolean = true,
    val allowPopups: Boolean = false,
    val mediaPlaybackRequiresUserGesture: Boolean = false,
    val isVideoService: Boolean = false,
    val isAudioService: Boolean = false,
    val autoDismissNagBanners: Boolean = true
) {
    val serviceId: String get() = id

    /**
     * Resolves the appropriate User-Agent string based on this profile's userAgentType.
     */
    fun resolveUserAgent(mobileUa: String, desktopUa: String): String {
        return if (userAgentType == UserAgentType.DESKTOP_CHROME) desktopUa else mobileUa
    }

    /**
     * Checks if a given URL host is permitted under this service's origin policy.
     */
    fun isOriginAllowed(rawUrlOrHost: String?): Boolean {
        if (rawUrlOrHost.isNullOrBlank()) return false
        val cleanHost = try {
            val uri = URI(rawUrlOrHost)
            uri.host ?: rawUrlOrHost
        } catch (_: Exception) {
            rawUrlOrHost
        }.lowercase()

        return allowedOrigins.any { allowed ->
            if (allowed.startsWith("*.")) {
                val base = allowed.removePrefix("*.")
                cleanHost == base || cleanHost.endsWith(".$base")
            } else {
                cleanHost == allowed || cleanHost.endsWith(".$allowed")
            }
        }
    }
}
