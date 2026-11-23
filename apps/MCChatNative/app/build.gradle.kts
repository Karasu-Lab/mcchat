plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.datastore.okio)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
}
