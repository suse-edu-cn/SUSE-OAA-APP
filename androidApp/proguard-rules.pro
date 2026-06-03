# Keep app-specific ProGuard rules here.

# =========================
# Glance Widgets
# =========================
# Keep all widget receivers and widgets so they are not stripped or renamed.
# Glance uses class names to map widgets, and Android OS instantiates receivers via reflection.
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.suseoaa.projectoaa.composeapp.widget.** { *; }
-keep class androidx.glance.** { *; }

# Keep WorkManager and Room completely to prevent InitializationProvider crashes
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }

# =========================
# Koin & Coroutines
# =========================
# Prevent Koin from failing to resolve dependencies when obfuscated
-keep class org.koin.** { *; }

# =========================
# Domain Models
# =========================
# Keep data classes used by the widget fetcher to prevent serialization/mapping issues
-keep class com.suseoaa.projectoaa.shared.domain.model.** { *; }
