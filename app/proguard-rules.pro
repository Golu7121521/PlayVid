# ==========================================
# 1. Fix Missing Classes (Rhino / Desugar Warnings)
# ==========================================
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn com.google.re2j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.mozilla.javascript.tools.**

# ==========================================
# 2. NewPipeExtractor & Mozilla Rhino (Shrink-friendly)
# ==========================================
# Poora library keep karne ke bajay public API aur classfile bytecode writer rakhein
-keep class org.mozilla.javascript.** {
    public *;
}
-keep class org.mozilla.classfile.ClassFileWriter

# NewPipe timeago string parsers reflection use karte hain
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

# ==========================================
# 3. Media3 Optimizations
# ==========================================
# '-keep class androidx.media3.** { *; }' KO HATA DIYA HAI.
# Sirf reflection aur core services ke members ko keep karein:
-keepclassmembers class androidx.media3.** {
    @androidx.media3.common.util.UnstableApi *;
}
-keep class androidx.media3.session.MediaSessionService { *; }

# ==========================================
# 4. Aggressive Size Shrinking Rules
# ==========================================
# Class paths ko flatten karke empty package me daalna (DEX size reduce karta hai)
-repackageclasses ''
-allowaccessmodification

# Log calls ko release APK se strip karna (Performance + Size dono sudhrega)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Kotlin metadata ko strip karein
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
}
