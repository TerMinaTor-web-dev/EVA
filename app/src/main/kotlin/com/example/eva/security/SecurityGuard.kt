package com.example.eva.security

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.view.WindowManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Socket
import java.security.MessageDigest

/**
 * Enterprise-grade security verification and runtime defense engine for EVA Super-App.
 * Protects against root exploits, dynamic instrumentation (Frida/Xposed), debuggers,
 * tapjacking/overlay attacks, and APK tampering.
 */
object SecurityGuard {

    private const val OFFICIAL_RELEASE_SHA256 =
        "A7:18:65:E2:53:52:D6:AD:A9:30:AE:E6:53:75:EC:A5:EE:94:18:EC:F3:A9:1B:1E:29:8B:D2:9E:E5:4D:6B:62"

    private val ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    private val ROOT_PACKAGES = arrayOf(
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.topjohnwu.magisk"
    )

    data class SecurityStatus(
        val isRooted: Boolean,
        val isDebuggerAttached: Boolean,
        val isHookingDetected: Boolean,
        val isEmulator: Boolean,
        val isTampered: Boolean,
        val issues: List<String>
    ) {
        val isCompromised: Boolean
            get() = isRooted || isDebuggerAttached || isHookingDetected || isTampered
    }

    /**
     * Conducts a non-destructive integrity audit across the operating environment.
     */
    fun performSecurityAudit(context: Context): SecurityStatus {
        val issues = mutableListOf<String>()

        val rooted = checkRoot(context, issues)
        val debugged = checkDebugging(context, issues)
        val hooked = checkHookingFrameworks(issues)
        val emulator = checkEmulator(issues)
        val tampered = checkSignature(context, issues)

        return SecurityStatus(
            isRooted = rooted,
            isDebuggerAttached = debugged,
            isHookingDetected = hooked,
            isEmulator = emulator,
            isTampered = tampered,
            issues = issues
        )
    }

    /**
     * Hardens the window against tapjacking, clickjacking, and malicious floating overlays.
     */
    fun applyAntiTapjacking(activity: Activity) {
        try {
            // Android 12+ (API 31+): Hide non-system overlays when app is focused
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.window.setHideOverlayWindows(true)
            }
            // Enforce touch filtration on obscured window decor
            activity.window.decorView.filterTouchesWhenObscured = true
        } catch (_: Exception) {}
    }

    /**
     * Configures Android FLAG_SECURE to prevent screen snooping in Recents preview and screen captures.
     */
    fun setScreenCaptureProtection(activity: Activity, secure: Boolean) {
        try {
            if (secure) {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        } catch (_: Exception) {}
    }

    private fun checkRoot(context: Context, issues: MutableList<String>): Boolean {
        var rooted = false

        // 1. Check su binary paths
        for (path in ROOT_PATHS) {
            try {
                if (File(path).exists()) {
                    issues.add("Root binary detected at: $path")
                    rooted = true
                    break
                }
            } catch (_: Exception) {}
        }

        // 2. Check Test-Keys build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            issues.add("OS compiled with non-production test-keys")
            rooted = true
        }

        // 3. Check Known Root Manager Packages
        val pm = context.packageManager
        for (pkg in ROOT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                issues.add("Root management package installed: $pkg")
                rooted = true
                break
            } catch (_: PackageManager.NameNotFoundException) {
                // Expected on clean devices
            } catch (_: Exception) {}
        }

        return rooted
    }

    private fun checkDebugging(context: Context, issues: MutableList<String>): Boolean {
        var debugged = false

        // 1. Hardware/OS Debugger Attached
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            issues.add("Active debugger attached to application process")
            debugged = true
        }

        // 2. Debuggable Flag Check (should only be active during local dev)
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable && !com.example.eva.BuildConfig.DEBUG) {
            issues.add("Release build marked debuggable")
            debugged = true
        }

        // 3. Linux TracerPid Check (/proc/self/status)
        try {
            BufferedReader(FileReader("/proc/self/status")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("TracerPid:") == true) {
                        val tracerPid = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                        if (tracerPid > 0) {
                            issues.add("Process traced by external PID: $tracerPid")
                            debugged = true
                        }
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        return debugged
    }

    private fun checkHookingFrameworks(issues: MutableList<String>): Boolean {
        var hooked = false

        // 1. Memory Maps scan for Frida / Xposed / Substrate agent libraries
        try {
            BufferedReader(FileReader("/proc/self/maps")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val lower = line?.lowercase() ?: continue
                    if (lower.contains("frida-agent") || lower.contains("xposed") || lower.contains("substrate")) {
                        issues.add("Dynamic instrumentation agent detected in process memory")
                        hooked = true
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Check Frida default server port (27042)
        try {
            val socket = Socket("127.0.0.1", 27042)
            socket.close()
            issues.add("Frida server listening on localhost:27042")
            hooked = true
        } catch (_: Exception) {
            // Port closed = clean
        }

        return hooked
    }

    private fun checkEmulator(issues: MutableList<String>): Boolean {
        val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)

        if (isEmulator) {
            issues.add("Virtual / Emulator environment detected")
        }
        return isEmulator
    }

    private fun checkSignature(context: Context, issues: MutableList<String>): Boolean {
        // Only enforce in release mode
        if (com.example.eva.BuildConfig.DEBUG) return false

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                issues.add("Application signature verification failed: empty cert")
                return true
            }

            val md = MessageDigest.getInstance("SHA-256")
            val certBytes = signatures[0].toByteArray()
            val digest = md.digest(certBytes)
            val currentSha256 = digest.joinToString(":") { String.format("%02X", it) }

            if (!currentSha256.equals(OFFICIAL_RELEASE_SHA256, ignoreCase = true)) {
                issues.add("APK signature altered or repackaged (Current: $currentSha256)")
                return true
            }
        } catch (e: Exception) {
            issues.add("Signature verification exception: ${e.message}")
        }
        return false
    }
}
