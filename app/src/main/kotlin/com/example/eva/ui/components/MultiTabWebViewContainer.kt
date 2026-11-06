package com.example.eva.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
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
import com.example.eva.data.EvaDownloadManager
import com.example.eva.data.ServiceWebProfileRegistry
import com.example.eva.diagnostics.EvaDiagnosticsManager
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

// Native App Shell Optimization: Injects native-app look & feel, strips 'Open in App' nag banners, enables 120 FPS momentum
private const val NATIVE_APP_SHELL_TRANSFORM_SCRIPT = """
(function() {
    if (window.__evaNativeShellInjected) return;
    window.__evaNativeShellInjected = true;

    var css = `
        * {
            -webkit-tap-highlight-color: transparent !important;
            -webkit-touch-callout: none !important;
        }
        input, textarea, [contenteditable="true"] {
            -webkit-touch-callout: default !important;
            -webkit-user-select: auto !important;
        }
        body {
            overscroll-behavior-y: contain !important;
            -webkit-font-smoothing: antialiased !important;
        }
        ::-webkit-scrollbar {
            display: none !important;
            width: 0px !important;
            height: 0px !important;
        }
        [href*="instagram://"], [href*="play.google.com/store/apps/details?id=com.instagram"],
        .x1c4vz4f, div:has(> [aria-label*="Get Instagram"]),
        ytm-smart-app-banner, .mobile-topbar-header [aria-label*="Open app"], ytm-promoted-sparkles-web-renderer,
        .XPromoPopup, shreddit-async-loader[bundlename*="xpromo"], [slot="bottom-banner"],
        reddit-header-action-items a[href*="reddit.com/get-app"],
        [data-testid*="banner-open-app"], .spotify-app-banner,
        [aria-label*="Switch to the app"], [data-testid="app-upsell"],
        .smartbanner, .app-banner, [class*="smartbanner"], [class*="app-banner"],
        [class*="install-app"], [class*="open-in-app"], [id*="smartbanner"] {
            display: none !important;
            visibility: hidden !important;
            height: 0px !important;
            max-height: 0px !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
    `;

    function injectStyle() {
        if (document.getElementById('eva-native-shell-style')) return;
        var style = document.createElement('style');
        style.id = 'eva-native-shell-style';
        style.appendChild(document.createTextNode(css));
        (document.head || document.documentElement).appendChild(style);
    }
    injectStyle();

    function dismissAppNag() {
        injectStyle();
        var redditDismiss = document.querySelector('.XPromoPopup button, shreddit-async-loader button[aria-label*="Close"], .xpromo-continue-button');
        if (redditDismiss) {
            try { redditDismiss.click(); } catch(e) {}
        }
        var instaClose = document.querySelector('div[role="dialog"] button:has(svg[aria-label*="Close"])');
        if (instaClose) {
            try { instaClose.click(); } catch(e) {}
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', dismissAppNag);
    } else {
        dismissAppNag();
    }

    var observer = new MutationObserver(function() {
        dismissAppNag();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
})();
"""

// Event-driven, thermal-optimized background audio keep-alive & multi-app media scraper
private const val BACKGROUND_AUDIO_SCRIPT = """
(function() {
    if (window.__evaEngineInitialized) return;
    window.__evaEngineInitialized = true;
    window.__evaExplicitUserPause = false;
    window.__evaServiceId = '%SERVICE_ID%';

    // 0. User Screen Interaction Tracker: Distinguishes physical user clicks/taps from background suspensions
    var lastUserInteractionTime = 0;
    ['touchstart', 'touchend', 'click', 'pointerdown', 'mousedown'].forEach(function(evt) {
        document.addEventListener(evt, function() {
            lastUserInteractionTime = Date.now();
        }, { capture: true, passive: true });
    });

    // 1. Intercept MediaSession action handlers (YouTube, Spotify, SoundCloud)
    window.__evaMediaSessionActions = {};
    if (navigator.mediaSession && navigator.mediaSession.setActionHandler) {
        var origSetActionHandler = navigator.mediaSession.setActionHandler;
        navigator.mediaSession.setActionHandler = function(action, handler) {
            window.__evaMediaSessionActions[action] = handler;
            try {
                return origSetActionHandler.apply(this, arguments);
            } catch(e) {}
        };
    }

    // 2. Intercept HTMLMediaElement.prototype.pause: Allow in-app pauses while blocking background pauses
    try {
        var originalPause = HTMLMediaElement.prototype.pause;
        HTMLMediaElement.prototype.pause = function() {
            var isRecentUserGesture = (Date.now() - lastUserInteractionTime < 1200);
            if (window.__evaExplicitUserPause === true || isRecentUserGesture) {
                window.__evaExplicitUserPause = true;
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

    // Helper: Deep DOM & MediaSession Track Metadata and Album Art Extractor
    function getTrackMetadata() {
        var title = '';
        var artist = '';
        var artwork = '';

        // 1. Check navigator.mediaSession.metadata
        if (navigator.mediaSession && navigator.mediaSession.metadata) {
            title = navigator.mediaSession.metadata.title || '';
            artist = navigator.mediaSession.metadata.artist || '';
            var arts = navigator.mediaSession.metadata.artwork;
            if (arts && arts.length > 0) {
                artwork = arts[arts.length - 1].src || arts[0].src || '';
            }
        }

        // 2. Check YouTube Music DOM selectors (Player Bar & Layout)
        if (!title || !artist || !artwork) {
            var ytBar = document.querySelector('ytmusic-player-bar, .ytmusic-player-bar, ytmusic-app-layout [slot="player-bar"]');
            if (ytBar) {
                if (!title) {
                    var ytTitleEl = ytBar.querySelector('.title, yt-formatted-string.title, .content-info-wrapper .title, .middle-controls .title, [class*="title"]');
                    if (ytTitleEl) title = ytTitleEl.textContent.trim();
                }
                if (!artist) {
                    var ytArtistEl = ytBar.querySelector('.byline, .subtitle, yt-formatted-string.byline, .content-info-wrapper .byline, [class*="byline"], [class*="subtitle"]');
                    if (ytArtistEl) artist = ytArtistEl.textContent.trim();
                }
                if (!artwork) {
                    var ytImg = ytBar.querySelector('img.image, img#img, .thumbnail img, [class*="thumbnail"] img, img');
                    if (ytImg && ytImg.src && ytImg.src.indexOf('data:') !== 0) {
                        artwork = ytImg.src;
                    }
                }
            }
        }

        // 3. Check Spotify DOM selectors (Now Playing Widget & Footer)
        if (!title || !artist || !artwork) {
            var spWidget = document.querySelector('[data-testid="now-playing-widget"], footer[data-testid="now-playing-bar"], footer');
            if (spWidget) {
                if (!title) {
                    var spTitleEl = spWidget.querySelector('[data-testid="context-item-link"], [data-testid="context-item-info-title"] a, [data-testid="track-info-name"]');
                    if (spTitleEl) title = spTitleEl.textContent.trim();
                }
                if (!artist) {
                    var spArtistEl = spWidget.querySelector('[data-testid="track-info-artists"], [data-testid="context-item-info-subtitles"] a');
                    if (spArtistEl) artist = spArtistEl.textContent.trim();
                }
                if (!artwork) {
                    var spImg = spWidget.querySelector('[data-testid="cover-art-image"], img');
                    if (spImg && spImg.src && spImg.src.indexOf('data:') !== 0) {
                        artwork = spImg.src;
                    }
                }
            }
        }

        // 4. Check YouTube standard DOM selectors
        if (!title || !artist || !artwork) {
            if (!title) {
                var ytMainTitle = document.querySelector('h1.ytd-watch-metadata, .slim-video-metadata-title, h1.title');
                if (ytMainTitle) title = ytMainTitle.textContent.trim();
            }
            if (!artist) {
                var ytChannel = document.querySelector('#upload-info ytd-channel-name a, .slim-owner-channel-name, #channel-name a');
                if (ytChannel) artist = ytChannel.textContent.trim();
            }
            if (!artwork) {
                var metaOg = document.querySelector('meta[property="og:image"]');
                if (metaOg && metaOg.content) {
                    artwork = metaOg.content;
                } else {
                    var linkImg = document.querySelector('link[rel="image_src"]');
                    if (linkImg && linkImg.href) artwork = linkImg.href;
                }
            }
        }

        // 5. Fallback: Parse document title if title is still missing
        if (!title && document.title) {
            var docT = document.title;
            var suffixes = [' - YouTube Music', ' | Spotify', ' - Spotify', ' - YouTube', ' • Spotify'];
            for (var s = 0; s < suffixes.length; s++) {
                docT = docT.replace(suffixes[s], '');
            }
            if (docT.indexOf(' - ') !== -1) {
                var p = docT.split(' - ');
                title = p[0].trim();
                if (!artist && p[1]) artist = p[1].trim();
            } else if (docT.indexOf(' • ') !== -1) {
                var p2 = docT.split(' • ');
                title = p2[0].trim();
                if (!artist && p2[1]) artist = p2[1].trim();
            } else {
                title = docT.trim();
            }
        }

        // Upscale Google/YouTube artwork thumbnail URL to high definition (512x512) for lockscreen
        if (artwork && artwork.indexOf('googleusercontent.com') !== -1) {
            artwork = artwork.replace(/=w\d+-h\d+/, '=w512-h512').replace(/=s\d+/, '=s512');
        }

        return { title: title, artist: artist, artwork: artwork };
    }

    var lastSentTitle = '';
    var lastSentArtist = '';
    var lastSentArtwork = '';

    function checkAndDispatchTrackInfo(force) {
        var meta = getTrackMetadata();
        if (!meta.title && !meta.artist && !meta.artwork) return;

        var changed = (meta.title !== lastSentTitle || meta.artist !== lastSentArtist || meta.artwork !== lastSentArtwork);
        if (changed || force) {
            lastSentTitle = meta.title;
            lastSentArtist = meta.artist;
            lastSentArtwork = meta.artwork;

            if (window.EvaMediaBridge && meta.title) {
                window.EvaMediaBridge.onTrackChanged(meta.title, meta.artist, meta.artwork);
            }
        }
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
                            var artwork = (meta.artwork && meta.artwork.length > 0) ? (meta.artwork[meta.artwork.length - 1].src || meta.artwork[0].src) : '';
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

            window.__evaExplicitUserPause = false;
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
            checkAndDispatchTrackInfo(true);
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

        el.addEventListener('play', function() {
            window.__evaExplicitUserPause = false;
            notifyPlay();
        });
        el.addEventListener('playing', function() {
            window.__evaExplicitUserPause = false;
            notifyPlay();
        });

        // CRITICAL FIX: Only forward pause to Android if user EXPLICITLY paused or physically tapped pause!
        el.addEventListener('pause', function() {
            var isRecentUserGesture = (Date.now() - lastUserInteractionTime < 1200);
            if (window.__evaExplicitUserPause === true || isRecentUserGesture) {
                // User intentionally tapped pause in-app or via notification!
                window.__evaExplicitUserPause = true;
                notifyPause();
            } else {
                // Involuntary background pause by Chromium! Auto-resume immediately!
                setTimeout(function() {
                    if (window.__evaExplicitUserPause !== true) {
                        el.play().catch(function(){});
                    }
                }, 40);
            }
        });

        el.addEventListener('ended', function() {
            notifyPause();
        });

        el.addEventListener('volumechange', function() {
            if (!el.muted && el.volume > 0 && !el.paused) {
                notifyPlay();
            } else if ((el.muted || el.volume === 0) && !el.paused) {
                notifyPause();
            }
        });

        // Throttled timeupdate for seekbar animation & real-time metadata sync
        var lastTimeReport = 0;
        el.addEventListener('timeupdate', function() {
            var now = Date.now();
            if (now - lastTimeReport > 2500 && !el.paused && !el.muted && el.volume > 0) {
                lastTimeReport = now;
                var dur = (el.duration && !isNaN(el.duration) && isFinite(el.duration)) ? el.duration : 0;
                var pos = (el.currentTime && !isNaN(el.currentTime)) ? el.currentTime : 0;
                if (window.EvaMediaBridge) {
                    window.EvaMediaBridge.onPlaybackPosition(window.__evaServiceId, dur, pos);
                }
                checkAndDispatchTrackInfo(false);
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

    // Lightweight 1000ms Keep-Alive Watchdog & Metadata Poller
    setInterval(function() {
        if (window.__evaExplicitUserPause !== true) {
            var v = document.querySelector('video') || document.querySelector('audio');
            if (v && v.currentTime > 0) {
                if (v.paused) {
                    // Auto-resume background stream involuntarily stopped by Chromium
                    v.play().catch(function(){});
                } else {
                    // Keep track title, artist, and album artwork synced in real time
                    checkAndDispatchTrackInfo(false);
                }
            }
        }
    }, 1000);
})();
"""

class EvaMediaBridge(
    private val context: Context,
    private val serviceId: String,
    private val defaultServiceName: String
) {
    private var lastTrack = ""
    private var lastArtwork = ""

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
        if (cleanTitle.isNotBlank() && (cleanTitle != lastTrack || artworkUrl != lastArtwork)) {
            lastTrack = cleanTitle
            lastArtwork = artworkUrl
            EvaMediaCoordinator.onTrackChanged(
                context = context,
                serviceId = serviceId,
                serviceName = defaultServiceName,
                artist = cleanArtist,
                title = cleanTitle,
                artworkUrl = artworkUrl
            )
        }
    }
}

object EvaWebViewPool {
    private const val MAX_ACTIVE_WEBVIEWS = 3
    private val webViews = mutableMapOf<String, WebView>()
    private val lruOrder = mutableListOf<String>()

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

    private fun touchService(serviceId: String) {
        lruOrder.remove(serviceId)
        lruOrder.add(serviceId)
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
        touchService(service.id)

        // Bounded LRU Cache Eviction: keep pool size <= MAX_ACTIVE_WEBVIEWS
        if (!webViews.containsKey(service.id) && webViews.size >= MAX_ACTIVE_WEBVIEWS) {
            val activeAudio = EvaMediaCoordinator.activeAudioServiceId
            val pausedAudio = EvaMediaCoordinator.pausedBackgroundServiceId
            val candidate = lruOrder.firstOrNull { id ->
                id != service.id && id != activeAudio && id != pausedAudio
            }
            if (candidate != null) {
                val evictedView = webViews.remove(candidate)
                lruOrder.remove(candidate)
                evictedView?.let { wv ->
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.stopLoading()
                    wv.destroy()
                }
                EvaDiagnosticsManager.updateActiveWebViewCount(webViews.size)
            }
        }

        return webViews.getOrPut(service.id) {
            val initStart = EvaDiagnosticsManager.recordWebViewInitStart()
            val profile = ServiceWebProfileRegistry.getProfileForService(service)

            val wv = WebView(context).apply {
                tag = service.id
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // High-performance hardware acceleration
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // Chromium Web Core Configuration driven by ServiceWebProfile
                settings.apply {
                    javaScriptEnabled = profile.allowJavaScript
                    domStorageEnabled = profile.allowDomStorage
                    databaseEnabled = true
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
                    mediaPlaybackRequiresUserGesture = profile.mediaPlaybackRequiresUserGesture
                    javaScriptCanOpenWindowsAutomatically = profile.allowPopups

                    // Serve High-Fidelity Chrome Mobile or Desktop Engine per profile
                    userAgentString = profile.resolveUserAgent(CHROME_MOBILE_USER_AGENT, DESKTOP_USER_AGENT)
                }

                // Cookie Persistence with Strict Security
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(profile.allowCookies)
                cookieManager.setAcceptThirdPartyCookies(this, profile.allowThirdPartyCookies)

                // Native Media Track Metadata Bridge
                addJavascriptInterface(
                    EvaMediaBridge(context, service.id, service.name),
                    "EvaMediaBridge"
                )

                // File & Media Downloads Handler powered by EvaDownloadManager
                setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                    if (!profile.allowDownloads) {
                        Toast.makeText(context, "Downloads restricted for ${service.name}", Toast.LENGTH_SHORT).show()
                        return@setDownloadListener
                    }
                    try {
                        EvaDownloadManager.enqueueDownload(
                            context = context,
                            url = url,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimetype,
                            serviceName = service.name
                        )
                        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to process download", Toast.LENGTH_SHORT).show()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress / 100f)
                        if (newProgress > 45) {
                            view?.evaluateJavascript(NATIVE_APP_SHELL_TRANSFORM_SCRIPT, null)
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        if (!title.isNullOrBlank()) {
                            onTitleChanged(title)
                            EvaMediaCoordinator.onDocumentTitleChanged(context, service.id, title)
                        }
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return
                        val reqOrigin = request.origin?.toString() ?: ""
                        if (!profile.isOriginAllowed(reqOrigin)) {
                            request.deny()
                            return
                        }
                        val granted = mutableListOf<String>()
                        for (res in request.resources) {
                            when (res) {
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> if (profile.allowCamera) granted.add(res)
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> if (profile.allowMicrophone) granted.add(res)
                                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> granted.add(res)
                            }
                        }
                        if (granted.isNotEmpty()) {
                            request.grant(granted.toTypedArray())
                        } else {
                            request.deny()
                        }
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
                        if (!profile.allowFileUploads) {
                            filePathCallback?.onReceiveValue(null)
                            return false
                        }
                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        onOpenFileChooserAction?.invoke(filePathCallback, intent)
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    // Renderer crash recovery: prevents host application termination on isolated renderer death
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        EvaDiagnosticsManager.recordRendererCrashRecovered(service.id)
                        Handler(Looper.getMainLooper()).post {
                            try {
                                (view?.parent as? ViewGroup)?.removeView(view)
                                view?.destroy()
                                webViews.remove(service.id)
                                lruOrder.remove(service.id)
                                EvaDiagnosticsManager.updateActiveWebViewCount(webViews.size)
                            } catch (_: Exception) {}
                        }
                        return true
                    }

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

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errorCode = error?.errorCode ?: -1
                            val desc = error?.description?.toString() ?: "Error"
                            EvaDiagnosticsManager.recordPageLoadError(service.id, errorCode, desc)
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let(onUrlChanged)
                        EvaDiagnosticsManager.recordPageLoadStarted(service.id, service.name)
                        view?.evaluateJavascript(NATIVE_APP_SHELL_TRANSFORM_SCRIPT, null)
                        val script = BACKGROUND_AUDIO_SCRIPT.replace("%SERVICE_ID%", service.id)
                        view?.evaluateJavascript(script, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        EvaDiagnosticsManager.recordPageLoadFinished(service.id)
                        cookieManager.flush()
                        view?.evaluateJavascript(NATIVE_APP_SHELL_TRANSFORM_SCRIPT, null)
                        val script = BACKGROUND_AUDIO_SCRIPT.replace("%SERVICE_ID%", service.id)
                        view?.evaluateJavascript(script, null)
                        view?.title?.let { t ->
                            EvaMediaCoordinator.onDocumentTitleChanged(context, service.id, t)
                        }
                    }
                }

                loadUrl(service.url)
            }

            EvaDiagnosticsManager.recordWebViewInitEnd(initStart)
            EvaDiagnosticsManager.updateActiveWebViewCount(webViews.size)
            wv
        }
    }

    /**
     * Executes Play/Pause directly on the Android Main Looper without waiting for view attachment.
     */
    fun setPlayPauseState(serviceId: String?, play: Boolean) {
        val targetId = serviceId ?: EvaMediaCoordinator.activeAudioServiceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = if (play) {
            """
            (function() {
                window.__evaExplicitUserPause = false;
                if (window.__evaMediaSessionActions && typeof window.__evaMediaSessionActions['play'] === 'function') {
                    try { window.__evaMediaSessionActions['play'](); return true; } catch(e) {}
                }
                var v = document.querySelector('video') || document.querySelector('audio');
                if (v && v.paused) { v.play().catch(function(){}); return true; }
                var btn = document.querySelector('#play-pause-button, button[data-testid="control-button-playpause"], .ytp-play-button, button[aria-label*="Play"], [title*="Play"]');
                if (btn) { btn.click(); return true; }
                return false;
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                window.__evaExplicitUserPause = true;
                if (window.__evaMediaSessionActions && typeof window.__evaMediaSessionActions['pause'] === 'function') {
                    try { window.__evaMediaSessionActions['pause'](); return true; } catch(e) {}
                }
                var v = document.querySelector('video') || document.querySelector('audio');
                if (v && !v.paused) { v.pause(); return true; }
                var btn = document.querySelector('#play-pause-button, button[data-testid="control-button-playpause"], .ytp-play-button, button[aria-label*="Pause"], [title*="Pause"]');
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
        val targetId = serviceId ?: EvaMediaCoordinator.activeAudioServiceId ?: activeServiceId
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
        val targetId = serviceId ?: EvaMediaCoordinator.activeAudioServiceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val seconds = positionMs / 1000.0
        val script = """
        (function() {
            if (window.__evaMediaSessionActions && typeof window.__evaMediaSessionActions['seekto'] === 'function') {
                try { window.__evaMediaSessionActions['seekto']({ seekTime: $seconds }); return; } catch(e) {}
            }
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
        val targetId = serviceId ?: EvaMediaCoordinator.activeAudioServiceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = """
        (function() {
            window.__evaExplicitUserPause = false;
            if (window.__evaMediaSessionActions && typeof window.__evaMediaSessionActions['nexttrack'] === 'function') {
                try { window.__evaMediaSessionActions['nexttrack'](); return true; } catch(e) {}
            }
            var nextBtn = document.querySelector(
                'ytmusic-player-bar .next-button, .next-button, button[data-testid="control-button-skip-forward"], .ytp-next-button, button[aria-label*="Next"], [title*="Next"]'
            );
            if (nextBtn) {
                nextBtn.click();
                return true;
            }
            var v = document.querySelector('video') || document.querySelector('audio');
            if (v) {
                v.currentTime = Math.min((v.duration || Infinity), v.currentTime + 10);
                return true;
            }
            return false;
        })();
        """.trimIndent()

        Handler(Looper.getMainLooper()).post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    fun skipPrevious(serviceId: String?) {
        val targetId = serviceId ?: EvaMediaCoordinator.activeAudioServiceId ?: activeServiceId
        val wv = webViews[targetId] ?: webViews.values.firstOrNull() ?: return
        val script = """
        (function() {
            window.__evaExplicitUserPause = false;
            if (window.__evaMediaSessionActions && typeof window.__evaMediaSessionActions['previoustrack'] === 'function') {
                try { window.__evaMediaSessionActions['previoustrack'](); return true; } catch(e) {}
            }
            var prevBtn = document.querySelector(
                'ytmusic-player-bar .previous-button, .previous-button, button[data-testid="control-button-skip-back"], .ytp-prev-button, button[aria-label*="Previous"], [title*="Previous"]'
            );
            if (prevBtn) {
                prevBtn.click();
                return true;
            }
            var v = document.querySelector('video') || document.querySelector('audio');
            if (v) {
                v.currentTime = Math.max(0, v.currentTime - 10);
                return true;
            }
            return false;
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

    fun canGoForward(serviceId: String): Boolean {
        return webViews[serviceId]?.canGoForward() == true
    }

    fun goForward(serviceId: String) {
        Handler(Looper.getMainLooper()).post {
            webViews[serviceId]?.goForward()
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
                lruOrder.clear()
                EvaDiagnosticsManager.updateActiveWebViewCount(0)

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
            lruOrder.clear()
            EvaDiagnosticsManager.updateActiveWebViewCount(0)
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
                val switchStart = EvaDiagnosticsManager.recordServiceSwitchStart()
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
                EvaDiagnosticsManager.recordServiceSwitchEnd(switchStart)
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
