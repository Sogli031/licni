# Specifična ProGuard pravila za projekat (release ne koristi minifikaciju).

# ========== GSON ==========
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ========== APP DATA MODELS ==========
-keep class com.licnipodaci.data.** { *; }
-keepclassmembers class com.licnipodaci.data.** { *; }

-keep class com.licnipodaci.data.backup.SignInState { *; }
-keep class com.licnipodaci.data.backup.SignInState$* { *; }
-keep class com.licnipodaci.data.backup.SyncState { *; }
-keep class com.licnipodaci.data.backup.SyncState$* { *; }

# ========== GOOGLE API CLIENT ==========
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-keep class com.google.http.** { *; }
-dontwarn com.google.api.client.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-dontwarn org.apache.http.**
-dontwarn android.net.http.**
-dontwarn com.google.api.client.extensions.android.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

# ========== KOTLIN ==========
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy { *; }
-keep class * extends kotlin.coroutines.jvm.internal.SuspendLambda { *; }

# ========== MISC ==========
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclasseswithmembernames class * {
    native <methods>;
}
