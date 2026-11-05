# EVA • One-For-All Super-App

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="128" height="128" alt="EVA Logo" />
</p>

<p align="center">
  <b>The Unified Digital Life Engine for Android</b><br>
  Replace 10+ battery-draining apps with a single, high-performance, sandboxed 120 FPS super-engine.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF?style=flat&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Security-OWASP%20Hardened-green?style=flat" alt="Security" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
</p>

---

## 🌟 The Vision

Modern mobile operating systems force users to install dozens of heavy, background-churning applications that consume gigabytes of storage, drain battery through persistent telemetry, and clutter the device.

**EVA** unifies your essential digital experiences—**Instagram, WhatsApp Web, YouTube, YouTube Music, Spotify, LinkedIn, Facebook, X (Twitter), Reddit, and any custom web service**—into one cohesive, ultra-responsive native container with:
- **0ms Instant App Switching**: Multi-tab in-memory pooling with zero network reloading between tabs.
- **Micro Gesture Dock**: A sleek floating home bar enabling horizontal swipe gesture cycling between services and tap-to-expand taskbar.
- **Uninterrupted Background Playback**: Foreground media service with persistent notification keeping Spotify, YouTube Music, and podcast streams alive when the screen turns off.
- **Picture-in-Picture (PiP)**: Native Android PiP mode when multitasking.
- **Ambient AI Co-Pilot**: Integrated Gemini-powered assistant to summarize pages, draft posts, and analyze trends.
- **Zero Tracking Sandbox**: Strict security policies preventing cross-site local data extraction.

---

## 🏗️ Architecture

EVA follows Google's official modern Android architecture blueprints:

```
                        ┌────────────────────────┐
                        │     EVA MainActivity   │
                        │ (PiP & Multi-Window)   │
                        └───────────┬────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            ▼                                               ▼
┌─────────────────────────┐                     ┌─────────────────────────┐
│     Home Hub Screen     │                     │   App Container Screen  │
│ • Dual-Tone Atmosphere  │                     │ • In-Memory WebViewPool │
│ • Crystal Bubbles       │                     │ • Micro Floating Dock   │
│ • Universal Omnibar     │                     │ • EVA AI Copilot Sheet  │
└─────────────────────────┘                     └───────────┬─────────────┘
                                                            │
                                                ┌───────────▼─────────────┐
                                                │ EvaMediaPlaybackService │
                                                │ (Background Audio Keep) │
                                                └─────────────────────────┘
```

---

## 🛡️ Security & Privacy Hardening

EVA is hardened against real-world mobile vulnerabilities in compliance with OWASP Mobile Top 10:

1. **Strict Sandbox Isolation**:
   - `allowFileAccess = false`, `allowContentAccess = false`.
   - `allowFileAccessFromFileURLs = false` and `allowUniversalAccessFromFileURLs = false` prevent arbitrary file read attacks.
2. **Encrypted Network Security**:
   - Enforces `cleartextTrafficPermitted="false"` across all external endpoints.
   - Strict SSL Verification: Rejects untrusted/MITM certificates (`handler.cancel()`).
3. **No Unsafe JavaScript Interfaces**:
   - Eliminates reflective code injection by exposing zero unsafe `@JavascriptInterface` bridges.
4. **Scheme Hijack Defense**:
   - Navigation filter blocks `javascript:`, `file:`, and `content:` URL executions.
5. **Session Isolation & Secure Storage**:
   - Local device storage rules configured with `android:allowBackup="false"` to prevent unauthorized physical extraction.

---

## 🚀 Building from Source

### Prerequisites
- **JDK 17 or 21** (Adoptium / OpenJDK recommended)
- **Android SDK** with Platform `android-36` and Build Tools `35.0.0+`
- Environment variables: `JAVA_HOME` and `ANDROID_HOME` configured.

### Commands

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/eva.git
   cd eva
   ```

2. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The output APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Install to Connected Device:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 License

```
Copyright 2026 EVA Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
