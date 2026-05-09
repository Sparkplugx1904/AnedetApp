# 📦 LIBRARY & DEPENDENCIES - BUILD.GRADLE.KTS

> **Daftar lengkap library yang WAJIB ada untuk implementasi Android**  
> Berdasarkan spesifikasi `CLAUDE.md` dan kebutuhan `live_inference.py`

---

## 🎯 OVERVIEW

Dokumen ini berisi:
1. **build.gradle.kts (Project level)** - Plugin dan repository
2. **build.gradle.kts (App level)** - Dependencies lengkap
3. **gradle/libs.versions.toml** - Version catalog
4. **Penjelasan setiap library** dan fungsinya

---

## 📁 1. PROJECT LEVEL BUILD.GRADLE.KTS

**File:** `build.gradle.kts` (root project)

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

---

## 📱 2. APP LEVEL BUILD.GRADLE.KTS

**File:** `app/build.gradle.kts`

```kotlin
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
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // JANGAN tambahkan tensorflow-lite-gpu - CPU only sesuai spesifikasi

    // ══════════════════════════════════════════════════════════════════════════
    // OPENCV FOR ANDROID - Untuk preprocessing (CLAHE, bilateral, white balance)
    // ══════════════════════════════════════════════════════════════════════════
    implementation("com.quickbirdstudios:opencv:4.9.0")

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
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 📋 3. GRADLE VERSION CATALOG

**File:** `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.5.2"
kotlin = "1.9.22"
compose-bom = "2024.06.00"
hilt = "2.51.1"
ksp = "1.9.22-1.0.17"
camerax = "1.3.4"
tensorflow-lite = "2.16.1"
opencv = "4.9.0"
room = "2.6.1"
navigation = "2.8.0"
coroutines = "1.8.1"
datastore = "1.1.1"
coil = "2.7.0"
accompanist = "0.35.1-alpha"

[libraries]
# Core
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version = "2.8.4" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }

# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# CameraX
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# TensorFlow Lite
tensorflow-lite = { group = "org.tensorflow", name = "tensorflow-lite", version.ref = "tensorflow-lite" }
tensorflow-lite-support = { group = "org.tensorflow", name = "tensorflow-lite-support", version = "0.4.4" }

# OpenCV
opencv = { group = "com.quickbirdstudios", name = "opencv", version.ref = "opencv" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# DataStore
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Coil
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Accompanist
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version.ref = "accompanist" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
mockito-kotlin = { group = "org.mockito.kotlin", name = "mockito-kotlin", version = "5.3.1" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version = "1.2.1" }
androidx-test-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version = "3.6.1" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

## 📚 4. PENJELASAN SETIAP LIBRARY

### 🎥 **CameraX (androidx.camera)**

**Versi:** 1.3.4

**Fungsi:**
- `camera-camera2`: Core CameraX API untuk akses kamera
- `camera-lifecycle`: Integrasi dengan Android Lifecycle
- `camera-view`: PreviewView untuk tampilkan preview kamera

**Kenapa dibutuhkan:**
- Menggantikan `cv2.VideoCapture()` dari Python
- Setup resolusi 1280×720 (identik dengan Python)
- Frame capture untuk preprocessing dan inference

**Mapping dari Python:**
```python
# Python
cap = cv2.VideoCapture(CAMERA_INDEX)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
```

```kotlin
// Android
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(1280, 720))
    .build()
```

---

### 🤖 **TensorFlow Lite**

**Versi:** 2.16.1

**Fungsi:**
- `tensorflow-lite`: Core TFLite runtime untuk inference
- `tensorflow-lite-support`: Helper untuk preprocessing dan postprocessing

**Kenapa dibutuhkan:**
- Menjalankan model segmentasi (`best_int8.tflite`)
- Menjalankan model klasifikasi (`best_float32.tflite`)
- **CPU only** - TIDAK menggunakan GPU delegate

**Mapping dari Python:**
```python
# Python
import tensorflow.lite as tflite
interpreter = tflite.Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()
```

```kotlin
// Android
val interpreter = Interpreter(loadModelBuffer(context, MODEL_PATH))
```

**⚠️ PENTING:**
- JANGAN tambahkan `tensorflow-lite-gpu`
- User menetapkan CPU only sesuai spesifikasi

---

### 🖼️ **OpenCV for Android**

**Versi:** 4.9.0

**Fungsi:**
- CLAHE (Contrast Limited Adaptive Histogram Equalization)
- Bilateral Filter
- Color space conversion (BGR ↔ LAB)
- Masking dan cropping dengan polygon

**Kenapa dibutuhkan:**
- Implementasi `apply_clahe()` dari Python
- Preprocessing pipeline identik dengan Python
- Operasi Mat untuk image processing

**Mapping dari Python:**
```python
# Python
lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
l = clahe.apply(l)
```

```kotlin
// Android
Imgproc.cvtColor(mat, labMat, Imgproc.COLOR_BGR2Lab)
val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
clahe.apply(lChannel, lEnhanced)
```

**⚠️ CATATAN:**
- OpenCV Android **TIDAK support CUDA**
- Semua processing di CPU (berbeda dengan Python yang bisa GPU)

---

### 🎨 **Jetpack Compose + Material 3**

**Versi:** BOM 2024.06.00

**Fungsi:**
- UI framework modern untuk Android
- Material Design 3 components
- Canvas untuk overlay polygon

**Kenapa dibutuhkan:**
- Tampilan kamera dengan overlay real-time
- Bottom sheet untuk hasil klasifikasi
- Dialog, button, dan UI components

**Mapping dari Python:**
```python
# Python
cv2.polylines(frame, [polygon], True, color, 3)
cv2.imshow("Main View", frame)
```

```kotlin
// Android
Canvas {
    drawPath(path, color.copy(alpha = 0.30f), style = Fill)
    drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
}
```

---

### 💉 **Hilt (Dependency Injection)**

**Versi:** 2.51.1

**Fungsi:**
- Dependency injection untuk TFLite Interpreter
- Singleton management untuk model
- ViewModel injection

**Kenapa dibutuhkan:**
- Pastikan hanya satu instance Interpreter (thread-safe)
- Lifecycle management untuk model
- Clean architecture dengan DI

**Contoh:**
```kotlin
@Singleton
class ConjunctivaSegmentor @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {
    private val interpreter: Interpreter
    // ...
}
```

---

### 🗄️ **Room Database**

**Versi:** 2.6.1

**Fungsi:**
- Local database untuk history pemeriksaan
- Simpan metadata hasil klasifikasi
- Query dan filter history

**Kenapa dibutuhkan:**
- Fitur history pemeriksaan (HistoryScreen)
- Simpan timestamp, label, confidence, image path
- Filter by label (Anemia / Non-Anemia)

**Schema:**
```kotlin
@Entity(tableName = "examinations")
data class ExaminationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val labelAnemia: Float,
    val labelNonAnemia: Float,
    val predictedLabel: String,
    val confidence: Float,
    val imagePath: String,
    val mode: String
)
```

---

### 🧭 **Navigation Compose**

**Versi:** 2.8.0

**Fungsi:**
- Navigasi antar screen (Camera, History, Settings, Onboarding)
- Deep linking
- Back stack management

**Kenapa dibutuhkan:**
- Multi-screen app (Camera, History, Settings, Onboarding)
- Navigation dengan type-safe arguments

---

### ⚡ **Kotlin Coroutines**

**Versi:** 1.8.1

**Fungsi:**
- Async processing untuk inference
- `Dispatchers.Default` untuk CPU-intensive task
- `delay()` untuk live inference interval

**Kenapa dibutuhkan:**
- Preprocessing dan inference di background thread
- Live inference dengan interval 1 detik
- Thread-safe dengan Mutex

**Mapping dari Python:**
```python
# Python
while True:
    ret, frame = cap.read()
    # process frame
```

```kotlin
// Android
viewModelScope.launch(Dispatchers.Default) {
    while (isLiveInferenceActive.value) {
        // process frame
        delay(1000)  // 1 detik interval
    }
}
```

---

### 💾 **DataStore Preferences**

**Versi:** 1.1.1

**Fungsi:**
- Simpan preferensi user (bahasa, tema, first-launch)
- Asynchronous dan type-safe
- Replacement untuk SharedPreferences

**Kenapa dibutuhkan:**
- Simpan pilihan bahasa (ID / EN / TH)
- Simpan pilihan tema (Light / Dark / System)
- Flag onboarding sudah ditampilkan atau belum

---

### 🖼️ **Coil**

**Versi:** 2.7.0

**Fungsi:**
- Image loading library untuk Compose
- Tampilkan thumbnail di HistoryScreen
- Async image loading dengan caching

**Kenapa dibutuhkan:**
- Tampilkan gambar hasil pemeriksaan di history
- Efficient memory management untuk list gambar

---

### 🔐 **Accompanist Permissions**

**Versi:** 0.35.1-alpha

**Fungsi:**
- Runtime permission handling untuk Compose
- Camera permission request
- Permission state management

**Kenapa dibutuhkan:**
- Request camera permission saat pertama kali buka
- Handle permission denied scenario
- Compose-friendly permission API

---

## 🔧 5. PROGUARD RULES

**File:** `app/proguard-rules.pro`

```proguard
# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.**

# OpenCV
-keep class org.opencv.** { *; }
-keep class org.opencv.android.** { *; }
-dontwarn org.opencv.**

# Hilt
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
```

---

## 📊 6. UKURAN APK ESTIMASI

| Component | Size |
|-----------|------|
| TensorFlow Lite | ~1.5 MB |
| OpenCV Android | ~15 MB |
| Model Segmentasi (INT8) | ~2 MB |
| Model Klasifikasi (FLOAT32) | ~10 MB |
| CameraX | ~2 MB |
| Compose + Material 3 | ~5 MB |
| Hilt + Room + Navigation | ~3 MB |
| **Total Estimasi** | **~40 MB** |

**Target:** < 40 MB (sesuai spesifikasi CLAUDE.md)

---

## ✅ CHECKLIST DEPENDENCIES

- [ ] CameraX 1.3.4 (camera-camera2, camera-lifecycle, camera-view)
- [ ] TensorFlow Lite 2.16.1 (tensorflow-lite, tensorflow-lite-support)
- [ ] OpenCV 4.9.0 (com.quickbirdstudios:opencv)
- [ ] Compose BOM 2024.06.00 (ui, material3, icons-extended)
- [ ] Hilt 2.51.1 (hilt-android, hilt-compiler, hilt-navigation-compose)
- [ ] Room 2.6.1 (room-runtime, room-ktx, room-compiler)
- [ ] Navigation Compose 2.8.0
- [ ] Coroutines 1.8.1
- [ ] DataStore 1.1.1
- [ ] Coil 2.7.0
- [ ] Accompanist Permissions 0.35.1-alpha
- [ ] ProGuard rules untuk TFLite, OpenCV, Hilt, Room

---

## ⚠️ CATATAN PENTING

### 1. **JANGAN Tambahkan GPU Delegate**

```kotlin
// ❌ SALAH - Jangan tambahkan ini
implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")

// ✅ BENAR - CPU only
implementation("org.tensorflow:tensorflow-lite:2.16.1")
```

**Alasan:** User menetapkan CPU only sesuai spesifikasi CLAUDE.md

### 2. **OpenCV Initialization**

```kotlin
// WAJIB init OpenCV di Application class
class AnemiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        }
    }
}
```

### 3. **Packaging Options**

```kotlin
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "META-INF/DEPENDENCIES"
    }
    jniLibs {
        useLegacyPackaging = true  // PENTING untuk TFLite + OpenCV
    }
}
```

**Alasan:** Cegah konflik native library (.so files) dari TFLite dan OpenCV

### 4. **Minimum SDK 30**

```kotlin
minSdk = 30  // Android 11 - API 30
```

**Alasan:**
- MediaStore API untuk simpan gambar tanpa WRITE_EXTERNAL_STORAGE
- CameraX stable support
- Compose performance optimization

---

**📌 RINGKASAN:**

Semua library di atas adalah **WAJIB** untuk implementasi yang identik dengan `live_inference.py`.

Tidak ada library alternatif atau opsional - semuanya diperlukan untuk:
1. Camera processing (CameraX)
2. Model inference (TFLite)
3. Image preprocessing (OpenCV)
4. UI (Compose + Material 3)
5. Architecture (Hilt + Room + Navigation)
6. Async processing (Coroutines)
7. Data persistence (DataStore + Room)
