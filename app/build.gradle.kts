plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.anemiadetector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.anemiadetector"
        minSdk = 30  // Android 11 - API 30
        targetSdk = 35  // Android 15 - API 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // PENTING: Cegah konflik library TFLite + OpenCV
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // ══════════════════════════════════════════════════════════════════════════
    // CORE ANDROID
    // ══════════════════════════════════════════════════════════════════════════
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.0")

    // ══════════════════════════════════════════════════════════════════════════
    // CAMERAX - Untuk capture frame kamera
    // ══════════════════════════════════════════════════════════════════════════
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ══════════════════════════════════════════════════════════════════════════
    // TENSORFLOW LITE - Untuk inference model (CPU only)
    // ══════════════════════════════════════════════════════════════════════════
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    // TIDAK menggunakan tensorflow-lite-support karena dependency GMS
    // Kita hanya butuh core TFLite untuk inference
    // JANGAN tambahkan tensorflow-lite-gpu - CPU only sesuai spesifikasi

    // ══════════════════════════════════════════════════════════════════════════
    // OPENCV FOR ANDROID - Untuk preprocessing (CLAHE, bilateral, white balance)
    // ══════════════════════════════════════════════════════════════════════════
    implementation(project(":opencv"))
    // ══════════════════════════════════════════════════════════════════════════
    // JETPACK COMPOSE + MATERIAL 3
    // ══════════════════════════════════════════════════════════════════════════
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // ══════════════════════════════════════════════════════════════════════════
    // HILT - Dependency Injection
    // ══════════════════════════════════════════════════════════════════════════
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ══════════════════════════════════════════════════════════════════════════
    // VIEWMODEL + LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // ══════════════════════════════════════════════════════════════════════════
    // ROOM - Database untuk history pemeriksaan
    // ══════════════════════════════════════════════════════════════════════════
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ══════════════════════════════════════════════════════════════════════════
    // NAVIGATION - Untuk navigasi antar screen
    // ══════════════════════════════════════════════════════════════════════════
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ══════════════════════════════════════════════════════════════════════════
    // COROUTINES - Untuk async processing
    // ══════════════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ══════════════════════════════════════════════════════════════════════════
    // DATASTORE - Untuk simpan preferensi (bahasa, tema, first-launch)
    // ══════════════════════════════════════════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ══════════════════════════════════════════════════════════════════════════
    // COIL - Untuk tampilkan gambar history
    // ══════════════════════════════════════════════════════════════════════════
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ══════════════════════════════════════════════════════════════════════════
    // ACCOMPANIST - Untuk permissions handling
    // ══════════════════════════════════════════════════════════════════════════
    implementation("com.google.accompanist:accompanist-permissions:0.35.1-alpha")

    // ══════════════════════════════════════════════════════════════════════════
    // TESTING
    // ══════════════════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
