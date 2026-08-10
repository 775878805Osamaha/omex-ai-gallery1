# Add project specific ProGuard rules here.

# Keep Room database & entities
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.omex.gallery.core.data.local.** { *; }

# Keep TFLite / LiteRT / MediaPipe
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class * {
    @org.tensorflow.lite.** *;
}
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**

# Keep ML Kit Text Recognition
-keep class com.google.mlkit.** { *; }

# Keep Coil Image Loader
-keep class coil.** { *; }

