# Keep source file names and line numbers for deobfuscated stack traces
-keepattributes SourceFile,LineNumberTable

# Compile-time annotations referenced by Tink (security-crypto transitive dependency)
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
