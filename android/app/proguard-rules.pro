# App-specific R8 rules. Library-provided consumer rules remain authoritative
# for Retrofit, Gson, Kotlin serialization, CameraX, ML Kit and Compose.

# Retrofit and Gson inspect generic signatures and runtime annotations.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Gson deserializes API DTO fields reflectively. Class names may still be
# obfuscated, but field names and fields themselves must remain stable for DTOs
# that intentionally rely on matching JSON names without @SerializedName.
-keep,allowoptimization,allowobfuscation class se.iloppis.app.network.**
-keepclassmembers,allowoptimization class se.iloppis.app.network.** {
    <fields>;
}
