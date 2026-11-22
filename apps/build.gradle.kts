import com.android.build.api.dsl.ApplicationExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
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
            add("testImplementation", libs.junit)
            add("androidTestImplementation", libs.androidx.espresso.core)
            add("androidTestImplementation", libs.androidx.junit)
        }
    }
}
