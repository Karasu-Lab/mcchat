import com.android.build.api.dsl.ApplicationExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk {
                version = release(37)
            }

            defaultConfig {
                minSdk = 24
                targetSdk = 37
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                release {
                    optimization {
                        enable = true
                        packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
                    }
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }

        dependencies {
            add("implementation", libs.androidx.appcompat)
            add("implementation", libs.androidx.core.ktx)
            add("implementation", libs.material)
            add("implementation", platform(libs.androidx.compose.bom))
            add("implementation", libs.androidx.compose.ui)
            add("implementation", libs.androidx.compose.ui.tooling.preview)
            add("implementation", libs.androidx.compose.material3)
            add("implementation", libs.androidx.compose.material.icons.core)
            add("implementation", libs.androidx.activity.compose)
            add("implementation", libs.androidx.lifecycle.viewmodel.compose)
            add("implementation", libs.androidx.lifecycle.runtime.compose)
            add("debugImplementation", libs.androidx.compose.ui.tooling)
            add("testImplementation", libs.junit)
            add("androidTestImplementation", libs.androidx.espresso.core)
            add("androidTestImplementation", libs.androidx.junit)
        }
    }
}
