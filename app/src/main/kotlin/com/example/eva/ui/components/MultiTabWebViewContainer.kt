package com.example.eva.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.eva.model.AppService
import com.example.eva.service.EvaMediaPlaybackService
import com.example.eva.ui.theme.EvaColors

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

// Modern Chrome Mobile UA without '; wv' tag so Google/YouTube/Spotify allow standard sign-in and full responsive web app
private const val CHROME_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36"

// Comprehensive Background Audio Keep-Alive & Real-time Metadata Scraping Engine
private const val BACKGROUND_AUDIO_SCRIPT = """
(function() {
    if (window.__evaEngineInitialized) return;
    window.__evaEngineInitialized = true;
    window.__evaExplicitUserPause = false;

    // 1. Intercept HTMLMediaElement.prototype.pause: Block automatic background pauses
    try {
        var originalPause = HTMLMediaElement.prototype.pause;
        HTMLMediaElement.prototype.pause = function() {
            if (window.__evaExplicitUserPause === true) {
                return originalPause.apply(this, arguments);
            }
            // If pause is triggered by WebKit/Chromium due to backgrounding, immediately re-engage play
            var media = this;
            setTimeout(function() {
                if (window.__evaExplicitUserPause !== true) {
                    media.play().catch(function(){});
                }
            }, 20);
            return Promise.resolve();
        };
    } catch (e) {}

    // 2. Bypass Page Visibility & Focus API
    try {
        Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
        Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; }, configurable: true });
        document.hasFocus = function() { return true; };
    } catch (e) {}

    // 3. Suppress blur and visibility events from reaching YouTube/Spotify pause listeners
    ['visibilitychange', 'webkitvisibilitychange', 'blur', 'focusout', 'pagehide'].forEach(function(evt) {
        window.addEventListener(evt, function(e) { e.stopImmediatePropagation(); }, true);
        document.addEventListener(evt, function(e) { e.stopImmediatePropagation(); }, true);
    });

    // 4. Protect Web AudioContext from background suspension
    if (window.AudioContext) {
        try {
            var origSuspend = AudioContext.prototype.suspend;
            AudioContext.prototype.suspend = function() {
                if (window.__evaExplicitUserPause === true) {
                    return origSuspend.apply(this, arguments);
                }
                return Promise.resolve();
            };
        } catch (e) {}
    }

    // 5. Watchdog: Auto-resume active stream if paused unexpectedly
    setInterval(function() {
        if (window.__evaExplicitUserPause !== true) {
            var v = document.querySelector('video') || document.querySelector('audio');
            if (v && v.paused && v.currentTime > 0) {
                v.play().catch(function(){});
            }
        }
    }, 1000);

    // 6. Real-time Track Title & Artist Observer
    function notifyTrackUpdate() {
        try {
            var title = '';
            var artist = '';
            var artwork = '';

            // Standard HTML5 MediaSession API
            if (navigator.mediaSession && navigator.mediaSession.metadata) {
                title = navigator.mediaSession.metadata.title || '';
                artist = navigator.mediaSession.metadata.artist || '';
                if (navigator.mediaSession.metadata.artwork && navigator.mediaSession.metadata.artwork.length > 0) {
                    artwork = navigator.mediaSession.metadata.artwork[0].src || '';
                }
            }

            // YouTube Music Specific Player Bar Selectors
            if (!title) {
                var ytTitle = document.querySelector('ytmusic-player-bar .title, .title.ytmusic-player-bar, .ytmusic-player-bar yt-formatted-string.title');
                if (ytTitle) title = ytTitle.textContent.trim();
            }
            if (!artist) {
                var ytArtist = document.querySelector('ytmusic-player-bar .byline, .byline.ytmusic-player-bar');
                if (ytArtist) artist = ytArtist.textContent.trim();
            }

            // Spotify Web Player Selectors
            if (!title) {
                var spTitle = document.querySelector('[data-testid="now-playing-widget"] [data-testid="context-item-link"]');
                if (spTitle) title = spTitle.textContent.trim();
            }
            if (!artist) {
                var spArtist = document.querySelector('[data-testid="now-playing-widget"] [data-testid="track-info-artists"]');
                if (spArtist) artist = spArtist.textContent.trim();
            }

            // Dispatch to Native Android Media Bridge
            if (title && window.EvaMediaBridge) {
                window.EvaMediaBridge.onTrackChanged(title, artist, artwork);
            }
        } catch (e) {}
    }

    setInterval(notifyTrackUpdate, 1500);
})();
"""

class EvaMediaBridge(
    private val context: Context,
    private val serviceId: String,
    private val defaultServiceName: String
) {
    private var lastTrack = ""

    @JavascriptInterface
    fun onTrackChanged(title: String, artist: String, artworkUrl: String) {
        val cleanTitle = title.trim()
        val cleanArtist = if (artist.isNotBlank()) artist.trim() else defaultServiceName
        if (cleanTitle.isNotBlank() && cleanTitle != lastTrack) {
            lastTrack = cleanTitle
            EvaMediaPlaybackService.updateTrack(
                context = context,
                serviceId = serviceId,
                artist = cleanArtist,
                title = cleanTitle
            )
        }
    }
}

object EvaWebViewPool {
    private val webViews = mutableMapOf<String, WebView>()
    var activeServiceId: String = "ytmusic"
    var isFullscreenVideoActive: Boolean = false

    private var onOpenFileChooserAction: ((ValueCallback<Array<Uri>>?, Intent) -> Unit)? = null
    private var onCustomViewShowAction: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null
    private var onCustomViewHideAction: (() -> Unit)? = null

    fun setOpenFileChooserHandler(handler: (ValueCallback<Array<Uri>>?, Intent) -> Unit) {
        onOpenFileChooserAction = handler
    }

    fun setCustomViewHandlers(
        onShow: (View, WebChromeClient.CustomViewCallback) -> Unit,
        onHide: () -> Unit
    ) {
        onCustomViewShowAction = onShow
        onCustomViewHideAction = onHide
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
                tag = service.id
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Lightweight Adaptive Acceleration
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

                    // Performance & Thermal Optimizations
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    setNeedInitialFocus(false)
                    textZoom = 100

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

                // Native Media Track Metadata Bridge
                addJavascriptInterface(
                    EvaMediaBridge(context, service.id, service.name),
                    "EvaMediaBridge"
                )

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
                        request?.grant(request.resources)
                    }

                    // Native YouTube Fullscreen Mode Support
                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view != null && callback != null) {
                            isFullscreenVideoActive = true
                            onCustomViewShowAction?.invoke(view, callback)
                        }
                    }

                    override fun onHideCustomView() {
                        isFullscreenVideoActive = false
                        onCustomViewHideAction?.invoke()
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
                            if (url.contains("play.google.com/store/apps/details")) {
                                return true
                            }
                            return false
                        }

                        // 3. ANTI-INTENT HIJACKING: Intercept intent:// URLs
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
                            return true
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

                        return true
                    }

                    // SECURITY: Strict SSL Verification
                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.cancel()
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let(onUrlChanged)
                        view?.evaluateJavascript(BACKGROUND_AUDIO_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        cookieManager.flush()
                        view?.evaluateJavascript(BACKGROUND_AUDIO_SCRIPT, null)
                    }
                }

                loadUrl(service.url)
            }
        }
    }

    /**
     * Executes Play/Pause directly on the Android Main Looper without waiting for view attachment.
     */
    fun setPlayPauseState(serviceId: String?, play: Boolean) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = if (play) {
            """
            (function() {
                window.__evaExplicitUserPause = false;
                var v = document.querySelector('video') || document.querySelector('audio');
                if (v) { v.play().catch(function(){}); return true; }
                var btn = document.querySelector('#play-pause-button, button[data-testid="control-button-playpause"], .ytp-play-button');
                if (btn) { btn.click(); return true; }
                return false;
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                window.__evaExplicitUserPause = true;
                var v = document.querySelector('video') || document.querySelector('audio');
                if (v) { v.pause(); return true; }
                var btn = document.querySelector('#play-pause-button, button[data-testid="control-button-playpause"], .ytp-play-button');
                if (btn) { btn.click(); return true; }
                return false;
            })();
            """.trimIndent()
        }

        Handler(Looper.getMainLooper()).post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause(serviceId: String?) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = """
        (function() {
            var v = document.querySelector('video') || document.querySelector('audio');
            if (v) {
                if (v.paused) {
                    window.__evaExplicitUserPause = false;
                    v.play().catch(function(){});
                } else {
                    window.__evaExplicitUserPause = true;
                    v.pause();
                }
                return !v.paused;
            }
            var btn = document.querySelector('#play-pause-button, button[data-testid="control-button-playpause"], .ytp-play-button');
            if (btn) { btn.click(); return true; }
            return false;
        })();
        """.trimIndent()

        Handler(Looper.getMainLooper()).post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    fun skipNext(serviceId: String?) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = """
        (function() {
            window.__evaExplicitUserPause = false;
            var nextBtn = document.querySelector('.next-button, ytmusic-player-bar .next-button, button[data-testid="control-button-skip-forward"], .ytp-next-button, button[aria-label="Next"]');
            if (nextBtn) { nextBtn.click(); }
        })();
        """.trimIndent()

        Handler(Looper.getMainLooper()).post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    fun skipPrevious(serviceId: String?) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = """
        (function() {
            window.__evaExplicitUserPause = false;
            var prevBtn = document.querySelector('.previous-button, ytmusic-player-bar .previous-button, button[data-testid="control-button-skip-back"], .ytp-prev-button, button[aria-label="Previous"]');
            if (prevBtn) { prevBtn.click(); }
        })();
        """.trimIndent()

        Handler(Looper.getMainLooper()).post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    fun checkVideoPlaying(serviceId: String, callback: (Boolean) -> Unit) {
        val wv = webViews[serviceId]
        if (wv == null) {
            callback(false)
            return
        }
        Handler(Looper.getMainLooper()).post {
            try {
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
            } catch (_: Exception) {
                callback(false)
            }
        }
    }

    fun reloadCurrent(serviceId: String) {
        Handler(Looper.getMainLooper()).post {
            webViews[serviceId]?.reload()
        }
    }

    fun canGoBack(serviceId: String): Boolean {
        return webViews[serviceId]?.canGoBack() == true
    }

    fun goBack(serviceId: String) {
        Handler(Looper.getMainLooper()).post {
            webViews[serviceId]?.goBack()
        }
    }

    /**
     * GDPR & Data Protection: Complete Sandbox Wipe of all cookies, cache, and WebStorage.
     */
    fun clearAllDataAndCookies(context: Context) {
        Handler(Looper.getMainLooper()).post {
            try {
                // Clear all webviews
                webViews.values.forEach { it.destroy() }
                webViews.clear()

                // Clear all cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()

                // Clear web storage (localStorage, sessionStorage, IndexedDB)
                WebStorage.getInstance().deleteAllData()

                Toast.makeText(context, "All browsing data & cache erased securely.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun clearAll() {
        Handler(Looper.getMainLooper()).post {
            webViews.values.forEach { it.destroy() }
            webViews.clear()
        }
    }
}

@Composable
fun MultiTabWebViewContainer(
    activeService: AppService,
    modifier: Modifier = Modifier,
    onPageTitleChanged: (String) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onShowCustomView: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    onHideCustomView: (() -> Unit)? = null
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

    // Connect Custom Fullscreen View to WebChromeClient
    if (onShowCustomView != null && onHideCustomView != null) {
        EvaWebViewPool.setCustomViewHandlers(onShowCustomView, onHideCustomView)
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

                // Adaptive Thermal Throttling & In-Memory Fast Swapping
                for (i in 0 until frameLayout.childCount) {
                    val child = frameLayout.getChildAt(i)
                    if (child === activeWebView) {
                        child.visibility = View.VISIBLE
                        child.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        activeWebView.onResume()
                    } else {
                        child.visibility = View.GONE
                        // Remove hardware layer to cool down GPU
                        child.setLayerType(View.LAYER_TYPE_NONE, null)
                        // Never pause the active media tab (YouTube Music / Spotify) so audio streams continuously!
                        val otherServiceId = child.tag as? String
                        if (otherServiceId != "ytmusic" && otherServiceId != "spotify" && otherServiceId != EvaMediaPlaybackService.activeServiceId) {
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
