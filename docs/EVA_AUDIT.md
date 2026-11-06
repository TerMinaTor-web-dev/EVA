# EVA 3.0 — Comprehensive Engineering Audit

**Date:** November 2026  
**Target:** EVA Super-App Android Codebase (`com.example.eva`)  
**Scope:** Architectural Integrity, WebView Engine, Security, Media, Storage, and Performance.

---

## 1. Executive Summary

EVA is an Android super-app built with Kotlin 2.2, Jetpack Compose Material 3, and AndroidX WebKit. Its core value proposition is unifying essential web experiences (YouTube, YouTube Music, Instagram, Spotify, LinkedIn, Facebook, X/Twitter, Reddit) into a single, cohesive, lightweight client (~1.5 MB APK footprint) with integrated cloud quota management, local flash tiering, background audio playback, and custom gesture navigation.

This audit evaluates the codebase against the requirements of **EVA Web Experience 3.0**, identifying critical architectural strengths, design bottlenecks, security gaps, and performance risks.

---

## 2. Component-by-Component Audit

### 2.1 Entry Point & Lifecycle: `MainActivity.kt`
- **Responsibilities:**
  - Single-activity container hosting Compose `MainNavigation()`.
  - Performs environment security audit via `SecurityGuard.performSecurityAudit()`.
  - Audits runtime permissions via `EvaPermissionManager.hasCorePermissions()`.
  - Adaptive display refresh rate detection up to 120Hz via `display?.supportedModes`.
  - Context-aware Picture-in-Picture (`onUserLeaveHint`) delegating to `EvaPipDecisionEngine`.
  - Flushes `CookieManager` in `onPause()` and `onStop()`.
- **Gaps & Risks Identified:**
  - Does not propagate Android OS memory warnings (`onTrimMemory()`) to the WebView pool.
  - Does not manage `WebView.onPause()` / `WebView.onResume()` across activity transitions.
  - Does not handle configuration changes or state restoration if the process is killed in background.

---

### 2.2 Navigation & Screens
- **`Navigation.kt`**:
  - Uses `androidx.navigation3` backstack with routes `OnboardingRoute`, `HomeRoute`, `AppRoute`.
  - Clean and simple; handles update dialogs and AI copilot sheets globally.
- **`HomeScreen.kt`**:
  - Displays user profile, cloud storage quota card, engine status card, universal omnibox, category chips, and a 3-column `LazyVerticalGrid` of `CrystalBubble` services.
  - Hosts dual-tab dialog (Tab 0: 15 GB Cloud Storage, Tab 1: All-in-One Super-App & Flash Savings).
  - *Issues Identified:*
    - Hardcoded label "120 FPS All-in-One" rather than real measured performance metrics.
    - Multiple redundant remember blocks and heavy UI re-evaluations during text entry in omnibox.
- **`AppContainerScreen.kt`**:
  - Hosts active service in `MultiTabWebViewContainer`, edge-to-edge full-screen video player, and dedicated `FloatingPillDock`.
  - Manages recent services history stack (`recentHistory`, `historyIndex`).
  - *Issues Identified:*
    - Lacks native pull-to-refresh mechanism.
    - Lacks forward web history navigation (only back navigation handled via `BackHandler`).
    - Lacks a native web address/search bar or tab switcher inside the browsing canvas.
    - Missing custom error/offline states when network drops.

---

### 2.3 WebView Engine & Pooling: `MultiTabWebViewContainer.kt` & `EvaWebViewPool`
- **Implementation:**
  - `EvaWebViewPool` is an in-memory `object` storing active instances in `private val webViews = mutableMapOf<String, WebView>()`.
  - Reuses WebViews by service ID (`webViews.getOrPut(service.id)`).
  - Injects `BACKGROUND_AUDIO_SCRIPT` and `NATIVE_APP_SHELL_TRANSFORM_SCRIPT` on `onPageStarted` and `onPageFinished`.
  - Exposes `EvaMediaBridge` JavascriptInterface for media position, duration, and track metadata extraction.
- **Critical Architectural & Stability Gaps:**
  1. **Unbounded Memory Retention:** `webViews` stores every opened service indefinitely. Opening 8 services retains 8 distinct Chromium renderers in memory simultaneously (~500MB+ RAM), risking Out-Of-Memory (OOM) crashes on 3GB-4GB RAM devices.
  2. **Missing Inactive Page Suspension:** Detached WebViews are never paused via `onPause()` or `pauseTimers()`, allowing background timers, WebGL animations, and DOM scripts to consume battery and CPU cycles.
  3. **No Crash Recovery (`onRenderProcessGone`):** If Android's OS terminates an isolated Chromium render process under memory pressure, `WebViewClient.onRenderProcessGone()` is unhandled, crashing the host application immediately.
  4. **Blanket Permission Granting (Security Risk):** `WebChromeClient.onPermissionRequest` unconditionally grants camera and microphone permissions (`request?.grant(request.resources)`) to ANY origin without validating against allowed domains.
  5. **Lack of Per-Service Profiles:** Settings are globally applied to all WebViews. Origin allowlists, custom headers, cookies, popups, and JavaScript toggles are not configured per service.
  6. **Download Manager Limitations:** `DownloadManager.Request` lacks support for `blob:` URLs, `data:` URIs, progress feedback, pause/resume, and file sharing via FileProvider.
  7. **Unused File:** `EvaWebView.kt` is an obsolete standalone component completely unreferenced in the project.

---

### 2.4 Service Catalog & Web Profiles: `AppServiceCatalog.kt`
- **Implementation:**
  - Defines 8 core services: `instagram`, `youtube`, `ytmusic`, `spotify`, `linkedin`, `facebook`, `twitter`, `reddit`.
  - Contains basic metadata: ID, Name, URL, Category, Colors, Description, `isDesktopUserAgent`.
- **Gaps:**
  - Lacks allowed navigation origin domains (e.g. `youtube.com`, `accounts.google.com`, `googlevideo.com`).
  - Lacks cookie policies (third-party cookies enabled indiscriminately across all services).
  - Lacks granular permission flags (which services should be permitted to access camera, mic, location).
  - Lacks custom headers and popup handling configuration.

---

### 2.5 Media Ecosystem: `EvaMediaPlaybackService.kt`, `EvaMediaCoordinator.kt`, `EvaPipDecisionEngine.kt`
- **Implementation:**
  - Uses `MediaSessionCompat` and `androidx.media.app.NotificationCompat.MediaStyle` to render lockscreen and notification media controls.
  - Scrapes metadata (title, artist, artwork) via `BACKGROUND_AUDIO_SCRIPT` and DOM selectors.
  - Implements multi-app audio focus arbitration in `EvaMediaCoordinator` (pauses background music if video starts playing).
  - `EvaPipDecisionEngine` allows PiP for video apps while suppressing blank PiP windows for audio/social apps.
- **Gaps:**
  - Does not manage native Android `AudioManager.requestAudioFocus()` natively; relies purely on JavaScript pausing.
  - Notification artwork downloads run on unbounded CoroutineScope without cancellation guards.

---

### 2.6 Storage & Persistence: `EvaCloudStorageManager.kt`, `EvaHybridStorageEngine.kt`, `UserSessionManager.kt`
- **Implementation:**
  - `UserSessionManager`: Encrypts/obfuscates credentials and preferences using Base64.
  - `EvaCloudStorageManager`: Enforces 15 GB total / 9+ GB free quota rules; provisions virtual vault hierarchy (`/EVA_CloudSpace/Sessions/`, `/Media/`, `/Archives/`).
  - `EvaHybridStorageEngine`: Local flash directories (`eva_vault/sessions`, `eva_vault/archives`, `eva_media`) capped at 250 MB; calculates estimated native app space saved (~2.4 GB).
- **Gaps:**
  - Session tokens and cookies are stored locally via Android's `CookieManager`; cloud sync is currently a checkpoint metadata tracker rather than a live state serializer.
  - Storage savings metric is an analytical estimate based on native APK package sizes rather than live differential telemetry.

---

### 2.7 Build Configuration & Secrets: `app/build.gradle.kts`
- **Issues Identified:**
  - **Exposed Keystore Passwords:** `storePassword = "evasuperapp2026"` and `keyPassword = "evasuperapp2026"` are committed in plaintext inside `build.gradle.kts`.
  - Must be refactored to read from `local.properties` or environment variables with fallback for local builds.

---

## 3. High-Priority Remediation Matrix

| Area | Issue | Severity | Target Phase |
| :--- | :--- | :--- | :--- |
| **Engine** | Unbounded WebView map causing memory bloat | High | Phase 1 |
| **Engine** | Missing `onRenderProcessGone` crash handler | Critical | Phase 1 |
| **Engine** | Inactive pages running timers in background | High | Phase 1 |
| **Security** | Blanket permission grant for Camera/Mic | High | Phase 2, Phase 9 |
| **Profiles** | Scattered service settings; no allowed origin lists | Medium | Phase 2 |
| **UX** | Missing pull-to-refresh and forward navigation | Medium | Phase 3 |
| **UX** | Missing custom offline and error recovery states | Medium | Phase 3, Phase 7 |
| **Files** | Incomplete download handling (no progress, no blob support) | Medium | Phase 4 |
| **Media** | No native Android AudioFocus integration | Medium | Phase 5 |
| **Auth** | No clear cookie isolation / third-party cookie policy | Medium | Phase 6 |
| **Diagnostics** | Static marketing labels instead of real telemetry | Medium | Phase 8 |
| **Security** | Plaintext keystore password in `build.gradle.kts` | High | Phase 10 |
