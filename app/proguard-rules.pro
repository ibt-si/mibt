# R8 (ProGuard rules). Line numbers stay in release stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# RouteKind and Letter are stored with Enum.name and restored with valueOf.
# Comparisons also use the literals "BASIC" and "EMERGENCY".
-keepclassmembers enum org.interferolog.fibt.domain.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# Room loads FibtDatabase_Impl by appending "_Impl" to the database class name.
-keep class org.interferolog.fibt.data.FibtDatabase { *; }
-keep class org.interferolog.fibt.data.*_Impl { *; }

# NavKey routes are kotlinx.serialization types on the saved back stack.
# The library's consumer rules cover serializer(), this keeps the generated companions.
-keepclassmembers class org.interferolog.fibt.navigation.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
