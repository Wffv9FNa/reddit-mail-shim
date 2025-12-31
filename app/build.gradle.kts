plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.Wffv9FNa.redditshim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.Wffv9FNa.redditshim"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
    }

    // Customize APK output names: reddit-mail-shim-{version}-{buildType}.apk
    // Example: reddit-mail-shim-1.0-release.apk, reddit-mail-shim-1.0-debug.apk
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "reddit-mail-shim-${defaultConfig.versionName}-${buildType.name}.apk"
        }
    }
}

dependencies {
    // HTTP client for redirect resolution
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlin coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // AndroidX core (minimal)
    implementation("androidx.core:core-ktx:1.12.0")

    // AndroidX activity for ComponentActivity and lifecycleScope
    implementation("androidx.activity:activity-ktx:1.8.0")
}
