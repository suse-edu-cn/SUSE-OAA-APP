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
