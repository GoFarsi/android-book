# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep WebView related classes
-keep class android.webkit.** { *; }
-keep class androidx.webkit.** { *; }

# Keep application class
-keep class com.book.gofarsi.** { *; }

# Keep coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep BuildConfig
-keep class com.book.gofarsi.BuildConfig { *; }

# General Android rules
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep all fragments and activities
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.appcompat.app.AppCompatActivity

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep setters and getters
-keepclassmembers public class * {
    public void set*(***);
    public *** get*();
}