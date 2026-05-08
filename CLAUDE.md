# 🩸 MASTER PROMPT — Android Real-Time Anemia Detection via Conjunctiva Segmentation + TFLite

> ## ⚠️ PERINGATAN KRITIS — LARANGAN MERINGKAS
> **Seluruh isi prompt ini adalah spesifikasi teknikal dan fungsional yang bersifat instruktif.**
> Setiap kalimat, setiap nilai parameter, setiap urutan pipeline, setiap larangan, dan setiap
> catatan implementasi adalah bagian yang tidak terpisahkan dari spesifikasi ini.
> **DILARANG KERAS:**
> - Meringkas, menyederhanakan, atau menggabungkan bagian manapun
> - Mengasumsikan detail yang tidak disebutkan
> - Melewati bagian yang dianggap "jelas"
> - Mengubah nilai parameter tanpa alasan teknis yang eksplisit
> - Mengganti library atau API dengan alternatif tanpa konfirmasi
>
> Jika ada ketidakjelasan atau konflik antar-bagian: **TANYAKAN, JANGAN ASUMSIKAN.**

---

## 📌 BAGIAN 0 — KONTEKS & ASAL PROYEK

### 0.1 Latar Belakang
Aplikasi ini adalah konversi langsung dari sistem deteksi anemia berbasis Python yang telah berjalan dan tervalidasi di environment desktop. Sistem tersebut menggunakan dua model TFLite terpisah yang dilatih menggunakan Ultralytics YOLOv26:

1. **Model Segmentasi Konjungtiva** — `best_int8.tflite`
   Arsitektur dasar: `yolo26n-seg.pt`, dilatih pada 640px, dieksport ke TFLite INT8 pada **320×320 px**.
   Task: Instance segmentation untuk mendeteksi dan men-segment area konjungtiva (lapisan dalam kelopak mata bawah).
   Confidence threshold: `0.35`
   Output polygon: **6–15 titik** via Algoritma Epsilon Adaptif (bukan semua piksel mask).
   NMS: **sudah di-embed** dalam model TFLite (nms=True saat export).
   Kelas: Single class — `conjunctiva` (index 0).

2. **Model Klasifikasi Anemia** — `best_float32.tflite`
   Arsitektur dasar: `yolo26m-cls.pt`, dilatih pada 448px dengan CLAHE-augmented dataset.
   Task: Binary classification — `Anemia` (index 0) vs `Non-Anemia` (index 1).
   Format: **FLOAT32** (bukan INT8 — tetap float32 untuk akurasi lebih tinggi).
   Input size: Baca **dinamis** dari `interpreter.getInputTensor(0).shape()[1]` saat runtime.
   Normalisasi input: `pixel / 255.0` → float32.
   Output: Array dua elemen `[score_Anemia, score_NonAnemia]` — tampilkan **keduanya** di UI.

### 0.2 Dataset & Preprocessing yang Digunakan
Preprocessing pipeline yang digunakan saat training (dari notebook `anemia-preprocessing-gpu-v2.ipynb`) adalah:

```
Raw BGR → Gray World White Balance → Adaptive Gamma Correction
→ Letterbox Resize 224×224 → Bilateral Filter → Adaptive CLAHE (L* channel only)
```

Parameter preprocessing yang WAJIB direplikasi identik di Android:
- **White Balance**: Gray World Assumption, strength=0.8 (blend: `scale = 1.0 + (scale - 1.0) * 0.8`)
- **Gamma**: Adaptive berdasarkan mean luminance L*, range `GAMMA_MIN=0.5`, `GAMMA_MAX=1.2`
- **Letterbox**: Resize tanpa distorsi, pad hitam di sisi yang lebih pendek
- **Bilateral Filter**: kernel 9×9, sigma_color=0.1, sigma_space=1.5 (Gaussian bilateral)
- **CLAHE**: Hanya pada channel L* dari LAB color space, clip adaptive 8–25 berdasarkan std L*, grid tile 8×8

---

## 📌 BAGIAN 1 — SPESIFIKASI TEKNIKAL APLIKASI

### 1.1 Platform & SDK
```
minSdkVersion    : 30  (Android 11 — API 30)
targetSdkVersion : 35  (Android 15 — API 35)
compileSdkVersion: 35
Bahasa           : Kotlin 100% (zero Java)
Build System     : Gradle Kotlin DSL (.kts)
```

### 1.2 Stack Teknologi Wajib
```
Camera          : CameraX (versi stable terbaru dari androidx.camera)
Inference       : TensorFlow Lite (CPU only — TANPA GPU Delegate, TANPA NNAPI Delegate)
                  → Alasan: user menetapkan CPU. GPU hanya aktif untuk preprocessing.
Image Processing: OpenCV for Android (via Maven, versi 4.9.0+)
Concurrency     : Kotlin Coroutines + Dispatchers.Default
UI Framework    : Jetpack Compose (Material Design 3)
DI              : Hilt (Dagger Hilt)
Architecture    : MVVM (ViewModel + StateFlow + Repository Pattern)
Local Database  : Room (untuk history pemeriksaan)
i18n            : Android Resource Strings (strings.xml per locale)
Locale Support  : Bahasa Indonesia (default), English, ภาษาไทย (Thai)
Theme           : Material Design 3, mendukung Light Mode DAN Dark Mode
Distribution    : Internal/Enterprise (bukan Play Store, tidak ada signing requirement ketat)
```

### 1.3 Lokasi Model di APK
```
app/src/main/assets/models/segments/best_int8.tflite    ← Segmentasi (INT8)
app/src/main/assets/models/classify/best_float32.tflite ← Klasifikasi (FLOAT32)
```

### 1.4 Struktur Package Lengkap
```
com.example.anemiadetector/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   └── ExaminationDao.kt
│   │   ├── entity/
│   │   │   └── ExaminationEntity.kt
│   │   └── AppDatabase.kt
│   ├── model/
│   │   ├── DetectionResult.kt          # bbox, polygon, confidence segmentasi
│   │   ├── ClassificationResult.kt     # label, allScores[2], confidence
│   │   ├── InferenceState.kt           # sealed class: Idle|Processing|Success|NoDetection|Error
│   │   └── ExaminationRecord.kt        # untuk history Room
│   └── repository/
│       ├── InferenceRepository.kt      # interface
│       ├── InferenceRepositoryImpl.kt  # implementasi
│       └── ExaminationRepository.kt    # history CRUD
├── di/
│   ├── AppModule.kt                    # Hilt module: provides TFLite, DB
│   └── DatabaseModule.kt
├── domain/
│   └── usecase/
│       ├── RunPreprocessingUseCase.kt   # Gray WB + Gamma + Letterbox + Bilateral + CLAHE
│       ├── RunSegmentationUseCase.kt    # Segmentasi → polygon 6-15 titik
│       ├── RunClassificationUseCase.kt  # Klasifikasi → [score_Anemia, score_NonAnemia]
│       ├── SaveExaminationUseCase.kt    # Save hasil + masked bitmap ke Room & galeri
│       └── GetHistoryUseCase.kt
├── ml/
│   ├── preprocessor/
│   │   ├── GrayWorldWhiteBalance.kt    # Gray World WB, strength=0.8
│   │   ├── AdaptiveGammaCorrector.kt   # Gamma adaptif 0.5–1.2 berdasarkan mean L*
│   │   ├── LetterboxResizer.kt         # Letterbox ke 224×224 dengan padding hitam
│   │   ├── BilateralFilterProcessor.kt # Bilateral 9×9
│   │   └── AdaptiveCLAHEProcessor.kt   # CLAHE adaptif 8–25 pada L* LAB
│   ├── segmentation/
│   │   └── ConjunctivaSegmentor.kt     # TFLite INT8 320×320 segmentasi
│   └── classification/
│       └── AnemiaClassifier.kt         # TFLite FLOAT32, dynamic input size
├── ui/
│   ├── camera/
│   │   ├── CameraScreen.kt             # Main Compose screen (segmentasi default)
│   │   ├── CameraViewModel.kt
│   │   ├── OverlayCanvas.kt            # Polygon fill alpha + label overlay
│   │   └── CaptureResultSheet.kt       # Bottom sheet setelah capture/classify
│   ├── history/
│   │   ├── HistoryScreen.kt
│   │   └── HistoryViewModel.kt
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt         # Tutorial panduan pertama kali buka
│   │   └── OnboardingViewModel.kt
│   ├── settings/
│   │   └── SettingsScreen.kt           # Bahasa, tema, mode inference
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt                    # Light + Dark theme Material 3
│       └── Type.kt
└── utils/
    ├── BitmapUtils.kt                  # BGR↔RGB, crop, mask apply, resize, normalize
    ├── PolygonUtils.kt                 # Area hitung, argmax, mask fill, alpha overlay
    ├── CameraUtils.kt                  # YUV→Bitmap, FPS throttle
    ├── LocaleUtils.kt                  # Language switcher runtime
    └── PermissionUtils.kt
```

---

## 📌 BAGIAN 2 — BUILD CONFIGURATION

### 2.1 `build.gradle.kts` (app level) — WAJIB LENGKAP PERSIS INI
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
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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

    // Penting: cegah konflik library TFLite + OpenCV
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
    // ── CameraX ──────────────────────────────────────────────────────────────
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ── TensorFlow Lite (CPU only — no GPU delegate) ──────────────────────────
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // JANGAN tambahkan tensorflow-lite-gpu — user menetapkan CPU only

    // ── OpenCV for Android (untuk CLAHE, bilateral, white balance, masking) ───
    implementation("com.quickbirdstudios:opencv:4.9.0")

    // ── Jetpack Compose + Material 3 ─────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")

    // ── Hilt DI ───────────────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── ViewModel + Lifecycle ─────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // ── Room (history pemeriksaan) ────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ── DataStore (simpan preferensi bahasa, tema, first-launch) ─────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Coil (tampilkan gambar history) ──────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Test ──────────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

### 2.2 `AndroidManifest.xml` — LENGKAP
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.FLASHLIGHT" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Hardware Features -->
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
    <uses-feature android:name="android.hardware.camera.flash" android:required="false" />

    <application
        android:name=".AnemiaApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AnemiaDetector"
        android:localeConfig="@xml/locales_config">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

### 2.3 `res/xml/locales_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="in"/>  <!-- Bahasa Indonesia -->
    <locale android:name="en"/>  <!-- English -->
    <locale android:name="th"/>  <!-- ภาษาไทย (Thai) -->
</locale-config>
```

### 2.4 `proguard-rules.pro` — Tambahan Wajib
```proguard
# TFLite
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }

# OpenCV
-keep class org.opencv.** { *; }
-keep class org.opencv.android.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
```

---

## 📌 BAGIAN 3 — PREPROCESSING PIPELINE (WAJIB IDENTIK DENGAN PYTHON v2)

> **KRITIS:** Seluruh preprocessing di Android harus menghasilkan output yang
> secara visual identik dengan `preprocess_batch_gpu_v2()` dari `anemia-preprocessing-gpu-v2.ipynb`.
> Urutan langkah TIDAK BOLEH diubah karena setiap langkah bergantung pada output langkah sebelumnya.

### 3.1 Pipeline Urutan Resmi (dari notebook Python)
```
Bitmap (RGB dari CameraX)
    │
    ▼ Step 1: GrayWorldWhiteBalance
    │   → strength = 0.8
    │   → mean_gray = (mean_R + mean_G + mean_B) / 3.0
    │   → scale_ch = 1.0 + (mean_gray / mean_ch - 1.0) * 0.8
    │   → clip scale ke [0.5, 1.8] SEBELUM blend
    │   → clip pixel ke [0, 255]
    │
    ▼ Step 2: AdaptiveGammaCorrection
    │   → Hitung mean luminance L* dari LAB color space
    │   → gamma = GAMMA_MIN + (GAMMA_MAX - GAMMA_MIN) * (mean_L / 0.9)
    │   → clip gamma ke [0.5, 1.2]
    │   → output = pixel^gamma  (brightening jika gamma < 1)
    │
    ▼ Step 3: LetterboxResize → 224×224
    │   → scale = 224 / max(height, width)
    │   → resize proporsional
    │   → pad dengan pixel hitam (value=0) di sisi yang lebih pendek
    │   → output selalu 224×224
    │
    ▼ Step 4: BilateralFilter
    │   → kernel_size = 9×9
    │   → sigma_color = 0.1 (dalam skala 0–1, karena gambar ternormalisasi)
    │   → sigma_space = 1.5
    │   → OpenCV: cv::bilateralFilter dengan nilai yg ekuivalen
    │
    ▼ Step 5: AdaptiveCLAHE (hanya pada L* channel)
    │   → Konversi BGR→LAB
    │   → Pisahkan channel: L, a, b
    │   → Hitung std L channel → clip_limit = adaptif 8–25
    │     Formula: clip = max(8, min(25, 25 - (25-8) * (std_L / 0.20 * 255)))
    │   → Terapkan CLAHE HANYA pada L channel
    │   → grid tile: 8×8
    │   → Merge kembali (L_enhanced, a, b)
    │   → Konversi LAB→BGR
    │
    ▼ Output: Bitmap RGB 224×224, siap untuk model segmentasi & klasifikasi
```

### 3.2 `GrayWorldWhiteBalance.kt` — Implementasi Lengkap
```kotlin
object GrayWorldWhiteBalance {
    private const val WB_STRENGTH = 0.8f
    private const val SCALE_MIN = 0.5f
    private const val SCALE_MAX = 1.8f

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)          // RGBA mat dari Bitmap Android

        // RGBA → BGR (OpenCV standard, dan sesuai Python yang juga BGR)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

        val floatMat = Mat()
        mat.convertTo(floatMat, CvType.CV_32FC3)

        // Split channels B, G, R
        val channels = mutableListOf<Mat>()
        Core.split(floatMat, channels)

        val meanB = Core.mean(channels[0]).`val`[0]
        val meanG = Core.mean(channels[1]).`val`[0]
        val meanR = Core.mean(channels[2]).`val`[0]
        val meanGray = (meanB + meanG + meanR) / 3.0

        if (meanGray < 1e-6) {
            mat.release(); floatMat.release(); channels.forEach { it.release() }
            return bitmap
        }

        fun computeScale(meanCh: Double): Float {
            val rawScale = (meanGray / (meanCh + 1e-6)).toFloat()
            val clipped = rawScale.coerceIn(SCALE_MIN, SCALE_MAX)
            return 1.0f + (clipped - 1.0f) * WB_STRENGTH
        }

        val scaleB = computeScale(meanB)
        val scaleG = computeScale(meanG)
        val scaleR = computeScale(meanR)

        Core.multiply(channels[0], Scalar(scaleB.toDouble()), channels[0])
        Core.multiply(channels[1], Scalar(scaleG.toDouble()), channels[1])
        Core.multiply(channels[2], Scalar(scaleR.toDouble()), channels[2])

        Core.merge(channels, floatMat)
        Core.min(floatMat, Scalar(255.0, 255.0, 255.0), floatMat)
        Core.max(floatMat, Scalar(0.0, 0.0, 0.0), floatMat)

        floatMat.convertTo(mat, CvType.CV_8UC3)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)  // Kembali ke RGBA untuk Bitmap

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)

        mat.release(); floatMat.release(); channels.forEach { it.release() }
        return result
    }
}
```

### 3.3 `AdaptiveCLAHEProcessor.kt` — Implementasi Lengkap
```kotlin
object AdaptiveCLAHEProcessor {
    private const val CLAHE_CLIP_MIN = 8.0
    private const val CLAHE_CLIP_MAX = 25.0
    private val TILE_GRID = Size(8.0, 8.0)

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)           // RGBA
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

        // BGR → LAB
        val labMat = Mat()
        Imgproc.cvtColor(mat, labMat, Imgproc.COLOR_BGR2Lab)

        // Split L, a, b channels
        val channels = mutableListOf<Mat>()
        Core.split(labMat, channels)
        val lChannel = channels[0]

        // Hitung std L channel untuk adaptive clip
        val meanStdResult = MatOfDouble()
        Core.meanStdDev(lChannel, MatOfDouble(), meanStdResult)
        val stdL = meanStdResult.get(0, 0)[0]    // std dalam range 0–255

        // Adaptive clip: std tinggi (sudah kontras) → clip rendah; std rendah → clip tinggi
        val clipLimit = (CLAHE_CLIP_MAX - (CLAHE_CLIP_MAX - CLAHE_CLIP_MIN) * (stdL / (0.20 * 255.0)))
            .coerceIn(CLAHE_CLIP_MIN, CLAHE_CLIP_MAX)

        // Terapkan CLAHE pada L channel saja
        val clahe = Imgproc.createCLAHE(clipLimit, TILE_GRID)
        val lEnhanced = Mat()
        clahe.apply(lChannel, lEnhanced)
        channels[0] = lEnhanced

        // Merge kembali
        Core.merge(channels, labMat)
        Imgproc.cvtColor(labMat, mat, Imgproc.COLOR_Lab2BGR)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)

        mat.release(); labMat.release()
        channels.forEach { it.release() }
        lEnhanced.release()
        return result
    }
}
```

### 3.4 `LetterboxResizer.kt`
```kotlin
object LetterboxResizer {
    fun resize(bitmap: Bitmap, targetSize: Int): Bitmap {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val scale = targetSize.toFloat() / maxOf(srcW, srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()

        val canvas = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val androidCanvas = android.graphics.Canvas(canvas)
        androidCanvas.drawColor(android.graphics.Color.BLACK)

        val xOff = (targetSize - newW) / 2
        val yOff = (targetSize - newH) / 2

        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        androidCanvas.drawBitmap(scaled, xOff.toFloat(), yOff.toFloat(), null)
        scaled.recycle()
        return canvas
    }
}
```

### 3.5 `BilateralFilterProcessor.kt`
```kotlin
object BilateralFilterProcessor {
    // Ekuivalen dengan Kornia bilateral_blur(kernel=(9,9), sigma_color=0.1, sigma_space=1.5)
    // Untuk OpenCV CPU: konversi sigma_color dari [0,1] ke [0,255]
    private const val KERNEL_DIAMETER = 9
    private const val SIGMA_COLOR = 25.5   // 0.1 * 255
    private const val SIGMA_SPACE = 1.5

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

        val filtered = Mat()
        Imgproc.bilateralFilter(mat, filtered, KERNEL_DIAMETER, SIGMA_COLOR, SIGMA_SPACE)

        Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_BGR2RGBA)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(filtered, result)

        mat.release(); filtered.release()
        return result
    }
}
```

---

## 📌 BAGIAN 4 — MODEL INFERENCE

### 4.1 `ConjunctivaSegmentor.kt` — Segmentasi TFLite INT8

```kotlin
// SPESIFIKASI MODEL (dari conjunctiva-detection.ipynb):
// - Arsitektur: yolo26n-seg.pt → TFLite INT8
// - Training imgsz: 640px
// - Export imgsz: 320×320 px (trade-off kecepatan vs presisi)
// - NMS: SUDAH di-embed dalam model (nms=True saat export)
// - Confidence threshold: 0.35 (FIXED, tidak ada slider)
// - Kelas: single class "conjunctiva" (index 0)
// - Algoritma pemilihan: TERBESAR AREA POLYGON (bukan confidence tertinggi)
//   → Identik dengan Python: best_idx = np.argmax(areas)
// - Output polygon: 6-15 titik via Adaptive Epsilon
//   MIN_POINTS=6, MAX_POINTS=15, epsilon_factors dari 0.10 ke 0.005

class ConjunctivaSegmentor @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {

    companion object {
        const val INPUT_SIZE = 320
        const val CONF_THRESHOLD = 0.35f
        const val MIN_POLYGON_POINTS = 6
        const val MAX_POLYGON_POINTS = 15
        private const val MODEL_PATH = "models/segments/best_int8.tflite"
    }

    private val interpreter: Interpreter = run {
        val options = Interpreter.Options().apply {
            numThreads = 4  // CPU only, maksimalkan thread
        }
        Interpreter(loadModelBuffer(context, MODEL_PATH), options)
    }

    data class SegmentationResult(
        val polygon: List<PointF>,       // 6-15 titik dalam koordinat frame asli
        val boundingBox: RectF,          // Bounding box koordinat frame asli
        val maskBitmap: Bitmap,          // Binary mask DENGAN ALPHA fill (untuk overlay)
        val confidence: Float
    )

    fun segment(preprocessedBitmap: Bitmap, originalWidth: Int, originalHeight: Int): SegmentationResult? {
        // 1. Resize input ke INPUT_SIZE×INPUT_SIZE
        // 2. Konversi Bitmap RGB → ByteBuffer INT8
        //    CATATAN: Model INT8 quantized → input mungkin UINT8 atau tetap FLOAT32
        //    → Cek interpreter.getInputTensor(0).dataType() di runtime
        // 3. Run inference (NMS sudah embedded, output langsung berisi deteksi final)
        // 4. Parse output tensor → bbox + mask coords
        // 5. Scale koordinat dari 320×320 ke originalWidth×originalHeight
        // 6. Hitung area tiap polygon → pilih TERBESAR (argmax area)
        // 7. Jalankan Adaptive Epsilon untuk reduce polygon ke 6-15 titik
        //    → IDENTIK dengan get_precision_points() dari Python
        // 8. Generate maskBitmap dengan alpha fill (lihat Bagian 6 untuk spesifikasi alpha)
        // 9. Return null jika tidak ada deteksi di atas CONF_THRESHOLD
    }

    // Adaptive Epsilon Polygon Reduction — IDENTIK dengan Python
    private fun getAdaptivePolygon(
        contourPoints: Array<PointF>,
        minPts: Int = MIN_POLYGON_POINTS,
        maxPts: Int = MAX_POLYGON_POINTS
    ): List<PointF> {
        // Iterasi epsilon_factor dari 0.10 → 0.005 (100 langkah linear)
        // Hitung arcLength dari kontur
        // Untuk setiap epsilon: approxPolyDP(epsilon * arcLength)
        // Stop segera setelah n_pts masuk range [minPts, maxPts]
        // Fallback: kembalikan approx paling halus jika tidak ada yang masuk range
        val epsilonFactors = (0 until 100).map { i -> 0.10 - (0.10 - 0.005) * i / 99.0 }
        // ... implementasi
    }

    private fun computeContourArea(polygon: List<PointF>): Float {
        // Shoelace formula: identik dengan cv2.contourArea()
        var area = 0f
        val n = polygon.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += polygon[i].x * polygon[j].y
            area -= polygon[j].x * polygon[i].y
        }
        return kotlin.math.abs(area) / 2f
    }

    override fun close() { interpreter.close() }
}
```

### 4.2 `AnemiaClassifier.kt` — Klasifikasi TFLite FLOAT32

```kotlin
// SPESIFIKASI MODEL (dari anemia-classify-clahe.ipynb):
// - Arsitektur: yolo26m-cls.pt → TFLite FLOAT32
// - Training imgsz: 448px, CLAHE-augmented dataset 8,256 gambar
// - Input size: BACA DINAMIS dari interpreter.getInputTensor(0).shape()[1]
//   → Jangan hardcode 448 atau 224 — baca dari model
// - Normalisasi: pixel / 255.0 → float32
// - Output: [score_Anemia, score_NonAnemia] — 2 elemen
// - Label map: index 0 = "Anemia" (atau "Anemic"), index 1 = "Non-Anemia"
//   → Verify dari model output names jika tersedia
// - TAMPILKAN KEDUA SCORE di UI (tidak hanya argmax)

class AnemiaClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {

    companion object {
        private const val MODEL_PATH = "models/classify/best_float32.tflite"
        val CLASS_NAMES = mapOf(0 to "Anemia", 1 to "Non-Anemia")
    }

    private val interpreter: Interpreter
    val inputSize: Int  // Dibaca dinamis dari model

    init {
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(loadModelBuffer(context, MODEL_PATH), options)
        // Baca input size DINAMIS
        inputSize = interpreter.getInputTensor(0).shape()[1]
    }

    data class ClassificationResult(
        val label: String,          // "Anemia" atau "Non-Anemia"
        val confidence: Float,      // Score kelas yang dipilih
        val allScores: FloatArray,  // [score_Anemia, score_NonAnemia] — WAJIB expose keduanya
        val isAnemic: Boolean
    )

    fun classify(conjunctivaCrop: Bitmap): ClassificationResult {
        // 1. Resize crop ke inputSize × inputSize
        val resized = Bitmap.createScaledBitmap(conjunctivaCrop, inputSize, inputSize, true)

        // 2. Konversi ke float array, normalize /255.0
        //    Input Bitmap Android = RGB, model dilatih dengan RGB → TIDAK perlu konversi warna
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)  // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)   // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)            // B
        }

        // 3. Run inference
        val outputBuffer = Array(1) { FloatArray(2) }
        interpreter.run(inputBuffer, outputBuffer)

        // 4. Parse output
        val scores = outputBuffer[0]
        val predictedIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
        val label = CLASS_NAMES[predictedIdx] ?: "Unknown"

        resized.recycle()
        return ClassificationResult(
            label = label,
            confidence = scores[predictedIdx],
            allScores = scores,
            isAnemic = predictedIdx == 0
        )
    }

    override fun close() { interpreter.close() }
}
```

---

## 📌 BAGIAN 5 — MODE OPERASI KAMERA

### 5.1 Dua Mode Operasi yang Harus Diimplementasikan

#### MODE 1 — Live Segmentation (DEFAULT, Selalu Aktif)
```
Status: Aktif secara default saat aplikasi dibuka
Pipeline: Tangkap frame → CLAHE preprocessing → Segmentasi → Overlay polygon SAJA
Klasifikasi: TIDAK berjalan di mode ini secara otomatis
FPS target: ≥ 30 FPS untuk preview kamera
Inference rate segmentasi: seoptimal mungkin tanpa menurunkan FPS di bawah 30
Overlay: Polygon konjungtiva dengan alpha fill (warna netral, misal biru/cyan transparan)
         → Pengguna bisa melihat seberapa jauh konjungtiva terdeteksi
```

#### MODE 2 — Single Capture (Tombol Kamera)
```
Trigger: User menekan tombol kamera (ikon capture / shutter)
Pipeline:
  1. Ambil FRAME TERAKHIR yang sudah di-buffer (bukan jepret baru)
  2. Jalankan full pipeline: Preprocessing → Segmentasi → Cropping → Klasifikasi
  3. Tampilkan hasil di CaptureResultSheet (bottom sheet)
  4. Tawarkan opsi simpan (lihat Bagian 7)
Durasi processing: boleh 200–500ms (bukan real-time, single inference)
```

#### MODE 3 — Live Inference (Opsional, Harus Ada Peringatan)
```
Trigger: User mengaktifkan toggle di UI (dengan dialog peringatan dulu)
Peringatan dialog (WAJIB muncul sebelum mode aktif, tidak bisa dilewati):
  → ID: "Mode inferensi langsung diaktifkan. Mode ini akan memproses gambar
         setiap 1 detik dan dapat menyebabkan perangkat sedikit melambat
         atau cepat panas. Lanjutkan?"
  → EN: "Live inference mode activated. This mode processes images every
         1 second and may cause the device to slow down or heat up. Continue?"
  → TH: "โหมดการอนุมานสด กำลังทำงาน โหมดนี้จะประมวลผลภาพทุก 1 วินาที
         และอาจทำให้อุปกรณ์ทำงานช้าลงหรือร้อนขึ้น ต้องการดำเนินการต่อหรือไม่?"
Pipeline: Setiap 1000ms → full preprocessing → segmentasi → klasifikasi → update hasil
Interval: Gunakan coroutine delay(1000) — bukan Timer atau handler
Overlay: Polygon + label + confidence seperti mode single, update setiap 1 detik
```

### 5.2 CameraX Configuration
```kotlin
// WAJIB: Resolusi tetap 1280×720 (identik dengan Python: cap.set(CAP_PROP_FRAME_WIDTH, 1280))
// WAJIB: STRATEGY_KEEP_ONLY_LATEST (cegah frame queue build-up)
// WAJIB: OUTPUT_IMAGE_FORMAT_RGBA_8888 (untuk konversi Bitmap langsung)

val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(1280, 720))
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
    .build()

// FPS management: gunakan frame skip adaptif
// Jangan gunakan Thread.sleep() — gunakan timestamp comparison
var lastSegInferenceMs = 0L
val SEG_INTERVAL_MS = 100L   // Segmentasi max 10x per detik untuk hemat CPU

imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
    val now = SystemClock.elapsedRealtime()
    if (now - lastSegInferenceMs >= SEG_INTERVAL_MS) {
        lastSegInferenceMs = now
        // Proses frame untuk segmentasi
        processFrameForSegmentation(imageProxy.toBitmap())
    }
    // WAJIB: Selalu panggil close() meski frame di-skip
    imageProxy.close()
}

// Camera control yang HARUS diekspose ke UI:
// 1. Torch on/off (senter)
// 2. Switch front/back camera
// 3. Tap-to-focus via SurfaceOrientedMeteringPointFactory
// 4. Camera selector: default BACK camera (untuk foto konjungtiva mata)

val cameraControl = camera.cameraControl
val cameraInfo = camera.cameraInfo

// Torch
fun setTorch(enabled: Boolean) { cameraControl.enableTorch(enabled) }

// Flip camera
fun flipCamera() {
    currentLens = if (currentLens == CameraSelector.LENS_FACING_BACK)
        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    rebindCamera()
}

// Auto-focus on tap
fun focusOnPoint(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
    val factory = SurfaceOrientedMeteringPointFactory(viewWidth.toFloat(), viewHeight.toFloat())
    val point = factory.createPoint(x, y)
    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
        .setAutoCancelDuration(3, TimeUnit.SECONDS)
        .build()
    cameraControl.startFocusAndMetering(action)
}
```

---

## 📌 BAGIAN 6 — VISUALISASI OVERLAY (CANVAS)

### 6.1 Spesifikasi Alpha Mask Fill
```
KRITIS: Masking di mobile HARUS menggunakan alpha fill agar user bisa melihat
sampai bagian mana konjungtiva terdeteksi (berbeda dari Python yang hanya polylines).

Implementasi:
- Gambar polygon path tertutup
- Fill interior dengan warna sesuai status + alpha = 0.3 (30% opacity)
- Stroke garis tepi dengan warna penuh + alpha = 1.0 (100% opacity), tebal 3dp
- Titik-titik vertex polygon: gambar dot kecil (radius 4dp) sebagai visual feedback

Warna berdasarkan status:
┌─────────────────┬────────────────────────────────────────────────────────┐
│ Status          │ Warna Stroke + Fill                                    │
├─────────────────┼────────────────────────────────────────────────────────┤
│ Anemia          │ Stroke: #FF3B30 (Merah iOS-style)                      │
│                 │ Fill: #FF3B30 dengan alpha 0.3                         │
├─────────────────┼────────────────────────────────────────────────────────┤
│ Non-Anemia      │ Stroke: #34C759 (Hijau iOS-style)                      │
│                 │ Fill: #34C759 dengan alpha 0.3                         │
├─────────────────┼────────────────────────────────────────────────────────┤
│ Segmented only  │ Stroke: #007AFF (Biru iOS-style)                       │
│ (belum classify)│ Fill: #007AFF dengan alpha 0.25                        │
├─────────────────┼────────────────────────────────────────────────────────┤
│ Tidak terdeteksi│ Tidak ada overlay, tampilkan guide helper di tengah    │
└─────────────────┴────────────────────────────────────────────────────────┘

Overlay Bounding Box Label (saat mode Single Capture atau Live Inference):
- Background rectangle di atas bbox, padding 8dp
- Teks: "{Label} {confidence*100:.1f}%"  → contoh: "Anemia 87.4%"
- Warna background = warna status
- Warna teks = putih
```

### 6.2 `OverlayCanvas.kt` — Compose Canvas
```kotlin
@Composable
fun ConjunctivaOverlay(
    segmentationResult: ConjunctivaSegmentor.SegmentationResult?,
    classificationResult: AnemiaClassifier.ClassificationResult?,
    frameSize: Size,
    showClassificationOverlay: Boolean,   // hanya true saat mode classify aktif
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        segmentationResult ?: return@Canvas

        val cls = classificationResult
        val color = when {
            cls == null       -> Color(0xFF007AFF)   // Biru — hanya segmentasi
            cls.isAnemic      -> Color(0xFFFF3B30)   // Merah — Anemia
            else              -> Color(0xFF34C759)   // Hijau — Non-Anemia
        }
        val fillAlpha = if (cls == null) 0.25f else 0.30f

        // Scale faktor: frame asli → layar
        val scaleX = size.width / frameSize.width
        val scaleY = size.height / frameSize.height

        // 1. Bangun path polygon
        val path = Path()
        segmentationResult.polygon.forEachIndexed { i, pt ->
            val sx = pt.x * scaleX
            val sy = pt.y * scaleY
            if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        path.close()

        // 2. Gambar fill alpha
        drawPath(path, color.copy(alpha = fillAlpha), style = Fill)

        // 3. Gambar stroke tepi
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))

        // 4. Gambar vertex dots
        segmentationResult.polygon.forEach { pt ->
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(pt.x * scaleX, pt.y * scaleY))
        }

        // 5. Label box — hanya saat showClassificationOverlay = true
        if (showClassificationOverlay && cls != null) {
            val bbox = segmentationResult.boundingBox
            val bx1 = bbox.left * scaleX
            val by1 = bbox.top * scaleY
            val labelText = "${cls.label} ${(cls.confidence * 100).roundToInt()}%"
            // Gambar rect background + teks label
            drawRoundRect(
                color = color,
                topLeft = Offset(bx1, by1 - 36.dp.toPx()),
                size = Size(180.dp.toPx(), 28.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            // drawText membutuhkan TextMeasurer — inject via parameter
        }
    }
}
```

---

## 📌 BAGIAN 7 — FITUR SIMPAN HASIL (SAVE DENGAN MASKING)

### 7.1 Spesifikasi Simpan
```
Trigger: Setelah klasifikasi selesai (mode Single Capture), tampilkan bottom sheet
         dengan tombol "Simpan / Save / บันทึก"

Yang disimpan:
1. Bitmap hasil = frame ASLI (non-CLAHE) + overlay mask alpha fill pada area konjungtiva
   → Bukan frame CLAHE yang dipakai untuk inference
   → Alpha fill identik dengan overlay di layar (warna + opacity sesuai hasil klasifikasi)
   → Polygon di-draw langsung ke Bitmap menggunakan Canvas.drawPath()

2. Metadata (disimpan ke Room database):
   - Timestamp (Unix ms)
   - Label hasil: "Anemia" / "Non-Anemia"
   - Confidence Anemia: score[0]
   - Confidence Non-Anemia: score[1]
   - Nama file gambar yang disimpan
   - Mode yang digunakan: "single_capture" atau "live_inference"

3. File gambar: Simpan ke MediaStore (public galeri) menggunakan MediaStore.Images API
   → Kompatibel Android 10+ (minSdk 30, tidak perlu WRITE_EXTERNAL_STORAGE)
   → Format: JPEG, quality 95
   → Nama file: "anemia_YYYYMMDD_HHmmss.jpg"
   → Album/folder: "AnemiaDetector"

4. Konfirmasi UI setelah simpan:
   → Snackbar: "Tersimpan di Galeri" / "Saved to Gallery" / "บันทึกลงแกลเลอรีแล้ว"
   → Icon centang hijau
```

### 7.2 `BitmapUtils.kt` — generateMaskedBitmap()
```kotlin
// Fungsi untuk generate bitmap dengan overlay mask polygon
fun generateMaskedBitmap(
    originalFrame: Bitmap,
    polygon: List<PointF>,
    isAnemic: Boolean?,           // null = segmentation only
    originalFrameWidth: Int,
    originalFrameHeight: Int
): Bitmap {
    val result = originalFrame.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(result)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    val color = when (isAnemic) {
        true  -> android.graphics.Color.parseColor("#FFFF3B30")
        false -> android.graphics.Color.parseColor("#FF34C759")
        null  -> android.graphics.Color.parseColor("#FF007AFF")
    }

    // Fill alpha
    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        alpha = 77  // ~30% opacity (77/255 ≈ 0.302)
        style = android.graphics.Paint.Style.FILL
    }
    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        alpha = 255
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }

    val path = android.graphics.Path()
    polygon.forEachIndexed { i, pt ->
        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    path.close()

    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)

    return result
}
```

---

## 📌 BAGIAN 8 — UI/UX LENGKAP

### 8.1 Layout CameraScreen
```
┌────────────────────────────────────────────────────────┐
│ [StatusBar — transparant overlay]                       │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │                                                  │  │
│  │  ┌─────────────────────────────────────────────┐ │  │
│  │  │  [CAMERA PREVIEW — FULLSCREEN 16:9]         │ │  │
│  │  │                                             │ │  │
│  │  │  [POLYGON OVERLAY CANVAS]                   │ │  │
│  │  │     → Polygon fill alpha                    │ │  │
│  │  │     → Vertex dots                           │ │  │
│  │  │     → Label box (saat classify aktif)       │ │  │
│  │  │                                             │ │  │
│  │  │  [GUIDE BOX] — saat tidak ada deteksi       │ │  │
│  │  │  "Arahkan ke konjungtiva mata bawah"        │ │  │
│  │  └─────────────────────────────────────────────┘ │  │
│  │                                                  │  │
│  │  ┌──────────────────────────────────────────────┐ │  │
│  │  │ STATUS CHIP (saat mode live inference):      │ │  │
│  │  │  🔴 ANEMIA  87.4%  |  🟢 Non-Anemia 12.6%   │ │  │
│  │  └──────────────────────────────────────────────┘ │  │
│  │                                                  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ BOTTOM ACTION BAR:                              │  │
│  │  [🔦Torch] [🔄Flip] [📸 CAPTURE] [⚡Live] [📋Hist]│  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 8.2 CaptureResultSheet (Bottom Sheet setelah Capture)
```
┌──────────────────────────────────────────────────────┐
│ ─────── (drag handle)                               │
│                                                      │
│  [PREVIEW IMAGE — hasil dengan mask overlay, 200dp]  │
│                                                      │
│  HASIL ANALISIS KONJUNGTIVA                          │
│  ┌──────────────────────────────────────────────┐    │
│  │  🔴 ANEMIA                    87.4%          │    │ ← Card merah
│  └──────────────────────────────────────────────┘    │
│                                                      │
│  SKOR DETAIL:                                        │
│  Anemia      [████████████████░░░░]  87.4%           │
│  Non-Anemia  [████░░░░░░░░░░░░░░░░]  12.6%           │
│                                                      │
│  ⚠️ Hasil ini bukan diagnosis medis. Konsultasikan   │
│     dengan dokter untuk diagnosis yang akurat.       │
│                                                      │
│  [💾 Simpan]           [✕ Tutup]                    │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### 8.3 Peringatan Live Inference (Dialog Wajib)
Dialog ini WAJIB muncul setiap kali user mengaktifkan mode live inference.
User tidak dapat bypass dialog ini.

```kotlin
@Composable
fun LiveInferenceWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.live_inference_warning_title)) },
        text = { Text(stringResource(R.string.live_inference_warning_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.continue_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn))
            }
        }
    )
}
// strings/strings.xml (Indonesia — default):
// live_inference_warning_title = "Peringatan Mode Live"
// live_inference_warning_body  = "Mode inferensi langsung akan memproses gambar setiap 1 detik.
//                                 Hal ini dapat menyebabkan perangkat sedikit melambat atau cepat panas.
//                                 Lanjutkan?"
// strings/strings-en.xml (English):
// live_inference_warning_body  = "Live inference mode will process images every 1 second.
//                                 This may cause the device to slow down or become warm. Continue?"
// strings/strings-th.xml (Thai):
// live_inference_warning_body  = "โหมดการอนุมานสดจะประมวลผลภาพทุก 1 วินาที
//                                 ซึ่งอาจทำให้อุปกรณ์ทำงานช้าลงหรือร้อนขึ้น ต้องการดำเนินการต่อหรือไม่?"
```

### 8.4 OnBoarding Screen (Tampil Pertama Kali)
```
Onboarding 3 halaman — ditampilkan HANYA saat pertama kali install/buka.
Simpan state "sudah_onboarding" ke DataStore Preferences.

Halaman 1: "Apa itu Aplikasi Ini?"
  → Ilustrasi mata + konjungtiva
  → Penjelasan singkat tentang deteksi anemia via konjungtiva

Halaman 2: "Cara Penggunaan"
  → Langkah 1: Buka kelopak mata bawah
  → Langkah 2: Arahkan kamera ke area merah muda
  → Langkah 3: Tunggu polygon muncul, lalu klik tombol kamera
  → Langkah 4: Lihat hasil dan simpan jika perlu

Halaman 3: "Peringatan Penting"
  → "Aplikasi ini BUKAN alat diagnosis medis"
  → "Hasil tidak menggantikan pemeriksaan dokter"
  → "Gunakan sebagai screening awal saja"
  → Tombol: "Saya Mengerti, Mulai" / "I Understand, Start" / "ฉันเข้าใจแล้ว เริ่มต้น"
```

### 8.5 HistoryScreen
```
List pemeriksaan tersimpan dari Room database, diurutkan terbaru di atas.

Setiap item:
  - Thumbnail gambar hasil (80×80dp)
  - Label + confidence utama (misal "🔴 Anemia 87.4%")
  - Timestamp (tanggal dan jam)
  - Swipe-to-delete (dengan konfirmasi)

Filter: All / Anemia / Non-Anemia
Sort: Terbaru / Terlama

Detail item (tap → layar detail):
  - Gambar penuh dengan mask overlay
  - Kedua skor detail
  - Timestamp
  - Tombol share + delete
```

### 8.6 Dark Mode
```
Mendukung system dark mode Android (mengikuti preferensi sistem).
User juga bisa override secara manual di SettingsScreen.
Simpan preferensi ke DataStore.

Color tokens Material 3 untuk Dark:
- Background: #121212 (bukan pitch black)
- Surface: #1E1E1E
- Overlay polygon biru: tetap #007AFF
- Overlay anemia merah: tetap #FF3B30 (lebih visible di dark)
- Overlay non-anemia hijau: tetap #34C759
```

### 8.7 Language Settings
```
3 bahasa: Indonesia (default), English, Thai
Runtime language switching — update AppCompatDelegate locale DAN Context locale
Simpan pilihan ke DataStore Preferences
Apply segera tanpa restart Activity (gunakan updateResources() pattern)

SettingsScreen menampilkan:
  → Dropdown / Radio: "Bahasa Indonesia" | "English" | "ภาษาไทย"
```

---

## 📌 BAGIAN 9 — ROOM DATABASE

### 9.1 Entity
```kotlin
@Entity(tableName = "examinations")
data class ExaminationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,                    // System.currentTimeMillis()
    val labelAnemia: Float,                 // score[0] dari allScores
    val labelNonAnemia: Float,              // score[1] dari allScores
    val predictedLabel: String,             // "Anemia" atau "Non-Anemia"
    val confidence: Float,                  // max(score[0], score[1])
    val imagePath: String,                  // Path di MediaStore atau internal storage
    val mode: String                        // "single_capture" atau "live_inference"
)
```

### 9.2 DAO
```kotlin
@Dao
interface ExaminationDao {
    @Query("SELECT * FROM examinations ORDER BY timestamp DESC")
    fun getAllExaminations(): Flow<List<ExaminationEntity>>

    @Query("SELECT * FROM examinations WHERE predictedLabel = :label ORDER BY timestamp DESC")
    fun getByLabel(label: String): Flow<List<ExaminationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(examination: ExaminationEntity): Long

    @Delete
    suspend fun delete(examination: ExaminationEntity)

    @Query("DELETE FROM examinations")
    suspend fun deleteAll()
}
```

---

## 📌 BAGIAN 10 — ALUR INFERENCE LENGKAP PER FRAME

### 10.1 Mode Single Capture — Pipeline Detail
```
[User tap tombol Capture]
        │
        ▼ [Main Thread]
Ambil frame terakhir dari CameraX buffer (sudah dalam bentuk Bitmap RGBA 1280×720)
        │
        ▼ [Dispatchers.Default — launch coroutine]
Step 1: GrayWorldWhiteBalance.apply(bitmap)
        → Input: Bitmap RGBA 1280×720
        → Output: Bitmap RGBA 1280×720 (white-balanced)
        │
        ▼
Step 2: AdaptiveGammaCorrector.apply(bitmap)
        → Output: Bitmap RGBA 1280×720 (gamma-corrected)
        │
        ▼
Step 3: LetterboxResizer.resize(bitmap, targetSize=224)
        → Output: Bitmap RGBA 224×224 (letterboxed)
        │
        ▼
Step 4: BilateralFilterProcessor.apply(bitmap)
        → Output: Bitmap RGBA 224×224 (denoised)
        │
        ▼
Step 5: AdaptiveCLAHEProcessor.apply(bitmap)
        → Output: Bitmap RGBA 224×224 (CLAHE enhanced)
        [INI ADALAH "processedBitmap" yang dipakai untuk SEMUA inferensi]
        │
        ▼
Step 6: ConjunctivaSegmentor.segment(processedBitmap, origW=1280, origH=720)
        → Resize internal ke 320×320 untuk model
        → Run TFLite INT8 inference (CPU, 4 threads)
        → Parse output → NMS sudah embedded
        → Scale polygon ke 1280×720
        → Adaptive Epsilon → 6-15 titik
        → Pilih SEGMEN TERBESAR (argmax area)
        → Output: SegmentationResult? (nullable)
        │
        ├── [null] → Emit InferenceState.NoDetection
        │           → Tampilkan Snackbar: "Konjungtiva tidak terdeteksi"
        │
        ▼ [non-null]
Step 7: Crop konjungtiva dari processedBitmap
        → Terapkan polygon mask sebagai alpha mask
        → Crop berdasarkan bounding box
        → Validasi: crop.width > 0 && crop.height > 0
        │
        ▼
Step 8: AnemiaClassifier.classify(conjunctivaCrop)
        → Resize ke inputSize × inputSize (dinamis dari model)
        → Normalize float32 / 255.0
        → Run TFLite FLOAT32 inference (CPU, 4 threads)
        → Output: ClassificationResult(label, confidence, allScores[2], isAnemic)
        │
        ▼ [Dispatchers.Main]
Step 9: Emit InferenceState.Success(segResult, classResult)
        → Update ViewModel StateFlow
        → Tampilkan CaptureResultSheet
        → Tampilkan overlay dengan warna sesuai label
```

### 10.2 Mode Live Inference — Pipeline
```
[Setiap 1000ms via coroutine delay]
        │
        ▼
Ambil frame terbaru dari buffer
        │
        ▼
Jalankan FULL pipeline identik dengan Single Capture (Steps 1-9)
        │
        ▼ [Dispatchers.Main]
Update hasil di overlay tanpa bottom sheet
Animasi transisi warna polygon jika label berubah
```

### 10.3 Mode Live Segmentation (Default) — Pipeline
```
[Setiap frame (target ≥30 FPS), dengan frame skip adaptif]
        │
        ▼
Ambil frame
        │
        ▼ [Hanya jika >= SEG_INTERVAL_MS sejak inference terakhir]
Langsung jalankan:
  Step 1-5: Full preprocessing (CLAHE pipeline)
  Step 6: Segmentasi saja
        │
        ▼
Update polygon overlay (biru, alpha 0.25, tanpa label klasifikasi)
        │
[Frame lain selama interval] → hanya update preview tanpa inference
```

---

## 📌 BAGIAN 11 — EDGE CASES WAJIB DIIMPLEMENTASIKAN

### 11.1 Tidak Ada Konjungtiva Terdeteksi
```kotlin
// Tampilkan guide overlay di tengah layar:
// - Rectangle putus-putus dengan pulse animation
// - Teks di dalam: "Arahkan ke konjungtiva mata bawah"
//   (dengan ikon mata + panah ke arah kelopak bawah)
// JANGAN crash, JANGAN tampilkan error fatal
```

### 11.2 Crop Kosong Setelah Masking
```kotlin
// Validasi SEBELUM classify:
if (crop.width <= 0 || crop.height <= 0 || crop.byteCount == 0) {
    emit(InferenceState.NoDetection("Area konjungtiva terlalu kecil"))
    return@launch
}
```

### 11.3 Model File Tidak Ditemukan di Assets
```kotlin
// Tampilkan ErrorScreen khusus (bukan crash):
// - Icon error
// - Teks: "Model tidak ditemukan. Reinstall aplikasi."
// - Tombol: "Hubungi Support"
// - Log ke Logcat dengan tag "ModelLoadError"
// JANGAN biarkan NullPointerException atau FileNotFoundException muncul ke user
```

### 11.4 Permission Kamera Ditolak
```kotlin
// Tampilkan layar khusus (BUKAN finish()):
// - Penjelasan mengapa kamera diperlukan
// - Tombol "Buka Pengaturan" → startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
// - Jika ditolak permanent: tombol menuju pengaturan sistem
```

### 11.5 Memory Management — WAJIB
```kotlin
// Di CameraViewModel.onCleared():
override fun onCleared() {
    super.onCleared()
    segmentor.close()      // interpreter.close() + release semua Mat
    classifier.close()     // interpreter.close()
    cameraExecutor.shutdown()
    // Recycle semua Bitmap yang tidak lagi digunakan
}

// Di setiap inference: recycle Bitmap intermediate setelah tidak dipakai
// Gunakan bitmap.recycle() dengan sadar — jangan biarkan OOM
```

### 11.6 Aplikasi Di-background Saat Live Inference Aktif
```kotlin
// Di onStop() atau LifecycleObserver:
// HENTIKAN live inference coroutine
// Restart saat onStart() jika mode live masih aktif
// JANGAN jalankan inference saat aplikasi tidak visible
```

### 11.7 Rotasi Layar
```kotlin
// android:screenOrientation="portrait" di Manifest
// Aplikasi HANYA berjalan portrait — tidak perlu handle landscape
```

---

## 📌 BAGIAN 12 — PERFORMANCE TARGETS

| Metrik | Target | Minimum Acceptable |
|---|---|---|
| Camera preview FPS | ≥ 30 FPS | ≥ 25 FPS |
| Segmentasi latency | < 100ms | < 200ms |
| Klasifikasi latency (single) | < 150ms | < 300ms |
| Full pipeline single capture | < 400ms | < 700ms |
| Memory usage (heap) | < 200MB | < 350MB |
| APK size | < 40MB | < 60MB |
| Database query | < 50ms | < 100ms |
| Cold start time | < 2 detik | < 4 detik |

---

## 📌 BAGIAN 13 — LOKALISASI (i18n) TRILINGUAL

### 13.1 String Resources Kritis
```xml
<!-- res/values/strings.xml — INDONESIA (DEFAULT) -->
<string name="app_name">Deteksi Anemia</string>
<string name="live_inference_warning_title">Mode Inferensi Langsung</string>
<string name="live_inference_warning_body">Mode ini memproses gambar setiap 1 detik dan dapat menyebabkan perangkat melambat atau cepat panas. Lanjutkan?</string>
<string name="result_anemia">Anemia</string>
<string name="result_non_anemia">Non-Anemia</string>
<string name="no_conjunctiva">Arahkan kamera ke konjungtiva mata bawah</string>
<string name="saved_to_gallery">Tersimpan di Galeri</string>
<string name="medical_disclaimer">Hasil ini bukan diagnosis medis. Konsultasikan dengan dokter.</string>
<string name="btn_capture">Ambil Gambar</string>
<string name="btn_save">Simpan</string>
<string name="btn_close">Tutup</string>
<string name="btn_continue">Lanjutkan</string>
<string name="btn_cancel">Batal</string>

<!-- res/values-en/strings.xml — ENGLISH -->
<string name="app_name">Anemia Detector</string>
<string name="live_inference_warning_body">This mode processes images every 1 second and may cause the device to slow down or heat up. Continue?</string>
<string name="no_conjunctiva">Aim the camera at the lower conjunctiva</string>
<string name="medical_disclaimer">This is not a medical diagnosis. Consult a doctor.</string>

<!-- res/values-th/strings.xml — THAI -->
<string name="app_name">ตรวจจับโรคโลหิตจาง</string>
<string name="live_inference_warning_body">โหมดนี้จะประมวลผลภาพทุก 1 วินาที และอาจทำให้อุปกรณ์ทำงานช้าลงหรือร้อนขึ้น ต้องการดำเนินการต่อหรือไม่?</string>
<string name="no_conjunctiva">หันกล้องไปที่เยื่อบุตาส่วนล่าง</string>
<string name="medical_disclaimer">ผลลัพธ์นี้ไม่ใช่การวินิจฉัยทางการแพทย์ โปรดปรึกษาแพทย์</string>
```

---

## 📌 BAGIAN 14 — ANTI-PATTERNS (LARANGAN KERAS)

1. **DILARANG** menjalankan inference (segmentasi atau klasifikasi) di Main Thread
2. **DILARANG** membuat `Interpreter` baru setiap frame — wajib singleton via Hilt `@Singleton`
3. **DILARANG** mengabaikan `imageProxy.close()` setelah analisis — menyebabkan camera hang
4. **DILARANG** menggunakan GPU Delegate atau NNAPI Delegate — CPU only sesuai spesifikasi
5. **DILARANG** melakukan color space conversion yang salah:
   - Android Bitmap = RGB (bukan BGR)
   - `Utils.bitmapToMat()` → menghasilkan RGBA Mat (bukan BGR)
   - Konversi wajib: `COLOR_RGBA2BGR` (bukan `COLOR_RGB2BGR`)
   - Setelah selesai: `COLOR_BGR2RGBA` sebelum `matToBitmap()`
6. **DILARANG** hardcode input size model — gunakan `interpreter.getInputTensor(0).shape()[1]`
7. **DILARANG** menggunakan `Thread.sleep()` — gunakan `delay()` dari Kotlin Coroutines
8. **DILARANG** menggunakan Camera API lama (`android.hardware.Camera`) — wajib CameraX
9. **DILARANG** menyimpan reference ke Bitmap yang sudah di-recycle
10. **DILARANG** menjalankan live inference saat aplikasi di background
11. **DILARANG** menampilkan live inference result di bottom sheet — hanya di overlay
12. **DILARANG** skip onboarding untuk first-launch — cek DataStore dan tampilkan
13. **DILARANG** menyimpan gambar tanpa masking polygon — gambar simpan HARUS include overlay
14. **DILARANG** menampilkan hanya satu class score — WAJIB tampilkan keduanya
15. **DILARANG** melewati dialog peringatan live inference — dialog tidak bisa di-bypass
16. **DILARANG** blur/downscale preview kamera — pertahankan resolusi 1280×720 penuh

---

## 📌 BAGIAN 15 — DELIVERABLES BERURUTAN

### Prioritas 1 — Preprocessing Engine (Fondasi)
- [x] `GrayWorldWhiteBalance.kt`
- [x] `AdaptiveGammaCorrector.kt`
- [x] `LetterboxResizer.kt`
- [x] `BilateralFilterProcessor.kt`
- [x] `AdaptiveCLAHEProcessor.kt`
- [x] `RunPreprocessingUseCase.kt` (orchestrates semua 5 langkah)

### Prioritas 2 — Inference Engine
- [x] `ConjunctivaSegmentor.kt` (INT8, 320×320, adaptive epsilon polygon)
- [x] `AnemiaClassifier.kt` (FLOAT32, dynamic input size, expose all scores)
- [ ] `InferenceRepository.kt` + `InferenceRepositoryImpl.kt`
- [ ] `RunSegmentationUseCase.kt`
- [ ] `RunClassificationUseCase.kt`

### Prioritas 3 — Camera & ViewModel
- [ ] `CameraViewModel.kt` (StateFlow, 3 mode, frame buffer)
- [ ] `CameraUtils.kt` (YUV→Bitmap, frame skip throttle)
- [ ] `PermissionUtils.kt`
- [x] `BitmapUtils.kt` (generateMaskedBitmap, crop, normalize)
- [ ] `PolygonUtils.kt` (area, argmax, adaptive epsilon, mask fill)

### Prioritas 4 — UI Layer
- [ ] `CameraScreen.kt` (main screen Compose)
- [ ] `OverlayCanvas.kt` (polygon alpha fill)
- [ ] `CaptureResultSheet.kt` (bottom sheet hasil)
- [ ] `LiveInferenceWarningDialog.kt`
- [ ] `OnboardingScreen.kt` (3 halaman)
- [ ] `HistoryScreen.kt`
- [ ] `SettingsScreen.kt` (bahasa, tema, mode)

### Prioritas 5 — Data Layer
- [x] `ExaminationEntity.kt` + `ExaminationDao.kt`
- [x] `AppDatabase.kt`
- [x] `ExaminationRepository.kt`
- [x] `SaveExaminationUseCase.kt` (simpan ke Room + MediaStore)
- [x] `GetHistoryUseCase.kt`

### Prioritas 6 — Configuration & DI
- [x] `build.gradle.kts` (app + project)
- [x] `AndroidManifest.xml`
- [ ] `AppModule.kt` + `DatabaseModule.kt`
- [x] `AnemiaApp.kt` (Application class + Hilt init)
- [x] `proguard-rules.pro`
- [x] `res/xml/locales_config.xml`
- [ ] `strings.xml` trilingual (ID + EN + TH)

### Prioritas 7 — Testing
- [ ] Unit test preprocessing (visual equivalence dengan Python output)
- [ ] Unit test classifier (label mapping, score range 0–1)
- [ ] Unit test polygon area argmax selection
- [ ] Unit test adaptive epsilon (output 6–15 titik)
- [ ] Instrumented test overlay warna (merah/hijau/biru)

---

## 📌 BAGIAN 16 — CATATAN IMPLEMENTASI KRITIS

### 16.1 Tentang Output Shape Model Segmentasi YOLOv26n-seg TFLite
Model `best_int8.tflite` dieksport dengan `nms=True` (embedded NMS).
Output shape akan berbeda dari model tanpa NMS. Lakukan pengecekan runtime:
```kotlin
val outputCount = interpreter.outputTensorCount
for (i in 0 until outputCount) {
    Log.d("ModelInfo", "Output $i shape: ${interpreter.getOutputTensor(i).shape().contentToString()}")
    Log.d("ModelInfo", "Output $i dtype: ${interpreter.getOutputTensor(i).dataType()}")
}
```
Parse output sesuai shape yang ditemukan. Dokumentasikan shape di komentar kode.

### 16.2 Tentang INT8 Input untuk Segmentasi
```kotlin
// Cek data type input tensor sebelum membuat ByteBuffer
val inputDataType = interpreter.getInputTensor(0).dataType()
// DataType.UINT8 → gunakan ByteBuffer tanpa float, nilai 0-255
// DataType.FLOAT32 → normalize /255.0 seperti biasa
```

### 16.3 Tentang Koordinat Polygon — Dua Tahap Scaling
Model segmentasi dijalankan pada 320×320, frame asli 1280×720.
Overlay Canvas beroperasi pada koordinat layar yang berbeda dari frame.
Ada DUA tahap scaling yang HARUS dilakukan:

```
Step A: Scale polygon dari model space ke frame space
  poly_x_frame = poly_x_model * (1280 / 320) = poly_x_model * 4.0
  poly_y_frame = poly_y_model * (720 / 320) = poly_y_model * 2.25

Step B: Scale polygon dari frame space ke screen/canvas space (di OverlayCanvas)
  poly_x_screen = poly_x_frame * (canvasWidth / 1280)
  poly_y_screen = poly_y_frame * (canvasHeight / 720)
```

### 16.4 Tentang OpenCV Initialization
```kotlin
// Di Application class, WAJIB init OpenCV sebelum dipakai:
class AnemiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        }
    }
}
```

### 16.5 Tentang CLAHE Clip Limit Skala
```
Python (Kornia): clip_limit dalam range [0, 1] (gambar ternormalisasi)
  → CLAHE_CLIP_MIN = 8.0/255 ≈ 0.031
  → CLAHE_CLIP_MAX = 25.0/255 ≈ 0.098

Android (OpenCV): clip_limit dalam range [0, 255] (gambar uint8)
  → CLAHE_CLIP_MIN = 8.0
  → CLAHE_CLIP_MAX = 25.0

JANGAN salah konversi skala ini — menyebabkan CLAHE terlalu agresif atau tidak efektif.
```

### 16.6 Tentang Letterbox Offset untuk Crop
Saat preprocessing menggunakan letterbox resize dari 1280×720 ke 224×224,
ada offset padding hitam. Saat meng-crop konjungtiva dari gambar letterboxed,
bounding box sudah dalam koordinat letterboxed (224×224).
**Crop langsung dari gambar 224×224 — TIDAK perlu inverse letterbox untuk klasifikasi.**
Inverse letterbox hanya diperlukan jika ingin menampilkan polygon di frame asli 1280×720.

### 16.7 Tentang Thread Safety TFLite
```kotlin
// TFLite Interpreter TIDAK thread-safe.
// Gunakan satu Dispatcher.Default coroutine context per interpreter.
// JANGAN gunakan interpreter dari dua coroutine secara bersamaan.
// Hilt @Singleton memastikan hanya satu instance, tapi akses harus sequential.

// Gunakan Mutex:
private val inferenceMutex = Mutex()

suspend fun runSegmentation(bitmap: Bitmap): SegmentationResult? {
    return inferenceMutex.withLock {
        segmentor.segment(bitmap, ...)
    }
}
```

---

## 📌 BAGIAN 17 — CHECKLIST VALIDASI AKHIR

Sebelum menyatakan implementasi selesai, verifikasi SEMUA item berikut:

**Preprocessing:**
- [ ] Output CLAHE secara visual mendekati Python untuk gambar yang sama
- [ ] Urutan pipeline: WB → Gamma → Letterbox → Bilateral → CLAHE (tidak boleh diubah)
- [ ] Letterbox menghasilkan output 224×224 tanpa distorsi
- [ ] CLAHE hanya diterapkan pada channel L* (bukan seluruh gambar)

**Inference:**
- [ ] Segmentasi memilih polygon dengan AREA TERBESAR (bukan confidence tertinggi)
- [ ] Polygon memiliki 6–15 titik (adaptive epsilon)
- [ ] Klasifikasi menampilkan KEDUA score (Anemia + Non-Anemia)
- [ ] Label mapping: index 0 = Anemia, index 1 = Non-Anemia

**Camera & Mode:**
- [ ] Default mode: live segmentasi berjalan ≥30 FPS
- [ ] Tombol capture: klasifikasi pada frame terakhir (bukan jepret baru)
- [ ] Live inference: interval 1 detik via coroutine delay
- [ ] Dialog peringatan live inference muncul SETIAP kali mode diaktifkan

**UI:**
- [ ] Polygon fill alpha (bukan hanya outline)
- [ ] Warna: merah=Anemia, hijau=Non-Anemia, biru=segmentasi-saja
- [ ] Onboarding muncul hanya pertama kali
- [ ] Disclaimer medis tampil di result sheet
- [ ] Bottom sheet tidak muncul saat live inference — hanya overlay

**Simpan:**
- [ ] Gambar yang disimpan MENYERTAKAN mask polygon overlay
- [ ] Metadata tersimpan ke Room database
- [ ] File tersimpan ke MediaStore di album "AnemiaDetector"
- [ ] Konfirmasi Snackbar setelah simpan

**Lokalisasi:**
- [ ] Semua teks utama tersedia dalam 3 bahasa (ID, EN, TH)
- [ ] Language switcher berfungsi tanpa restart Activity

**Performance:**
- [ ] Preview kamera ≥30 FPS (tidak turun akibat inference)
- [ ] Tidak ada memory leak dari TFLite Interpreter (cek Android Profiler)
- [ ] `imageProxy.close()` dipanggil di setiap frame (termasuk yang di-skip)
- [ ] Live inference berhenti saat app di-background

**Stabilitas:**
- [ ] Tidak crash saat konjungtiva tidak terdeteksi
- [ ] Tidak crash saat crop berukuran 0
- [ ] Tidak crash saat model file tidak ada (tampilkan error screen)
- [ ] Permission flow lengkap: request → denied → settings deeplink

---

*Dokumen ini adalah spesifikasi final dan lengkap.*
*Setiap implementasi HARUS mengacu ke dokumen ini secara menyeluruh.*
*Tidak ada bagian yang boleh dilewati atau diringkas.*
*Tanyakan sebelum mengubah nilai parameter apapun yang disebutkan di sini.*
