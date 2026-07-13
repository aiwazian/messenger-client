-keepattributes Signature

# This is generated automatically by the Android Gradle plugin.
-dontwarn androidx.window.extensions.WindowExtensions
-dontwarn androidx.window.extensions.WindowExtensionsProvider
-dontwarn androidx.window.extensions.area.ExtensionWindowAreaPresentation
-dontwarn androidx.window.extensions.core.util.function.Consumer
-dontwarn androidx.window.extensions.core.util.function.Function
-dontwarn androidx.window.extensions.core.util.function.Predicate
-dontwarn androidx.window.extensions.layout.DisplayFeature
-dontwarn androidx.window.extensions.layout.FoldingFeature
-dontwarn androidx.window.extensions.layout.WindowLayoutComponent
-dontwarn androidx.window.extensions.layout.WindowLayoutInfo
-dontwarn androidx.window.sidecar.SidecarDeviceState
-dontwarn androidx.window.sidecar.SidecarDisplayFeature
-dontwarn androidx.window.sidecar.SidecarInterface$SidecarCallback
-dontwarn androidx.window.sidecar.SidecarInterface
-dontwarn androidx.window.sidecar.SidecarProvider
-dontwarn androidx.window.sidecar.SidecarWindowLayoutInfo

-keep class com.aiwazian.messenger.BuildConfig { *; }

-keep @kotlinx.serialization.Serializable class * { *; }

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room / Ketch Database ---
-keep class * extends androidx.room3.RoomDatabase
-keep class **_Impl {
    public <init>(...);
}
-keep class * extends androidx.room3.Entity
-keep class * extends androidx.room3.Dao

-keep,includedescriptorclasses class com.aiwazian.messenger.**$$serializer { *; }

-keepclassmembers class com.aiwazian.messenger.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiwazian.messenger.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Сами data-классы (замени на реальный пакет DTO)
-keep class com.aiwazian.messenger.network.** { *; }

# --- WorkManager ---
#-keep class * extends androidx.work.Worker { *; }
#-keep class * extends androidx.work.CoroutineWorker { *; }
#-keep class * extends androidx.work.ListenableWorker { *; }
#-keep class * extends androidx.work.InputMerger { *; }

-keep class androidx.work.** { *; }

#-keepclassmembers class * extends androidx.work.ListenableWorker {
#    public <init>(android.content.Context, androidx.work.WorkerParameters);
#}

# Deleting all calls Log.v, Log.d, Log.i, Log.w, Log.e, Log.wtf
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** i(...);
    public static *** v(...);
    public static *** w(...);
    public static *** wtf(...);
}
