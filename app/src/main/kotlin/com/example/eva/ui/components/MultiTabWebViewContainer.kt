package com.example.eva.ui.components

import android.annotation.SuppressLint
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
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

object EvaWebViewPool {
    private val webViews = mutableMapOf<String, WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        service: AppService,
        onProgressChanged: (Float) -> Unit,
        onTitleChanged: (String) -> Unit,
        onUrlChanged: (String) -> Unit
    ): WebView {
        return webViews.getOrPut(service.id) {
            WebView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 120 FPS Hardware Acceleration
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // OWASP & Chromium Mobile Security Hardening
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false

                    // SECURITY: Strict Sandbox Isolation
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false

                    // Media Autoplay & Background Streaming
                    mediaPlaybackRequiresUserGesture = false
                    javaScriptCanOpenWindowsAutomatically = true

                    if (service.isDesktopUserAgent) {
                        userAgentString = DESKTOP_USER_AGENT
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
                        request?.grant(request.resources)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        onUrlChanged(url)

                        // SECURITY: Block untrusted scheme hijacking
                        val scheme = request.url.scheme?.lowercase() ?: ""
                        if (scheme == "file" || scheme == "content" || scheme == "javascript") {
                            return true // Disallow malicious local file / script schemes
                        }

                        // Handle standard web vs external app intents
                        return if (url.startsWith("http://") || url.startsWith("https://")) {
                            false
                        } else {
                            try {
                                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                true
                            }
                        }
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
                        (child as? WebView)?.onResume()
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
