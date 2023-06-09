# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep only the GoogleDriveBackupManager class
-keep class media.uqab.libdrivebackup.GoogleDriveBackupManager {
    public *;
}

-keep class media.uqab.libdrivebackup.model.InitializationException
-keep class media.uqab.libdrivebackup.model.UserPermissionDeniedException
-keep class media.uqab.libdrivebackup.model.NoUserFoundException
-keep class media.uqab.libdrivebackup.model.FileInfo { *;}
-keep class media.uqab.libdrivebackup.model.UserInfo { *;}
-keep class com.google.** { *;}

# Obfuscate method names
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class media.uqab.libdrivebackup.GoogleDriveBackupManager {
    public *;
}