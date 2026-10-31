plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.paper.calc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.paper.calc"
        minSdk = 26
        targetSdk = 34
        versionCode = 15
        versionName = "1.36"
    }

    // One fixed signing key for every build: Android only accepts an update
    // when it is signed with the same key as the installed version.
    // The passwords can be overridden with environment variables (e.g. GitHub secrets).
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("PAPERCALC_KEYSTORE") ?: "papercalc-release.jks")
            storePassword = System.getenv("PAPERCALC_KEYSTORE_PASSWORD") ?: "papercalc-release"
            keyAlias = System.getenv("PAPERCALC_KEY_ALIAS") ?: "papercalc"
            keyPassword = System.getenv("PAPERCALC_KEY_PASSWORD") ?: "papercalc-release"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
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
    testImplementation("junit:junit:4.13.2")
}
