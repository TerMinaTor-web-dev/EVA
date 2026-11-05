package com.example.eva.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.eva.model.AppService
import com.example.eva.service.EvaMediaPlaybackService
import com.example.eva.ui.theme.EvaColors

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

// Modern Chrome Mobile UA that avoids the restrictive '; wv' token so websites don't force 'Open in App'
private const val CHROME_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"

object EvaWebViewPool {
    private val webViews = mutableMapOf<String, WebView>()
    var activeServiceId: String = "instagram"
    private var onOpenFileChooserAction: ((ValueCallback<Array<Uri>>?, Intent) -> Unit)? = null

    fun setOpenFileChooserHandler(handler: (ValueCallback<Array<Uri>>?, Intent) -> Unit) {
        onOpenFileChooserAction = handler
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        service: AppService,
        onProgressChanged: (Float) -> Unit,
        onTitleChanged: (String) -> Unit,
        onUrlChanged: (String) -> Unit
    ): WebView {
        activeServiceId = service.id
        return webViews.getOrPut(service.id) {
            WebView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 120 FPS Fluid Hardware Acceleration Engine
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // Chromium Web Core Configuration
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false

                    // SECURITY: Strict Local File Sandbox Isolation
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false

                    // Background Media Playback & WebRTC
                    mediaPlaybackRequiresUserGesture = false
                    javaScriptCanOpenWindowsAutomatically = true

                    // Serve High-Fidelity Chrome Mobile or Desktop Engine
                    userAgentString = if (service.isDesktopUserAgent) {
                        DESKTOP_USER_AGENT
                    } else {
                        CHROME_MOBILE_USER_AGENT
                    }
                }

                // Cookie Persistence with Strict Security
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                // File & Media Downloads Handler
                setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                    try {
                        val request = DownloadManager.Request(Uri.parse(url)).apply {
                            setMimeType(mimetype)
                            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                            addRequestHeader("User-Agent", userAgent)
                            setDescription("Downloading file from ${service.name}")
                            setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                URLUtil.guessFileName(url, contentDisposition, mimetype)
                            )
                        }
                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                        manager?.enqueue(request)
                        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to process download", Toast.LENGTH_SHORT).show()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        if (!title.isNullOrBlank()) onTitleChanged(title)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        // Grant camera and microphone permissions inside the sandbox
                        request?.grant(request.resources)
                    }

                    // Native Storage File Chooser for Photos, Videos, and Document Uploads
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        onOpenFileChooserAction?.invoke(filePathCallback, intent)
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        val url = uri.toString()
                        onUrlChanged(url)

                        // 1. SECURITY: Block untrusted scheme hijacking
                        val scheme = uri.scheme?.lowercase() ?: ""
                        if (scheme == "file" || scheme == "content" || scheme == "javascript") {
                            return true
                        }

                        // 2. HTTP/HTTPS: Maintain inside EVA Chromium Core
                        if (scheme == "http" || scheme == "https") {
                            // Block intrusive play store redirects to force web fidelity
                            if (url.contains("play.google.com/store/apps/details")) {
                                return true
                            }
                            return false
                        }

                        // 3. ANTI-INTENT HIJACKING: Intercept intent:// URLs so native apps are NOT launched
                        if (scheme == "intent" || url.startsWith("intent://")) {
                            try {
                                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                if (!fallbackUrl.isNullOrBlank()) {
                                    view?.loadUrl(fallbackUrl)
                                    return true
                                }
                                val dataUri = intent.data
                                if (dataUri != null && (dataUri.scheme == "http" || dataUri.scheme == "https")) {
                                    view?.loadUrl(dataUri.toString())
                                    return true
                                }
                            } catch (_: Exception) {}
                            return true // Disallow launching external native app
                        }

                        // 4. Map app-specific deep links back into secure Web URLs
                        when {
                            url.startsWith("instagram://") -> {
                                val path = url.substringAfter("instagram://")
                                view?.loadUrl("https://www.instagram.com/$path")
                                return true
                            }
                            url.startsWith("spotify://") -> {
                                val path = url.substringAfter("spotify://")
                                view?.loadUrl("https://open.spotify.com/$path")
                                return true
                            }
                            url.startsWith("twitter://") || url.startsWith("x://") -> {
                                val path = url.substringAfter("://")
                                view?.loadUrl("https://x.com/$path")
                                return true
                            }
                            url.startsWith("vnd.youtube:") -> {
                                val path = url.substringAfter("vnd.youtube:")
                                view?.loadUrl("https://www.youtube.com/$path")
                                return true
                            }
                            url.startsWith("fb://") -> {
                                val path = url.substringAfter("fb://")
                                view?.loadUrl("https://www.facebook.com/$path")
                                return true
                            }
                        }

                        // Disallow external escape
                        return true
                    }

                    // SECURITY: Strict SSL Verification - Never bypass invalid certificates!
                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.cancel() // Prevents MITM attacks
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let(onUrlChanged)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        cookieManager.flush()
                    }
                }

                loadUrl(service.url)
            }
        }
    }

    fun togglePlayPause(serviceId: String?) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull()
        wv?.post {
            wv.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video, audio');
                    if (v) {
                        if (v.paused) { v.play(); } else { v.pause(); }
                        return !v.paused;
                    }
                    var playBtn = document.querySelector('button[data-testid="control-button-playpause"], .ytp-play-button, button[aria-label="Play"], button[aria-label="Pause"]');
                    if (playBtn) { playBtn.click(); return true; }
                    return false;
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun skipNext(serviceId: String?) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull()
        wv?.post {
            wv.evaluateJavascript(
                """
                (function() {
                    var nextBtn = document.querySelector('button[data-testid="control-button-skip-forward"], .ytp-next-button, button[aria-label="Next"], button[aria-label="Next track"], .next-button');
                    if (nextBtn) { nextBtn.click(); }
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun skipPrevious(serviceId: String?) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull()
        wv?.post {
            wv.evaluateJavascript(
                """
                (function() {
                    var prevBtn = document.querySelector('button[data-testid="control-button-skip-back"], .ytp-prev-button, button[aria-label="Previous"], button[aria-label="Previous track"], .previous-button');
                    if (prevBtn) { prevBtn.click(); }
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun checkVideoPlaying(serviceId: String, callback: (Boolean) -> Unit) {
        val wv = webViews[serviceId]
        if (wv == null) {
            callback(false)
            return
        }
        wv.post {
            wv.evaluateJavascript(
                """
                (function() {
                    var v = document.querySelector('video');
                    return (v != null && !v.paused && v.currentTime > 0);
                })();
                """.trimIndent()
            ) { result ->
                callback(result == "true")
            }
        }
    }

    fun reloadCurrent(serviceId: String) {
        webViews[serviceId]?.reload()
    }

    fun canGoBack(serviceId: String): Boolean {
        return webViews[serviceId]?.canGoBack() == true
    }

    fun goBack(serviceId: String) {
        webViews[serviceId]?.goBack()
    }

    fun clearAll() {
        webViews.values.forEach { it.destroy() }
        webViews.clear()
    }
}

@Composable
fun MultiTabWebViewContainer(
    activeService: AppService,
    modifier: Modifier = Modifier,
    onPageTitleChanged: (String) -> Unit = {},
    onUrlChanged: (String) -> Unit = {}
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }

    // Native Storage File Chooser State
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null) {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
        } else {
            null
        }
        pendingFileCallback?.onReceiveValue(uris)
        pendingFileCallback = null
    }

    // Connect File Chooser to WebChromeClient
    EvaWebViewPool.setOpenFileChooserHandler { callback, intent ->
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = callback
        fileChooserLauncher.launch(intent)
    }

    // Hardware back navigation
    BackHandler(enabled = EvaWebViewPool.canGoBack(activeService.id)) {
        EvaWebViewPool.goBack(activeService.id)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { frameLayout ->
                val activeWebView = EvaWebViewPool.getOrCreateWebView(
                    context = frameLayout.context,
                    service = activeService,
                    onProgressChanged = { p ->
                        progress = p
                        isLoading = p < 1.0f
                    },
                    onTitleChanged = { title ->
                        onPageTitleChanged(title)
                        // Trigger background audio keep-alive for music/video streaming services
                        if (activeService.category in listOf("Music", "Video", "Audio")) {
                            EvaMediaPlaybackService.startPlayback(
                                frameLayout.context,
                                activeService.id,
                                activeService.name,
                                title
                            )
                        }
                    },
                    onUrlChanged = onUrlChanged
                )

                // Attach to view hierarchy if not already attached
                if (activeWebView.parent != frameLayout) {
                    (activeWebView.parent as? ViewGroup)?.removeView(activeWebView)
                    frameLayout.addView(activeWebView)
                }

                // In-Memory Fast Swapping (0ms lag, no network reloads)
                for (i in 0 until frameLayout.childCount) {
                    val child = frameLayout.getChildAt(i)
                    if (child === activeWebView) {
                        child.visibility = View.VISIBLE
                        activeWebView.onResume()
                    } else {
                        child.visibility = View.GONE
                        // If background child is media, don't pause it so audio keeps streaming!
                        val otherServiceId = child.tag as? String
                        if (otherServiceId != "spotify" && otherServiceId != "ytmusic") {
                            (child as? WebView)?.onPause()
                        }
                    }
                }
            }
        )

        // Slim Cybernetic Progress Indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = EvaColors.NeonCyan,
                trackColor = Color.Transparent
            )
        }
    }
}
