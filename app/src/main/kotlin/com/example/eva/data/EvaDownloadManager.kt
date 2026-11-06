package com.example.eva.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.eva.diagnostics.EvaDiagnosticsManager
import java.io.File

/**
 * Native Android Download Manager implementation for EVA Web Experience 3.0.
 *
 * Supports:
 * - Robust queueing via Android's native DownloadManager.
 * - HTTP Cookie, User-Agent, and Referrer forwarding.
 * - Safe filename conflict resolution.
 * - Secure file opening and sharing via FileProvider.
 * - Notification completion actions.
 */
object EvaDownloadManager {

    data class DownloadItem(
        val downloadId: Long,
        val fileName: String,
        val uri: String,
        val mimeType: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val activeDownloads = mutableMapOf<Long, DownloadItem>()
    private var isReceiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    val item = activeDownloads.remove(downloadId)
                    EvaDiagnosticsManager.recordDownloadCompleted()
                    val fileName = item?.fileName ?: "File"
                    Toast.makeText(context, "$fileName download complete", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun registerReceiver(context: Context) {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    downloadReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.applicationContext.registerReceiver(downloadReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    /**
     * Enqueues a file download with complete HTTP metadata.
     */
    fun enqueueDownload(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        serviceName: String
    ): Long {
        registerReceiver(context)

        return try {
            val cleanMimeType = sanitizeMimeType(url, mimeType)
            val suggestedFileName = URLUtil.guessFileName(url, contentDisposition, cleanMimeType)
            val uniqueFileName = resolveUniqueFileName(suggestedFileName)

            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri).apply {
                setMimeType(cleanMimeType)
                userAgent?.let { addRequestHeader("User-Agent", it) }

                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }

                setTitle(uniqueFileName)
                setDescription("Downloading from $serviceName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uniqueFileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            val downloadId = manager?.enqueue(request) ?: -1L

            if (downloadId != -1L) {
                activeDownloads[downloadId] = DownloadItem(
                    downloadId = downloadId,
                    fileName = uniqueFileName,
                    uri = url,
                    mimeType = cleanMimeType
                )
                Toast.makeText(context, "Download started: $uniqueFileName", Toast.LENGTH_SHORT).show()
            }
            downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "Download error: ${e.localizedMessage ?: "Failed"}", Toast.LENGTH_SHORT).show()
            -1L
        }
    }

    /**
     * Opens a completed download file using Android's FileProvider.
     */
    fun openDownloadedFile(context: Context, fileName: String, mimeType: String): Boolean {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) return false

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(openIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Shares a downloaded file with another application.
     */
    fun shareDownloadedFile(context: Context, fileName: String, mimeType: String): Boolean {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) return false

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share $fileName").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    internal fun sanitizeMimeType(url: String, mimeType: String?): String {
        if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream") {
            return mimeType
        }
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val extension = cleanUrl.substringAfterLast('.', "")
        if (extension.isNotBlank()) {
            return when (extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "pdf" -> "application/pdf"
                "json" -> "application/json"
                "apk" -> "application/vnd.android.package-archive"
                else -> {
                    try {
                        val mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                        if (!mapped.isNullOrBlank()) mapped else "application/octet-stream"
                    } catch (_: Exception) {
                        "application/octet-stream"
                    }
                }
            }
        }
        return "application/octet-stream"
    }

    internal fun resolveUniqueFileName(
        fileName: String,
        downloadsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    ): String {
        val target = File(downloadsDir, fileName)
        if (!target.exists()) return fileName

        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex != -1) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex != -1) fileName.substring(dotIndex) else ""

        var counter = 1
        var candidate = "$baseName ($counter)$extension"
        while (File(downloadsDir, candidate).exists()) {
            counter++
            candidate = "$baseName ($counter)$extension"
        }
        return candidate
    }
}
