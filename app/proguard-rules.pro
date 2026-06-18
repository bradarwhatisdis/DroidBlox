# Keep serializable data classes
-keepclassmembers class com.drake.droidblox.data.models.** {
    *** Companion;
    <fields>;
}
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.drake.droidblox.**$$serializer { *; }
-keepclassmembers class com.drake.droidblox.** {
    *** Companion;
}
-keepclasseswithmembers class com.drake.droidblox.** {
    kotlinx.serialization.KSerializer serializer(...);
}
