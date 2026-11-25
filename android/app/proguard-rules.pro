-keep class uz.wegrow.invest.** { *; }
-dontwarn javax.naming.**
-dontwarn java.lang.instrument.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class uz.wegrow.invest.**$$serializer { *; }
-keepclassmembers class uz.wegrow.invest.** {
    *** Companion;
}
-keepclasseswithmembers class uz.wegrow.invest.** {
    kotlinx.serialization.KSerializer serializer(...);
}
