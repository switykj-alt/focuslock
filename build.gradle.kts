plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

android {
    namespace = "com.example.focuslock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.focuslock"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
