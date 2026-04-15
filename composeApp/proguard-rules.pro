# Project-specific R8/ProGuard rules.
# Keep this file minimal and add app-specific keep rules only when needed.

# Keep generated serializers for kotlinx.serialization.
-keepclassmembers class ** {
    *** Companion;
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$$serializer {
    *;
}
-keepnames class **$$serializer

# Keep source/debug metadata useful for crash analysis.
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,EnclosingMethod

# WebView JS bridge methods are invoked reflectively by name from JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep check-in models to avoid release-only serialization/obfuscation issues.
-keep class com.suseoaa.projectoaa.shared.domain.model.checkin.** { *; }

# Keep OCR pipeline classes used by check-in auto login.
-keep class com.suseoaa.projectoaa.util.CaptchaOcrRecognizer { *; }
-keep class com.suseoaa.projectoaa.util.DdddOcrRecognizer { *; }
-keep class com.google.mlkit.** { *; }
-keep class ai.onnxruntime.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn ai.onnxruntime.**
