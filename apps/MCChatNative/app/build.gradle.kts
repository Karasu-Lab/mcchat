plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val (major, minor, patch) = version.toString().split(".").map(String::toInt)

android {
    namespace = "com.karasu256.mcchatnative"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.karasu256.mcchatnative"
        versionCode = major * 10000 + minor * 100 + patch
        versionName = providers.gradleProperty("commitHash").orNull
            ?.let { "$version-$it" } ?: version.toString()
        minSdk = 24
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(if (rootProject.name == "apps") ":common" else ":modules:common"))
    implementation(libs.androidx.datastore.okio)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
}
