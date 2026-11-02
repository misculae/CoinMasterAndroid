plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ro.coinmaster.contor"
    compileSdk = 35

    defaultConfig {
        applicationId = "ro.coinmaster.contor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-prototip"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
