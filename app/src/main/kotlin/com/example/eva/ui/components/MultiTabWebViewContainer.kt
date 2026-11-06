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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.eva.model.AppService
import com.example.eva.service.EvaMediaCoordinator
import com.example.eva.service.EvaMediaPlaybackService
import com.example.eva.ui.theme.EvaColors

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

// Modern Chrome Mobile UA without '; wv' tag so Google/YouTube/Spotify allow standard sign-in and full responsive web app
private const val CHROME_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36"

// Event-driven, thermal-optimized background audio keep-alive & multi-app media scraper
private const val BACKGROUND_AUDIO_SCRIPT = """
(function() {
    if (window.__evaEngineInitialized) return;
    window.__evaEngineInitialized = true;
    window.__evaExplicitUserPause = false;
    window.__evaServiceId = '%SERVICE_ID%';

    // 1. Intercept HTMLMediaElement.prototype.pause: Block Chromium background pauses
    try {
        var originalPause = HTMLMediaElement.prototype.pause;
        HTMLMediaElement.prototype.pause = function() {
            if (window.__evaExplicitUserPause === true) {
                return originalPause.apply(this, arguments);
            }
            var media = this;
            setTimeout(function() {
                if (window.__evaExplicitUserPause !== true) {
                    media.play().catch(function(){});
                }
            }, 30);
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

    // 3. Suppress blur and visibility events
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

    // Helper: Extract current track info
    function getTrackMetadata() {
        var title = '';
        var artist = '';
        var artwork = '';

        if (navigator.mediaSession && navigator.mediaSession.metadata) {
            title = navigator.mediaSession.metadata.title || '';
            artist = navigator.mediaSession.metadata.artist || '';
            if (navigator.mediaSession.metadata.artwork && navigator.mediaSession.metadata.artwork.length > 0) {
                artwork = navigator.mediaSession.metadata.artwork[0].src || '';
            }
        }
        if (!title) {
            var ytTitle = document.querySelector('ytmusic-player-bar .title, .title.ytmusic-player-bar, .ytmusic-player-bar yt-formatted-string.title');
            if (ytTitle) title = ytTitle.textContent.trim();
        }
        if (!artist) {
            var ytArtist = document.querySelector('ytmusic-player-bar .byline, .byline.ytmusic-player-bar');
            if (ytArtist) artist = ytArtist.textContent.trim();
        }
        if (!title) {
            var spTitle = document.querySelector('[data-testid="now-playing-widget"] [data-testid="context-item-link"]');
            if (spTitle) title = spTitle.textContent.trim();
        }
        if (!artist) {
            var spArtist = document.querySelector('[data-testid="now-playing-widget"] [data-testid="track-info-artists"]');
            if (spArtist) artist = spArtist.textContent.trim();
        }
        if (!title) {
            var ytMainTitle = document.querySelector('h1.ytd-watch-metadata, .slim-video-metadata-title');
            if (ytMainTitle) title = ytMainTitle.textContent.trim();
        }
        return { title: title, artist: artist, artwork: artwork };
    }

    // 5. Intercept navigator.mediaSession.metadata setter for ZERO-POLLING instant track updates
    try {
        if (navigator.mediaSession) {
            var metaDesc = Object.getOwnPropertyDescriptor(MediaSession.prototype, 'metadata');
            if (metaDesc && metaDesc.set) {
                Object.defineProperty(navigator.mediaSession, 'metadata', {
                    set: function(meta) {
                        metaDesc.set.call(this, meta);
                        if (meta && window.EvaMediaBridge) {
                            var title = meta.title || '';
                            var artist = meta.artist || '';
                            var artwork = (meta.artwork && meta.artwork.length > 0) ? meta.artwork[0].src : '';
                            window.EvaMediaBridge.onTrackChanged(title, artist, artwork);
                        }
                    },
                    get: function() { return metaDesc.get.call(this); }
                });
            }
        }
    } catch (e) {}

    // 6. Zero-Idle Event-Driven Media Hooks for Multi-App Audio Concurrency & Animation
    function hookMediaElement(el) {
        if (!el || el.__evaHooked) return;
        el.__evaHooked = true;

        function notifyPlay() {
            var isAudible = (!el.muted && el.volume > 0);
            if (!isAudible) return; // Muted videos (e.g. silent Instagram/Reddit feeds) do not pause music!

            var meta = getTrackMetadata();
            var dur = (el.duration && !isNaN(el.duration) && isFinite(el.duration)) ? el.duration : 0;
            var pos = (el.currentTime && !isNaN(el.currentTime)) ? el.currentTime : 0;

            if (window.EvaMediaBridge) {
                window.EvaMediaBridge.onMediaPlayStarted(
                    window.__evaServiceId,
                    meta.title,
                    meta.artist,
                    meta.artwork,
                    dur,
                    pos
                );
            }
        }

        function notifyPause() {
            var dur = (el.duration && !isNaN(el.duration) && isFinite(el.duration)) ? el.duration : 0;
            var pos = (el.currentTime && !isNaN(el.currentTime)) ? el.currentTime : 0;

            if (window.EvaMediaBridge) {
                window.EvaMediaBridge.onMediaPlayPaused(
                    window.__evaServiceId,
                    dur,
                    pos
                );
            }
        }

        el.addEventListener('play', notifyPlay);
        el.addEventListener('playing', notifyPlay);
        el.addEventListener('pause', notifyPause);
        el.addEventListener('ended', notifyPause);

        el.addEventListener('volumechange', function() {
            if (!el.muted && el.volume > 0 && !el.paused) {
                notifyPlay();
            } else if ((el.muted || el.volume === 0) && !el.paused) {
                notifyPause();
            }
        });

        // Throttled timeupdate for seekbar animation (fires at most once every 3.5s)
        var lastTimeReport = 0;
        el.addEventListener('timeupdate', function() {
            var now = Date.now();
            if (now - lastTimeReport > 3500 && !el.paused && !el.muted && el.volume > 0) {
                lastTimeReport = now;
                var dur = (el.duration && !isNaN(el.duration) && isFinite(el.duration)) ? el.duration : 0;
                var pos = (el.currentTime && !isNaN(el.currentTime)) ? el.currentTime : 0;
                if (window.EvaMediaBridge) {
                    window.EvaMediaBridge.onPlaybackPosition(window.__evaServiceId, dur, pos);
                }
            }
        });
    }

    // Attach to existing media elements
    var initialMedia = document.querySelectorAll('video, audio');
    for (var i = 0; i < initialMedia.length; i++) {
        hookMediaElement(initialMedia[i]);
    }

    // Observe future dynamically inserted media elements with zero idle overhead
    try {
        var observer = new MutationObserver(function(mutations) {
            for (var m = 0; m < mutations.length; m++) {
                var added = mutations[m].addedNodes;
                for (var n = 0; n < added.length; n++) {
                    var node = added[n];
                    if (node.nodeType === 1) {
                        if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                            hookMediaElement(node);
                        } else if (node.querySelectorAll) {
                            var nested = node.querySelectorAll('video, audio');
                            for (var k = 0; k < nested.length; k++) {
                                hookMediaElement(nested[k]);
                            }
                        }
                    }
                }
            }
        });
        observer.observe(document.documentElement, { childList: true, subtree: true });
    } catch (e) {}
})();
"""

class EvaMediaBridge(
    private val context: Context,
    private val serviceId: String,
    private val defaultServiceName: String
) {
    private var lastTrack = ""

    @JavascriptInterface
    fun onMediaPlayStarted(
        serviceId: String,
        title: String,
        artist: String,
        artworkUrl: String,
        durationSec: Double,
        positionSec: Double
    ) {
        val cleanTitle = title.trim()
        val cleanArtist = if (artist.isNotBlank()) artist.trim() else defaultServiceName
        val durationMs = (durationSec * 1000).toLong().coerceAtLeast(0L)
        val positionMs = (positionSec * 1000).toLong().coerceAtLeast(0L)

        EvaMediaCoordinator.onMediaPlayStarted(
            context = context,
            serviceId = serviceId,
            title = cleanTitle,
            artist = cleanArtist,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            positionMs = positionMs
        )
    }

    @JavascriptInterface
    fun onMediaPlayPaused(
        serviceId: String,
        durationSec: Double,
        positionSec: Double
    ) {
        val durationMs = (durationSec * 1000).toLong().coerceAtLeast(0L)
        val positionMs = (positionSec * 1000).toLong().coerceAtLeast(0L)

        EvaMediaCoordinator.onMediaPlayPaused(
            context = context,
            serviceId = serviceId,
            durationMs = durationMs,
            positionMs = positionMs
        )
    }

    @JavascriptInterface
    fun onPlaybackPosition(
        serviceId: String,
        durationSec: Double,
        positionSec: Double
    ) {
        val durationMs = (durationSec * 1000).toLong().coerceAtLeast(0L)
        val positionMs = (positionSec * 1000).toLong().coerceAtLeast(0L)

        EvaMediaCoordinator.onPlaybackPosition(
            context = context,
            serviceId = serviceId,
            durationMs = durationMs,
            positionMs = positionMs
        )
    }

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
                        val script = BACKGROUND_AUDIO_SCRIPT.replace("%SERVICE_ID%", service.id)
                        view?.evaluateJavascript(script, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        cookieManager.flush()
                        val script = BACKGROUND_AUDIO_SCRIPT.replace("%SERVICE_ID%", service.id)
                        view?.evaluateJavascript(script, null)
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

    fun seekTo(serviceId: String?, positionMs: Long) {
        val targetId = serviceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val seconds = positionMs / 1000.0
        val script = """
        (function() {
            var v = document.querySelector('video') || document.querySelector('audio');
            if (v) { v.currentTime = $seconds; }
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
                webViews.values.forEach { it.destroy() }
                webViews.clear()

                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()

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
                    val childServiceId = child.tag as? String
                    if (child === activeWebView) {
                        child.visibility = View.VISIBLE
                        child.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        activeWebView.onResume()
                    } else {
                        child.visibility = View.GONE
                        // Remove hardware layer to cool down GPU
                        child.setLayerType(View.LAYER_TYPE_NONE, null)
                        // Only keep the actively playing or waiting audio tab alive in background; pause all others to eliminate heat/battery drain
                        val isActivelyPlayingAudio = (childServiceId == EvaMediaCoordinator.activeAudioServiceId ||
                                childServiceId == EvaMediaCoordinator.pausedBackgroundServiceId)
                        if (!isActivelyPlayingAudio && childServiceId != "ytmusic" && childServiceId != "spotify") {
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
