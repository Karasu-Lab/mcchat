plugins {
    alias(libs.plugins.android.application)
}

val (major, minor, patch) = version.toString().split(".").map(String::toInt)

android {
    namespace = "com.karasu256.mcchatnative"

    defaultConfig {
        applicationId = "com.karasu256.mcchatnative"
        versionCode = major * 10000 + minor * 100 + patch
        versionName = version.toString()
    }
}
