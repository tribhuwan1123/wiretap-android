# Keep all WireTap data classes so kotlinx.serialization works at runtime.
-keep @kotlinx.serialization.Serializable class com.wiretap.** { *; }
-keepclassmembers class com.wiretap.** {
    kotlinx.serialization.KSerializer serializer(...);
}
