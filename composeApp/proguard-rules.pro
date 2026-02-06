# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.markduenas.antonymizer.**$$serializer { *; }
-keepclassmembers class com.markduenas.antonymizer.** {
    *** Companion;
}
-keepclasseswithmembers class com.markduenas.antonymizer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used with serialization
-keep class com.markduenas.antonymizer.data.model.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Google Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.api.** { *; }

# Google AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep monetization classes
-keep class com.markduenas.antonymizer.monetization.** { *; }
