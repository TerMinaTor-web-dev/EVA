# ============================================================================
# EVA Security Hardening & Obfuscation ProGuard / R8 Rules
# ============================================================================

# 1. Strip All Debug Logging in Production
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 2. Prevent Reverse Engineering of Core Models & Deserialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 3. WebKit & JavaScript Interface Safety
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 4. Kotlin Serialization & Compose Stability
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer();
}

# 5. Security & Service Keep Rules
-keep class com.example.eva.service.EvaMediaPlaybackService { *; }
-keep class com.example.eva.ui.components.EvaWebViewPool { *; }
-keep class com.example.eva.security.** { *; }
-keep class com.example.eva.BuildConfig { *; }
