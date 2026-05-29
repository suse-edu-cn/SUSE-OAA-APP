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

# Keep ALL network/domain models to avoid release-only serialization/obfuscation issues.
-keep class com.suseoaa.projectoaa.shared.domain.model.** { *; }

# Keep ViewModels for Koin reflection
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep OCR pipeline classes used by check-in auto login.
-keep class com.suseoaa.projectoaa.util.CaptchaOcrRecognizer { *; }
-keep class com.suseoaa.projectoaa.util.DdddOcrRecognizer { *; }
-keep class com.google.mlkit.** { *; }
-keep class ai.onnxruntime.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn ai.onnxruntime.**

# Keep Room databases (fixes WorkManager / Room crash in Release)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class **_Impl {
    <init>();
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}
-keep class androidx.work.impl.** { *; }
