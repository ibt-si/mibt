# R8 (ProGuard rules). Line numbers stay in release stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# RouteKind and Letter are stored with Enum.name and restored with valueOf.
# Comparisons also use the literals "BASIC" and "EMERGENCY".
-keepclassmembers enum org.interferolog.mibt.domain.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# Room loads MibtDatabase_Impl by appending "_Impl" to the database class name.
-keep class org.interferolog.mibt.data.MibtDatabase { *; }
-keep class org.interferolog.mibt.data.*_Impl { *; }

# NavKey routes are kotlinx.serialization types on the saved back stack.
# The library's consumer rules cover serializer(), this keeps the generated companions.
-keepclassmembers class org.interferolog.mibt.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
