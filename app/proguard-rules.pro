# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.**

# OpenCV
-keep class org.opencv.** { *; }
-keep class org.opencv.android.** { *; }
-dontwarn org.opencv.**

# Hilt
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
