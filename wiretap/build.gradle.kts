plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

android {
    namespace = "com.wiretap"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "WIRETAP_BRIDGE_ENABLED", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "WIRETAP_BRIDGE_ENABLED", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // OkHttp is compileOnly — apps that use OkHttp get the interceptor for free;
    // apps that don't, simply never call WireTapInterceptor.
    compileOnly(libs.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

// ── Maven publishing ──────────────────────────────────────────────────────────
// Run:  ./gradlew :wiretap:publishToMavenLocal
// Then consume in any app with:
//   debugImplementation("com.wiretap:wiretap:1.0.0")
// after adding mavenLocal() to the app's repositories block.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId    = "com.wiretap"
                artifactId = "wiretap"
                version    = "1.0.0"

                pom {
                    name        = "WireTap"
                    description = "Drop-in Android debug inspector for HTTP, BLE and NFC traffic"
                    url         = "https://github.com/your-org/WireTap"
                    licenses {
                        license {
                            name = "MIT License"
                            url  = "https://opensource.org/licenses/MIT"
                        }
                    }
                }
            }

            // Debug variant — useful for local development / instrumented tests
            create<MavenPublication>("debug") {
                from(components["debug"])
                groupId    = "com.wiretap"
                artifactId = "wiretap-debug"
                version    = "1.0.0"
            }
        }

        repositories {
            // publishToMavenLocal target (default ~/.m2)
            mavenLocal()

            // Optional: publish to a local file-based repo inside the project
            maven {
                name = "ProjectLocal"
                url  = uri("${rootProject.buildDir}/repo")
            }
        }
    }
}
