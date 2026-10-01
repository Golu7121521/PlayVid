-keep class androidx.media3.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

## NewPipeExtractor & Rhino
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.**
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn com.google.re2j.**

# Android me java.beans missing hota hai, isliye ise ignore karein
-dontwarn java.beans.**
