# ProGuard Rules for Reddit Mail Link Shim
# Applied during release builds (app/build.gradle.kts:24)
# Purpose: Prevent code stripping/obfuscation of runtime-required classes

# OkHttp HTTP Client (com.squareup.okhttp3:okhttp:4.12.0)
# Required: Reflection-based protocol negotiation, platform detection, certificate pinning
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Coroutines (org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3)
# Required: Coroutine intrinsics, continuation serialization, debug agent
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# AndroidX Lifecycle (androidx.lifecycle:lifecycle-runtime-ktx:2.6.2)
# Required: LifecycleScope launch points, observer pattern reflection
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Application Classes (com.Wffv9FNa.redditshim.*)
# Keep all app classes for debugging stack traces in crash reports
# Maintains line number accuracy in stack traces for issue reports
-keep class com.Wffv9FNa.redditshim.** { *; }

# Keep main activity explicitly (intent filter target from AndroidManifest.xml)
-keep class com.Wffv9FNa.redditshim.MailClickShimActivity {
    public <methods>;
}

# Attribute preservation for debugging and reflection
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions,InnerClasses,Signature
-renamesourcefileattribute SourceFile

# Remove logging in release builds
# Strips Log.d(), Log.v(), Log.i() calls to reduce APK size and prevent URL leakage
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# Optimization settings
# Aggressive optimization: reduces APK size from ~2.7MB (debug) to ~1.5MB (release)
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
