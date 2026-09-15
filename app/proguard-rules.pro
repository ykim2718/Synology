# app/proguard-rules.pro
# kotlinx.serialization keeps the generated serializers reachable through reflection.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.yrocket.nasmusic.data.subsonic.** {
    *** Companion;
}
-keepclasseswithmembers class com.yrocket.nasmusic.data.subsonic.** {
    kotlinx.serialization.KSerializer serializer(...);
}
