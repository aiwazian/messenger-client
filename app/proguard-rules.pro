-keepattributes Signature, *Annotation*, InnerClasses

# --- AGP Auto-generated (Window Extensions) ---
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# --- App BuildConfig ---
-keep class com.aiwazian.messenger.BuildConfig { *; }

# --- Kotlinx Serialization ---
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Сохраняем все Serializable классы, их companion и генерируемые $$serializer
-keep @kotlinx.serialization.Serializable class * { *; }
-keep,includedescriptorclasses class com.aiwazian.messenger.**$$serializer { *; }
-keepclasseswithmembers class com.aiwazian.messenger.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room Database ---
-keep class * extends androidx.room3.RoomDatabase
-keep class * extends androidx.room3.Entity
-keep class * extends androidx.room3.Dao
-keep class **_Impl {
    public <init>(...);
}

# --- WorkManager ---
-keep class androidx.work.** { *; }

# --- Strip Logs in Release ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** i(...);
    public static *** v(...);
    public static *** w(...);
    public static *** wtf(...);
}
