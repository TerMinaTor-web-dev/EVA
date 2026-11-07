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
        ),
        AppService(
            id = "flipkart",
            name = "Flipkart",
            url = "https://www.flipkart.com",
            category = "Shopping",
            primaryColor = Color(0xFF2874F0),
            secondaryColor = Color(0xFFFFC200),
            description = "Online Shopping, Deals, Mobiles & SuperCoins"
        ),
        AppService(
            id = "amazon",
            name = "Amazon",
            url = "https://www.amazon.in",
            category = "Shopping",
            primaryColor = Color(0xFFFF9900),
            secondaryColor = Color(0xFF232F3E),
            description = "Prime Fast Delivery, Electronics & Amazon Pay"
        ),
        AppService(
            id = "myntra",
            name = "Myntra",
            url = "https://www.myntra.com",
            category = "Fashion",
            primaryColor = Color(0xFFFF3F6C),
            secondaryColor = Color(0xFFF16521),
            description = "Trendsetting Fashion, Beauty & Lifestyle Store"
        ),
        AppService(
            id = "instamart",
            name = "Instamart",
            url = "https://www.swiggy.com/instamart",
            category = "Quick Commerce",
            primaryColor = Color(0xFFFC8019),
            secondaryColor = Color(0xFF6B3CE9),
            description = "Instant Groceries & Essentials Delivered in 10 Mins"
        ),
        AppService(
            id = "blinkit",
            name = "Blinkit",
            url = "https://blinkit.com",
            category = "Quick Commerce",
            primaryColor = Color(0xFFF7C325),
            secondaryColor = Color(0xFF0C831F),
            description = "Instant Groceries Delivered to Doorstep in 10 Mins"
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
