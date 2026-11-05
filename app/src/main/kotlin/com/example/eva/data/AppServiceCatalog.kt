package com.example.eva.data

import androidx.compose.ui.graphics.Color
import com.example.eva.model.AppService

object AppServiceCatalog {
    private val customServices = mutableMapOf<String, AppService>()

    val services = listOf(
        AppService(
            id = "instagram",
            name = "Instagram",
            url = "https://www.instagram.com",
            category = "Social",
            primaryColor = Color(0xFFE1306C),
            secondaryColor = Color(0xFFF77737),
            description = "Photos, Reels & Direct Messages"
        ),
        AppService(
            id = "youtube",
            name = "YouTube",
            url = "https://m.youtube.com",
            category = "Video",
            primaryColor = Color(0xFFFF0000),
            secondaryColor = Color(0xFF990000),
            description = "Watch Videos, Shorts & Subscriptions"
        ),
        AppService(
            id = "ytmusic",
            name = "YT Music",
            url = "https://music.youtube.com",
            category = "Music",
            primaryColor = Color(0xFFFF0000),
            secondaryColor = Color(0xFF232323),
            description = "Stream Songs, Albums & Playlists"
        ),
        AppService(
            id = "spotify",
            name = "Spotify",
            url = "https://open.spotify.com",
            category = "Music",
            primaryColor = Color(0xFF1DB954),
            secondaryColor = Color(0xFF191414),
            description = "Music, Podcasts & Playlists"
        ),
        AppService(
            id = "linkedin",
            name = "LinkedIn",
            url = "https://www.linkedin.com",
            category = "Professional",
            primaryColor = Color(0xFF0A66C2),
            secondaryColor = Color(0xFF004182),
            description = "Jobs, Network & Professional Updates"
        ),
        AppService(
            id = "facebook",
            name = "Facebook",
            url = "https://m.facebook.com",
            category = "Social",
            primaryColor = Color(0xFF1877F2),
            secondaryColor = Color(0xFF0E52AB),
            description = "Connect with Friends & Communities"
        ),
        AppService(
            id = "twitter",
            name = "X (Twitter)",
            url = "https://x.com",
            category = "Social",
            primaryColor = Color(0xFFE7E9EA),
            secondaryColor = Color(0xFF1D9BF0),
            description = "Real-time News & Trending Posts"
        ),
        AppService(
            id = "reddit",
            name = "Reddit",
            url = "https://www.reddit.com",
            category = "Community",
            primaryColor = Color(0xFFFF4500),
            secondaryColor = Color(0xFFCC3700),
            description = "Communities, Discussions & Topics"
        )
    )

    fun registerService(service: AppService) {
        customServices[service.id] = service
    }

    fun getById(id: String): AppService {
        return customServices[id] ?: services.firstOrNull { it.id == id } ?: services.first()
    }

    fun getServiceById(id: String): AppService? {
        return customServices[id] ?: services.firstOrNull { it.id == id }
    }
}
