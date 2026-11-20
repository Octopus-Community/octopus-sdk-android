-dontwarn com.google.j2objc.annotations.ReflectionSupport
-dontwarn com.google.j2objc.annotations.RetainedWith

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# kotlinx-serialization-json specific rules
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Serializer for classes with named companion objects
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Runtime for classes with named companion objects
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializers
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generic signatures and annotations for runtime type info
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Don't shrink or obfuscate serializable classes and their fields
-keep,allowobfuscation class * implements kotlinx.serialization.KSerializer
-keepclassmembers,allowobfuscation class * implements kotlinx.serialization.KSerializer {
    <methods>;
}

# Keep Serializable classes and their members
-keep,includedescriptorclasses class com.octopuscommunity.sdk.**$$serializer { *; }
-keepclassmembers class com.octopuscommunity.sdk.** {
    *** Companion;
}
-keepclasseswithmembers class com.octopuscommunity.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all nested enums in OctopusDestination
-keep class com.octopuscommunity.sdk.ui.OctopusDestination$* { *; }