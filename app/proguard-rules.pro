# Rupee Splitter ProGuard Rules
# Optimized for offline, no-network calculator app

# Keep the application class and main entry points
-keep class com.example.rupeesplitter.MainActivity {
    public protected *;
}

# Keep model classes used for calculations
-keep class com.example.rupeesplitter.SplitCalculator {
    public *;
}
-keep class com.example.rupeesplitter.SplitCalculator$* {
    public *;
}
-keep class com.example.rupeesplitter.SplitResult {
    public *;
}
-keep class com.example.rupeesplitter.RupeeFormatter {
    public *;
}
-keep class com.example.rupeesplitter.BreakdownTextFormatter {
    public *;
}

# Keep enum/sealed classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations (if any added later)
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable implementations
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep callback methods for views
-keepclassmembers class * extends android.view.View {
    void set*(***);
    *** get*();
}

# Keep methods annotated with @Keep
-keep @androidx.annotation.Keep class * {*;}
-keep @kotlin.Metadata class * {*;}

# Optimize for size - remove unused code aggressively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Keep resource references
-keep class **.R$* {
    <fields>;
}

# Preserve annotations for Room/Serialization (if added later)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlin specific
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }

# Material Components - keep themes and styles
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ConstraintLayout
-keep class androidx.constraintlayout.** { *; }
-dontwarn androidx.constraintlayout.**

# Core library classes - don't warn about missing classes
-dontwarn java.lang.invoke.**
-dontwarn java.lang.module.**
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.serialization.**

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# If using R8 full mode (recommended for release)
# -overloadaggressively
# -repackageclasses ''
# -allowaccessmodification
