# EVA 3.0 — Architectural Specification

**Version:** 3.0.0-PROD  
**Target:** Android 8.0 (API 26) through Android 16 (API 36)  
**Architecture Pattern:** Clean MVI / Unidirectional Data Flow with Centralized WebKit Engine

---

## 1. Architectural Principles

1. **Native-First Fidelity:** Web experiences must behave like native Android applications (responsive touch physics, overscroll containment, zero browser artifacts, edge-to-edge rendering).
2. **Bounded Resource Footprint:** Memory usage must be capped regardless of how many services the user opens. Inactive pages must be suspended to conserve battery and CPU.
3. **Defense-in-Depth Security:** Never trust arbitrary web origins. Enforce strict HTTPS, allowed origin validation, granular camera/mic permissions, and protected cookie persistence.
4. **Resilient Failure Recovery:** Handle network drops, SSL errors, and Chromium renderer process crashes gracefully with automatic recovery.
5. **Zero Dependency Bloat:** Rely exclusively on standard Android SDK, AndroidX WebKit, Jetpack Compose, and Kotlin Coroutines. No heavy third-party web frameworks.

---

## 2. High-Level Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│  HomeScreen  │  AppContainerScreen  │  FloatingPillDock  │  Diagnostics│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (Commands / State Flows)
┌───────────────────────────────────▼────────────────────────────────────┐
│                       EVA Web Engine 3.0 Layer                         │
│  ┌───────────────────────┐  ┌────────────────────────────────────────┐ │
│  │ ServiceWebProfile     │  │ WebViewInstancePool                    │ │
│  │ - Allowed Origins     │  │ - Bounded LRU Pool (Max 3-4 hot)       │ │
│  │ - Granular Perms      │  │ - Inactive Page Suspension (onPause)   │ │
│  │ - Cookie Policies     │  │ - onRenderProcessGone Crash Recovery   │ │
│  │ - UA & Storage Flags  │  │ - Memory Trim (ComponentCallbacks2)    │ │
│  └───────────────────────┘  └────────────────────────────────────────┘ │
└───────────────────┬───────────────────────────────┬────────────────────┘
                    │                               │
┌───────────────────▼──────────────┐ ┌──────────────▼────────────────────┐
│    Media & Audio Coordinator     │ │     Native File & Downloads       │
│  - MediaSessionCompat            │ │  - Android DownloadManager        │
│  - Native AudioFocusRequest      │ │  - SAF File Chooser + Camera      │
│  - Foreground Playback Service   │ │  - FileProvider Secure Sharing    │
└──────────────────────────────────┘ └───────────────────────────────────┘
```

---

## 3. Core Engine Components

### 3.1 `ServiceWebProfile` & `ServiceWebProfileRegistry`
Defines an immutable configuration contract for each service:
```kotlin
data class ServiceWebProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val allowedOrigins: Set<String>,
    val userAgentType: UserAgentType = UserAgentType.CHROME_MOBILE,
    val allowJavaScript: Boolean = true,
    val allowDomStorage: Boolean = true,
    val allowThirdPartyCookies: Boolean = true,
    val allowCamera: Boolean = false,
    val allowMicrophone: Boolean = false,
    val allowDownloads: Boolean = true,
    val allowPopups: Boolean = false,
    val isVideoService: Boolean = false,
    val isAudioService: Boolean = false
)
```

### 3.2 `WebViewInstancePool` (Bounded LRU & Lifecycle Aware)
- **Pool Sizing:** Capped at 3 active WebViews in RAM. When a 4th service is opened, the least recently used inactive instance is safely suspended, its bundle state saved, and its view recycled.
- **Inactive Suspension:**
  - Active tab: `onResume()`, `resumeTimers()`.
  - Inactive tab: `onPause()`, `pauseTimers()`.
- **Renderer Crash Resilience:**
  ```kotlin
  override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
      // 1. Recycle crashed instance
      // 2. Clear from pool
      // 3. Re-instantiate clean WebView on main thread
      // 4. Reload last safe URL
      return true // Prevents host app termination
  }
  ```
- **OS Memory Trimming:** Implements `ComponentCallbacks2` to release cache and drop cold instances upon `TRIM_MEMORY_RUNNING_CRITICAL` or `TRIM_MEMORY_COMPLETE`.

---

## 4. Native Experience Enhancements

### 4.1 Navigation & History
- Multi-tier back navigation: WebView back stack $\rightarrow$ Recent Services stack $\rightarrow$ Home Hub.
- Forward navigation support with visual forward action.
- Pull-to-refresh: Integrated Compose `PullToRefresh` layout wrapping the active canvas.

### 4.2 Network & Offline State
- `ConnectivityManager.NetworkCallback` monitors real-time internet status.
- Displays a native, themed offline screen with instant retry action when disconnected.

---

## 5. Security & Privacy Safeguards

1. **Origin Verification:** Navigation requests validate `request.url.host` against `profile.allowedOrigins`. Unknown external domains are prevented from hijacking the authenticated sandbox.
2. **Selective Permissions:** Camera and microphone are granted **only** if `profile.allowCamera` or `profile.allowMicrophone` is `true` AND the requesting URL matches allowed origins.
3. **No SSL Bypass:** `onReceivedSslError` always executes `handler?.cancel()`.
4. **Secure File Chooser:** Disallows arbitrary file system paths; enforces Android Storage Access Framework (SAF) and `FileProvider`.

---

## 6. Observability & Developer Diagnostics

The `EvaDiagnosticsManager` records real operational telemetry:
- `webViewInitDurationMs`: Time to instantiate Chromium core.
- `pageLoadDurationMs`: Time from `onPageStarted` to `onPageFinished`.
- `activeWebViewCount`: Real count of instances in RAM.
- `rendererCrashCount`: Total recovered crashes.
- `memoryUsageMb`: Runtime heap and native allocation metrics.
- Accessible via an internal developer diagnostics dialog on the Home Screen.
