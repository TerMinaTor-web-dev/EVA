package com.example.eva.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.eva.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Enterprise GitHub-Connected Auto-Update Engine.
 * 
 * Features:
 * - Real-time version check against repository release manifests with zero API rate limits.
 * - 24-hour smart postpone snooze: if user selects "Update Later", notification/dialog
 *   is suppressed for 24 hours before reminding again.
 * - In-app background download stream with progress feedback.
 * - Secure native package installer trigger via Android FileProvider.
 */
object EvaUpdateManager {

    private const val PREFS_NAME = "eva_update_prefs"
    private const val KEY_LAST_POSTPONED = "key_last_postponed_timestamp"
    private const val SNOOZE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 Hours

    private const val PRIMARY_MANIFEST_URL =
        "https://raw.githubusercontent.com/TerMinaTor-web-dev/EVA/main/version.json"
    private const val FALLBACK_API_URL =
        "https://api.github.com/repos/TerMinaTor-web-dev/EVA/releases/latest"

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val changelog: String
    )

    private val updateScope = CoroutineScope(Dispatchers.Main + Job())

    /**
     * Checks for new updates from GitHub.
     * @param force If true, bypasses the 24-hour snooze interval (e.g. user manually clicked "Check Updates").
     */
    fun checkForUpdates(
        context: Context,
        force: Boolean = false,
        onResult: (UpdateInfo?) -> Unit
    ) {
        if (!force) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastPostponed = prefs.getLong(KEY_LAST_POSTPONED, 0L)
            val now = System.currentTimeMillis()
            if (now - lastPostponed < SNOOZE_DURATION_MS) {
                onResult(null)
                return
            }
        }

        updateScope.launch {
            val update = withContext(Dispatchers.IO) {
                fetchUpdateInfo()
            }

            if (update != null && update.versionCode > BuildConfig.VERSION_CODE) {
                onResult(update)
            } else {
                onResult(null)
            }
        }
    }

    /**
     * Records the 24-hour snooze timestamp when the user taps "Update Later".
     */
    fun postponeUpdate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_POSTPONED, System.currentTimeMillis()).apply()
    }

    /**
     * Downloads the APK update from GitHub with progress callbacks and triggers the system package installer.
     */
    fun downloadAndInstallUpdate(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        // Android 8.0+ Check for unknown app install permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                onError("Please grant install permission to continue update.")
                return
            }
        }

        updateScope.launch {
            val resultFile = withContext(Dispatchers.IO) {
                downloadApkFile(context, downloadUrl, onProgress)
            }

            if (resultFile != null && resultFile.exists()) {
                triggerPackageInstaller(context, resultFile)
            } else {
                onError("Failed to download update. Please check internet connection.")
            }
        }
    }

    private fun fetchUpdateInfo(): UpdateInfo? {
        // 1. Try Primary Direct Repository Manifest (Zero rate limits, instantaneous)
        try {
            val connection = URL(PRIMARY_MANIFEST_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.useCaches = false

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                return UpdateInfo(
                    versionCode = json.optInt("versionCode", 0),
                    versionName = json.optString("versionName", "1.0"),
                    downloadUrl = json.optString("downloadUrl", ""),
                    changelog = json.optString("changelog", "Bug fixes and performance improvements.")
                )
            }
        } catch (_: Exception) {}

        // 2. Fallback to GitHub Releases API
        try {
            val connection = URL(FALLBACK_API_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.useCaches = false

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "").removePrefix("v")
                val body = json.optString("body", "New production release available.")

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (apkDownloadUrl.isBlank()) {
                    apkDownloadUrl = "https://github.com/TerMinaTor-web-dev/EVA/raw/main/release/EVA_v${tagName}_Release.apk"
                }

                val majorMinor = tagName.split(".")
                val parsedCode = try {
                    majorMinor[0].toInt() * 10 + (majorMinor.getOrNull(1)?.toInt() ?: 0)
                } catch (_: Exception) {
                    BuildConfig.VERSION_CODE + 1
                }

                return UpdateInfo(
                    versionCode = parsedCode,
                    versionName = tagName,
                    downloadUrl = apkDownloadUrl,
                    changelog = body
                )
            }
        } catch (_: Exception) {}

        return null
    }

    private suspend fun downloadApkFile(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val destination = File(dir, "EVA_update_temp.apk")
            if (destination.exists()) destination.delete()

            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true
            connection.connect()

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(destination)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var count: Int

            while (input.read(buffer).also { count = it } != -1) {
                output.write(buffer, 0, count)
                totalBytesRead += count
                if (fileLength > 0) {
                    val progress = (totalBytesRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            output.flush()
            output.close()
            input.close()
            destination
        } catch (_: Exception) {
            null
        }
    }

    private fun triggerPackageInstaller(context: Context, apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (_: Exception) {}
    }
}
