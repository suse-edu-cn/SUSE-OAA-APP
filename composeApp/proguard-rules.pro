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

# =========================
# LiteRT-LM 端侧推理引擎
# =========================
# com.google.ai.edge.litertlm.* 的类名和成员在运行时由框架自身通过反射访问，
# 不能被 R8 重命名或移除。
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# =========================
# LiteRtNativePreloader JNI 桥接
# =========================
# preloadLiteRt() 是 native 方法，JNI 通过固定的 Java 签名名称绑定到 C 层。
# 若类名或方法名被混淆，System.loadLibrary() 之后的 JNI 符号查找会失败。
-keep class com.suseoaa.projectoaa.util.LiteRtNativePreloader {
    *;
}

# =========================
# CampusAiEngine（端侧推理入口 expect/actual）
# =========================
# 跨平台 expect/actual 对象，在 ViewModel 中通过全限定类名使用，
# 混淆后 Koin 和 Kotlin 反射均无法正确定位。
-keep class com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine { *; }

# =========================
# AiToolEngine / AcademicToolRouter（学业 AI 工具层）
# =========================
# AiToolEngine 在 ViewModel 层由 Koin 注入，AcademicToolRouter 由 AiToolEngine 直接 new。
# R8 会将没有被直接引用为类型的内部构造重命名，导致运行时异常。
-keep class com.suseoaa.projectoaa.shared.domain.engine.AiToolEngine { *; }
-keep class com.suseoaa.projectoaa.shared.domain.engine.AcademicToolRouter { *; }
-keep class com.suseoaa.projectoaa.shared.domain.engine.AcademicToolIntent { *; }
-keep class com.suseoaa.projectoaa.shared.domain.engine.AcademicToolRouteResult { *; }
-keep class com.suseoaa.projectoaa.shared.domain.engine.AcademicCalculator { *; }
