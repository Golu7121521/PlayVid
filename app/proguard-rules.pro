# ==========================================
# Fix for R8 Missing Classes & Warnings
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
# Mozilla Rhino / NewPipeExtractor Optimizations
# ==========================================
# Rhino ke pure codebase ko keep karne ke bajay zaroori classes rakhein
-keep class org.mozilla.javascript.** {
    public *;
}
-keep class org.mozilla.classfile.ClassFileWriter

# NewPipe patterns ko keep karein (reflection se access hote hain)
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

# ==========================================
# Media3 Optimization (Size Kam Karne Ke Liye)
# ==========================================
# Pure Media3 package ko blindly keep mat karein
-keepclassmembers class androidx.media3.** {
    @androidx.media3.common.util.UnstableApi *;
}

# ==========================================
# General Shrinking & Optimization
# ==========================================
-repackageclasses ''
-allowaccessmodification
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
