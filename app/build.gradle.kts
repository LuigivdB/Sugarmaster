plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sugarmaster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sugarmaster"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Compose compiler is now managed by org.jetbrains.kotlin.plugin.compose
}

dependencies {
    // Wear OS
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")
    implementation("androidx.wear.compose:compose-navigation:1.4.1")

    // Compose
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
    implementation("androidx.compose.runtime:runtime:1.8.2")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore for credentials
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Wear
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear:wear-input:1.2.0")

    // Watch Face
    implementation("androidx.wear.watchface:watchface:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
}