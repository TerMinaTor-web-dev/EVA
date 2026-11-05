# Contributing to EVA

Thank you for your interest in improving EVA! We welcome contributions to make EVA faster, more secure, and feature-rich.

---

## 🛠️ Development Setup

1. Fork and clone the repository.
2. Open the project in **Android Studio** or develop directly via terminal with `./gradlew`.
3. Verify that your system has **JDK 17 or 21** installed and `ANDROID_HOME` pointing to an Android SDK with Platform `android-36`.

---

## 📋 Code Standards

* **100% Jetpack Compose**: All new UI components must be written with Compose Material 3.
* **Fluid 120 FPS Animations**: Use Compose `spring` physics and hardware acceleration.
* **Security First**: 
  * Never bypass SSL verification.
  * Never enable `allowFileAccess` on WebViews without strict origin scoping.
  * Enforce safe URL filtering on all navigation requests.
* **Zero Bloat**: Keep external dependencies to an absolute minimum to keep the APK small and fast.

---

## 🚀 Submitting a Pull Request

1. Create a descriptive feature branch: `git checkout -b feature/your-feature-name`.
2. Ensure existing tests pass: `./gradlew test`.
3. Ensure the project compiles cleanly: `./gradlew assembleDebug`.
4. Submit a Pull Request with a clear description and screenshots of any UI changes.
