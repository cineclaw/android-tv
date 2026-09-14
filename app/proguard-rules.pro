# Proguard rules for CineClaw TV
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.cineclaw.tv.core.model.** { *; }
-keepclassmembers class com.cineclaw.tv.core.model.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Retrofit API Interfaces
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface com.cineclaw.tv.core.network.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Android YouTube Player & JavascriptInterface
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
