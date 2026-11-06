package com.example.eva.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Google Play Store & Android App Ecosystem Bridge for EVA Super-App.
 *
 * Allows users to:
 * 1. Link active Google Identity with the device Google Play Store.
 * 2. Manage updates and deep-link directly into official Play Store listings.
 * 3. Calculate hardware flash storage saved by virtualizing heavy native apps inside EVA.
 * 4. Check whether native APKs exist on device and provide quick launch or cloud sandbox options.
 */
object EvaPlayStoreBridgeManager {

    const val PLAY_STORE_PACKAGE = "com.android.vending"

    /**
     * Map of catalog service IDs to official Android Package IDs.
     */
    val SERVICE_PACKAGE_MAP = mapOf(
        "instagram" to "com.instagram.android",
        "youtube" to "com.google.android.youtube",
        "ytmusic" to "com.google.android.apps.youtube.music",
        "spotify" to "com.spotify.music",
        "linkedin" to "com.linkedin.android",
        "facebook" to "com.facebook.katana",
        "twitter" to "com.twitter.android",
        "reddit" to "com.reddit.frontpage",
        "whatsapp" to "com.whatsapp"
    )

    /**
     * Average disk footprint (APK + initial data cache) of the native Android application.
     */
    val NATIVE_APP_WEIGHT_MB = mapOf(
        "instagram" to 280,
        "youtube" to 320,
        "ytmusic" to 210,
        "spotify" to 260,
        "linkedin" to 180,
        "facebook" to 350,
        "twitter" to 240,
        "reddit" to 190,
        "whatsapp" to 210
    )

    data class ServiceEcosystemStatus(
        val serviceId: String,
        val packageName: String,
        val isInstalledOnDevice: Boolean,
        val nativeWeightMb: Int,
        val isManagedByEva: Boolean = true
    )

    fun getPackageNameForService(serviceId: String): String? {
        return SERVICE_PACKAGE_MAP[serviceId.lowercase()]
    }

    fun getNativeAppWeightMb(serviceId: String): Int {
        return NATIVE_APP_WEIGHT_MB[serviceId.lowercase()] ?: 150
    }

    fun getTotalEstimatedNativeWeightMb(): Int {
        return NATIVE_APP_WEIGHT_MB.values.sum()
    }

    fun isPlayStoreInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PLAY_STORE_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getServiceEcosystemStatus(context: Context, serviceId: String): ServiceEcosystemStatus {
        val pkg = getPackageNameForService(serviceId) ?: "com.$serviceId.android"
        val installed = isPackageInstalled(context, pkg)
        val weight = getNativeAppWeightMb(serviceId)
        return ServiceEcosystemStatus(
            serviceId = serviceId,
            packageName = pkg,
            isInstalledOnDevice = installed,
            nativeWeightMb = weight
        )
    }

    fun openInPlayStore(context: Context, packageName: String): Boolean {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            setPackage(PLAY_STORE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(marketIntent)
            true
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openPlayStoreApp(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PLAY_STORE_PACKAGE) ?: Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
