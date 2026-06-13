# Wallora ProGuard rules

# R8 optimizer had a ConcurrentModificationException bug in AGP 8.5.2.
# Fixed by upgrading to AGP 8.7.3; rule kept as safety net for older R8 versions.
-dontoptimize

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.wallora.app.**$$serializer { *; }
-keepclassmembers class com.wallora.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.wallora.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowshrinking,allowoptimization interface * { @retrofit2.http.* <methods>; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }

# Coil
-dontwarn coil.**
