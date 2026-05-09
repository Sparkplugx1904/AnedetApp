# ✅ RINGKASAN & CHECKLIST IMPLEMENTASI

> **Panduan lengkap untuk memastikan implementasi Android identik dengan `live_inference.py`**

---

## 🎯 TUJUAN DOKUMEN

Dokumen ini adalah **CHECKLIST FINAL** untuk memastikan:
1. Semua parameter identik dengan `live_inference.py`
2. Semua library terinstall dengan benar
3. Alur pipeline sesuai dengan Python
4. Tidak ada yang terlewat dari spesifikasi

---

## 📦 1. STRUKTUR FOLDER PROJECT

```
AnedetApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── models/
│   │   │   │       ├── segments/
│   │   │   │       │   └── best_int8.tflite          ← Model segmentasi
│   │   │   │       └── classify/
│   │   │   │           └── best_float32.tflite       ← Model klasifikasi
│   │   │   ├── java/com/example/anemiadetector/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   └── ExaminationDao.kt
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   └── ExaminationEntity.kt
│   │   │   │   │   │   └── AppDatabase.kt
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── DetectionResult.kt
│   │   │   │   │   │   ├── ClassificationResult.kt
│   │   │   │   │   │   └── InferenceState.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── InferenceRepository.kt
│   │   │   │   │       └── InferenceRepositoryImpl.kt
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   └── DatabaseModule.kt
│   │   │   │   ├── domain/
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── RunPreprocessingUseCase.kt
│   │   │   │   │       ├── RunSegmentationUseCase.kt
│   │   │   │   │       └── RunClassificationUseCase.kt
│   │   │   │   ├── ml/
│   │   │   │   │   ├── preprocessor/
│   │   │   │   │   │   ├── GrayWorldWhiteBalance.kt
│   │   │   │   │   │   ├── AdaptiveGammaCorrector.kt
│   │   │   │   │   │   ├── LetterboxResizer.kt
│   │   │   │   │   │   ├── BilateralFilterProcessor.kt
│   │   │   │   │   │   └── AdaptiveCLAHEProcessor.kt
│   │   │   │   │   ├── segmentation/
│   │   │   │   │   │   └── ConjunctivaSegmentor.kt
│   │   │   │   │   └── classification/
│   │   │   │   │       └── AnemiaClassifier.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── camera/
│   │   │   │   │   │   ├── CameraScreen.kt
│   │   │   │   │   │   ├── CameraViewModel.kt
│   │   │   │   │   │   ├── OverlayCanvas.kt
│   │   │   │   │   │   └── CaptureResultSheet.kt
│   │   │   │   │   ├── history/
│   │   │   │   │   │   ├── HistoryScreen.kt
│   │   │   │   │   │   └── HistoryViewModel.kt
│   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   └── OnboardingScreen.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   ├── utils/
│   │   │   │   │   ├── BitmapUtils.kt
│   │   │   │   │   ├── PolygonUtils.kt
│   │   │   │   │   ├── CameraUtils.kt
│   │   │   │   │   └── PermissionUtils.kt
│   │   │   │   ├── AnemiaApp.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   └── strings.xml                   ← Bahasa Indonesia (default)
│   │   │   │   ├── values-en/
│   │   │   │   │   └── strings.xml                   ← English
│   │   │   │   ├── values-th/
│   │   │   │   │   └── strings.xml                   ← Thai
│   │   │   │   └── xml/
│   │   │   │       └── locales_config.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📋 2. CHECKLIST DEPENDENCIES

### **build.gradle.kts (App Level)**

- [ ] `compileSdk = 35`
- [ ] `minSdk = 30`
- [ ] `targetSdk = 35`
- [ ] `jvmTarget = "17"`
- [ ] CameraX 1.3.4 (camera-camera2, camera-lifecycle, camera-view)
- [ ] TensorFlow Lite 2.16.1 (tensorflow-lite, tensorflow-lite-support)
- [ ] **TIDAK ADA** tensorflow-lite-gpu (CPU only!)
- [ ] OpenCV 4.9.0 (com.quickbirdstudios:opencv)
- [ ] Compose BOM 2024.06.00
- [ ] Material 3
- [ ] Hilt 2.51.1
- [ ] Room 2.6.1
- [ ] Navigation Compose 2.8.0
- [ ] Coroutines 1.8.1
- [ ] DataStore 1.1.1
- [ ] Coil 2.7.0
- [ ] Accompanist Permissions 0.35.1-alpha

### **Packaging Options**

- [ ] `excludes += "/META-INF/{AL2.0,LGPL2.1}"`
- [ ] `excludes += "META-INF/DEPENDENCIES"`
- [ ] `jniLibs.useLegacyPackaging = true`

### **ProGuard Rules**

- [ ] Keep TensorFlow Lite classes
- [ ] Keep OpenCV classes
- [ ] Keep Hilt classes
- [ ] Keep Room entities

---

## 🔧 3. CHECKLIST PREPROCESSING

### **SimpleCLAHEProcessor (dari live_inference.py)**

- [ ] `clipLimit = 2.0` (FIXED)
- [ ] `tileGridSize = (8, 8)` (FIXED)
- [ ] CLAHE hanya pada **L channel** dari LAB
- [ ] Channel a dan b **tidak diubah**
- [ ] Color space: BGR → LAB → BGR

### **Color Space Conversion**

- [ ] Bitmap → Mat menghasilkan **RGBA** (bukan RGB)
- [ ] Konversi RGBA → BGR sebelum processing
- [ ] Konversi BGR → RGBA sebelum kembali ke Bitmap
- [ ] **TIDAK** menggunakan `COLOR_RGB2BGR` (salah!)
- [ ] **GUNAKAN** `COLOR_RGBA2BGR` (benar!)

### **Full Pipeline (jika menggunakan CLAUDE.md Mode B)**

- [ ] Step 1: GrayWorldWhiteBalance (strength=0.8)
- [ ] Step 2: AdaptiveGammaCorrector (gamma 0.5-1.2)
- [ ] Step 3: LetterboxResizer (224×224)
- [ ] Step 4: BilateralFilter (9×9, sigma_color=25.5, sigma_space=1.5)
- [ ] Step 5: AdaptiveCLAHE (clip 8-25 adaptif)

**⚠️ REKOMENDASI:** Untuk Mode A (live_inference.py), cukup gunakan SimpleCLAHEProcessor saja.

---

## 🎥 4. CHECKLIST CAMERA

### **CameraX Setup**

- [ ] Resolusi **1280×720** (FIXED)
- [ ] `STRATEGY_KEEP_ONLY_LATEST`
- [ ] `OUTPUT_IMAGE_FORMAT_RGBA_8888`
- [ ] Default lens: `LENS_FACING_BACK`
- [ ] Torch control (on/off)
- [ ] Flip camera (front/back)
- [ ] Tap-to-focus (opsional)

### **Frame Processing**

- [ ] Frame skip dengan interval **100ms** untuk segmentasi
- [ ] Live inference dengan interval **1000ms** (1 detik)
- [ ] `imageProxy.close()` di **SETIAP** frame (termasuk yang di-skip)
- [ ] Mutex untuk thread-safe TFLite inference
- [ ] Recycle Bitmap setelah digunakan

---

## 🤖 5. CHECKLIST MODEL INFERENCE

### **ConjunctivaSegmentor (Segmentasi)**

- [ ] Model path: `models/segments/best_int8.tflite`
- [ ] Input size: **320×320** (FIXED)
- [ ] Confidence threshold: **0.35** (FIXED)
- [ ] Polygon selection: **AREA TERBESAR** (bukan confidence!)
- [ ] Adaptive Epsilon: 6-15 titik
- [ ] NMS sudah embedded dalam model
- [ ] CPU only (4 threads)

### **AnemiaClassifier (Klasifikasi)**

- [ ] Model path: `models/classify/best_float32.tflite`
- [ ] Input size: **DINAMIS** (baca dari model)
- [ ] Normalisasi: `/255.0` → float32
- [ ] Output: 2 elemen `[score_Anemia, score_NonAnemia]`
- [ ] Class mapping: `0=Anemia, 1=Non-Anemia`
- [ ] **EXPOSE semua scores** (jangan hanya argmax)
- [ ] CPU only (4 threads)

### **Polygon Area Calculation (Shoelace Formula)**

```kotlin
private fun computePolygonArea(polygon: List<PointF>): Float {
    var area = 0f
    val n = polygon.size
    for (i in 0 until n) {
        val j = (i + 1) % n
        area += polygon[i].x * polygon[j].y
        area -= polygon[j].x * polygon[i].y
    }
    return kotlin.math.abs(area) / 2f
}
```

- [ ] Implementasi Shoelace formula
- [ ] Pilih polygon dengan `maxByOrNull { area }`

---

## 🎨 6. CHECKLIST VISUALISASI

### **Overlay Canvas**

- [ ] Polygon dengan **alpha fill** (bukan hanya outline)
- [ ] Fill alpha: 0.25 (segmentasi saja), 0.30 (dengan klasifikasi)
- [ ] Stroke width: **3dp**
- [ ] Vertex dots: radius **4dp**

### **Warna**

- [ ] Biru `#007AFF` - Segmentasi saja (belum klasifikasi)
- [ ] Merah `#FF3B30` - Anemia
- [ ] Hijau `#34C759` - Non-Anemia

### **Label Box (saat klasifikasi aktif)**

- [ ] Background rectangle dengan warna sesuai hasil
- [ ] Teks: `"{label} {confidence*100:.1f}%"`
- [ ] Contoh: "Anemia 87.4%"
- [ ] Hanya tampil saat `showClassificationOverlay = true`

### **Guide Overlay (saat tidak ada deteksi)**

- [ ] Rectangle putus-putus dengan pulse animation
- [ ] Teks: "Arahkan ke konjungtiva mata bawah"
- [ ] Icon mata + panah

---

## 🔄 7. CHECKLIST MODE OPERASI

### **Mode 1: Live Segmentation (Default)**

- [ ] Aktif secara default saat app dibuka
- [ ] Hanya jalankan segmentasi (tidak klasifikasi)
- [ ] Interval: 100ms (max 10 FPS)
- [ ] Overlay: Polygon biru dengan alpha 0.25
- [ ] Target FPS: ≥30 FPS untuk preview kamera

### **Mode 2: Single Capture**

- [ ] Trigger: User tap tombol kamera
- [ ] Gunakan frame terakhir yang sudah di-buffer
- [ ] Full pipeline: Preprocessing → Segmentasi → Klasifikasi
- [ ] Tampilkan hasil di CaptureResultSheet (bottom sheet)
- [ ] Durasi: 200-500ms (boleh lebih lambat, bukan real-time)

### **Mode 3: Live Inference (Opsional)**

- [ ] Trigger: User aktifkan toggle
- [ ] **WAJIB** tampilkan warning dialog dulu
- [ ] Dialog **TIDAK BISA** di-bypass
- [ ] Interval: **1000ms** (1 detik)
- [ ] Full pipeline setiap 1 detik
- [ ] Update overlay (tidak tampilkan bottom sheet)
- [ ] Stop saat app di-background

---

## 💾 8. CHECKLIST FITUR SIMPAN

### **Generate Masked Bitmap**

- [ ] Gunakan frame **ASLI** (bukan frame CLAHE)
- [ ] Overlay polygon dengan alpha fill
- [ ] Warna sesuai hasil klasifikasi
- [ ] Alpha 30% (77/255)

### **Simpan ke MediaStore**

- [ ] Format: JPEG, quality 95
- [ ] Nama file: `anemia_YYYYMMDD_HHmmss.jpg`
- [ ] Album: "AnemiaDetector"
- [ ] Kompatibel Android 10+ (tidak perlu WRITE_EXTERNAL_STORAGE)

### **Simpan ke Room Database**

- [ ] Timestamp (Unix ms)
- [ ] Label: "Anemia" / "Non-Anemia"
- [ ] Confidence Anemia: `score[0]`
- [ ] Confidence Non-Anemia: `score[1]`
- [ ] Image path
- [ ] Mode: "single_capture" / "live_inference"

### **Konfirmasi UI**

- [ ] Snackbar: "Tersimpan di Galeri"
- [ ] Icon centang hijau

---

## 🌍 9. CHECKLIST LOKALISASI

### **Bahasa yang Didukung**

- [ ] Bahasa Indonesia (default)
- [ ] English
- [ ] ภาษาไทย (Thai)

### **String Resources Kritis**

- [ ] `app_name`
- [ ] `live_inference_warning_title`
- [ ] `live_inference_warning_body`
- [ ] `result_anemia`
- [ ] `result_non_anemia`
- [ ] `no_conjunctiva`
- [ ] `saved_to_gallery`
- [ ] `medical_disclaimer`
- [ ] `btn_capture`, `btn_save`, `btn_close`, `btn_continue`, `btn_cancel`

### **Runtime Language Switching**

- [ ] Simpan pilihan ke DataStore
- [ ] Apply tanpa restart Activity
- [ ] Update AppCompatDelegate locale

---

## 🎨 10. CHECKLIST UI/UX

### **CameraScreen**

- [ ] Preview kamera fullscreen
- [ ] Overlay canvas di atas preview
- [ ] Bottom action bar: Torch, Flip, Capture, Live, History
- [ ] Loading indicator saat processing
- [ ] Guide overlay saat tidak ada deteksi

### **CaptureResultSheet (Bottom Sheet)**

- [ ] Preview image dengan mask overlay (200dp)
- [ ] Card hasil: Merah (Anemia) / Hijau (Non-Anemia)
- [ ] Skor detail: Progress bar untuk kedua class
- [ ] Medical disclaimer
- [ ] Tombol: Simpan, Tutup

### **LiveInferenceWarningDialog**

- [ ] Icon warning (merah)
- [ ] Judul: "Peringatan Mode Live"
- [ ] Body: Penjelasan tentang device slowdown/heat
- [ ] Tombol: Lanjutkan, Batal
- [ ] **WAJIB** muncul setiap kali mode diaktifkan

### **OnboardingScreen**

- [ ] 3 halaman: Apa itu, Cara Penggunaan, Peringatan
- [ ] Tampil hanya pertama kali
- [ ] Simpan flag ke DataStore
- [ ] Tombol: "Saya Mengerti, Mulai"

### **HistoryScreen**

- [ ] List pemeriksaan (terbaru di atas)
- [ ] Thumbnail 80×80dp
- [ ] Label + confidence
- [ ] Timestamp
- [ ] Swipe-to-delete dengan konfirmasi
- [ ] Filter: All / Anemia / Non-Anemia

### **SettingsScreen**

- [ ] Pilihan bahasa (ID / EN / TH)
- [ ] Pilihan tema (Light / Dark / System)
- [ ] Mode inference (opsional)

### **Dark Mode**

- [ ] Mendukung system dark mode
- [ ] Override manual di Settings
- [ ] Color tokens Material 3
- [ ] Background: #121212, Surface: #1E1E1E

---

## 🧪 11. CHECKLIST TESTING

### **Unit Test**

- [ ] Preprocessing: Output visual identik dengan Python
- [ ] Classifier: Label mapping benar (0=Anemia, 1=Non-Anemia)
- [ ] Polygon area: Shoelace formula benar
- [ ] Adaptive epsilon: Output 6-15 titik

### **Instrumented Test**

- [ ] Overlay warna: Merah/Hijau/Biru sesuai hasil
- [ ] Camera permission flow
- [ ] Database CRUD operations

### **Manual Test**

- [ ] Preview kamera ≥30 FPS
- [ ] Segmentasi real-time smooth
- [ ] Single capture < 500ms
- [ ] Live inference tidak crash
- [ ] App tidak crash saat background
- [ ] Memory tidak leak (cek Android Profiler)

---

## ⚠️ 12. CHECKLIST ANTI-PATTERNS (LARANGAN)

- [ ] **TIDAK** jalankan inference di Main Thread
- [ ] **TIDAK** buat Interpreter baru setiap frame
- [ ] **TIDAK** abaikan `imageProxy.close()`
- [ ] **TIDAK** gunakan GPU Delegate atau NNAPI
- [ ] **TIDAK** salah konversi color space (RGB vs BGR)
- [ ] **TIDAK** hardcode input size model
- [ ] **TIDAK** gunakan `Thread.sleep()` (gunakan `delay()`)
- [ ] **TIDAK** gunakan Camera API lama
- [ ] **TIDAK** simpan reference Bitmap yang sudah recycle
- [ ] **TIDAK** jalankan live inference saat background
- [ ] **TIDAK** tampilkan live inference result di bottom sheet
- [ ] **TIDAK** skip onboarding untuk first-launch
- [ ] **TIDAK** simpan gambar tanpa masking polygon
- [ ] **TIDAK** tampilkan hanya satu class score
- [ ] **TIDAK** lewati dialog peringatan live inference
- [ ] **TIDAK** blur/downscale preview kamera

---

## 📊 13. PERFORMANCE TARGETS

| Metrik | Target | Minimum Acceptable |
|--------|--------|-------------------|
| Camera preview FPS | ≥ 30 FPS | ≥ 25 FPS |
| Segmentasi latency | < 100ms | < 200ms |
| Klasifikasi latency | < 150ms | < 300ms |
| Full pipeline | < 400ms | < 700ms |
| Memory usage | < 200MB | < 350MB |
| APK size | < 40MB | < 60MB |
| Database query | < 50ms | < 100ms |
| Cold start time | < 2 detik | < 4 detik |

---

## 🔍 14. VALIDASI AKHIR

### **Preprocessing**

- [ ] Output CLAHE visual identik dengan Python
- [ ] Urutan pipeline: WB → Gamma → Letterbox → Bilateral → CLAHE (jika Mode B)
- [ ] Atau: Hanya CLAHE (jika Mode A - live_inference.py)
- [ ] Letterbox 224×224 tanpa distorsi
- [ ] CLAHE hanya pada L channel

### **Inference**

- [ ] Segmentasi pilih polygon AREA TERBESAR
- [ ] Polygon 6-15 titik (adaptive epsilon)
- [ ] Klasifikasi tampilkan KEDUA score
- [ ] Label mapping: 0=Anemia, 1=Non-Anemia

### **Camera & Mode**

- [ ] Default: Live segmentation ≥30 FPS
- [ ] Capture: Klasifikasi frame terakhir
- [ ] Live inference: Interval 1 detik
- [ ] Dialog peringatan muncul setiap kali

### **UI**

- [ ] Polygon alpha fill (bukan outline saja)
- [ ] Warna: Merah=Anemia, Hijau=Non-Anemia, Biru=Segmentasi
- [ ] Onboarding pertama kali saja
- [ ] Disclaimer medis di result sheet
- [ ] Bottom sheet tidak muncul saat live inference

### **Simpan**

- [ ] Gambar dengan mask overlay
- [ ] Metadata ke Room
- [ ] File ke MediaStore
- [ ] Konfirmasi Snackbar

### **Lokalisasi**

- [ ] 3 bahasa (ID, EN, TH)
- [ ] Language switcher tanpa restart

### **Performance**

- [ ] Preview ≥30 FPS
- [ ] Tidak ada memory leak
- [ ] `imageProxy.close()` setiap frame
- [ ] Live inference stop saat background

### **Stabilitas**

- [ ] Tidak crash saat tidak ada deteksi
- [ ] Tidak crash saat crop kosong
- [ ] Tidak crash saat model tidak ada
- [ ] Permission flow lengkap

---

## 📝 15. CATATAN IMPLEMENTASI

### **OpenCV Initialization**

```kotlin
class AnemiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        }
    }
}
```

### **Model Loading Helper**

```kotlin
private fun loadModelBuffer(context: Context, modelPath: String): ByteBuffer {
    val assetFileDescriptor = context.assets.openFd(modelPath)
    val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = assetFileDescriptor.startOffset
    val declaredLength = assetFileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}
```

### **Thread Safety TFLite**

```kotlin
private val inferenceMutex = Mutex()

suspend fun runInference() {
    inferenceMutex.withLock {
        // Run TFLite inference
    }
}
```

---

## ✅ FINAL CHECKLIST

Sebelum menyatakan implementasi selesai, pastikan **SEMUA** item di bawah ini sudah ✅:

### **Kode**
- [ ] Semua file di struktur folder sudah dibuat
- [ ] Semua parameter identik dengan live_inference.py
- [ ] Semua library terinstall dengan benar
- [ ] ProGuard rules sudah ditambahkan

### **Model**
- [ ] `best_int8.tflite` di `assets/models/segments/`
- [ ] `best_float32.tflite` di `assets/models/classify/`
- [ ] Model loading berhasil (tidak crash)

### **Testing**
- [ ] Unit test preprocessing pass
- [ ] Instrumented test pass
- [ ] Manual test semua fitur berjalan

### **Performance**
- [ ] Preview kamera ≥30 FPS
- [ ] Segmentasi < 200ms
- [ ] Klasifikasi < 300ms
- [ ] Memory < 350MB
- [ ] APK < 60MB

### **UI/UX**
- [ ] Semua screen berfungsi
- [ ] Overlay polygon tampil dengan benar
- [ ] Bottom sheet tampil dengan benar
- [ ] Dialog warning tampil dengan benar
- [ ] Onboarding tampil pertama kali

### **Lokalisasi**
- [ ] 3 bahasa tersedia
- [ ] Language switcher berfungsi

### **Stabilitas**
- [ ] Tidak crash dalam kondisi apapun
- [ ] Memory tidak leak
- [ ] Permission flow lengkap

---

## 🎉 SELESAI!

Jika **SEMUA** checklist di atas sudah ✅, maka implementasi Android Anda **IDENTIK** dengan `live_inference.py`!

**Mode A (Referensi live_inference.py) - COMPLETE ✅**

---

**📌 CATATAN AKHIR:**

Dokumen ini adalah **MASTER CHECKLIST** untuk implementasi.

Gunakan sebagai panduan step-by-step dan validasi akhir.

Jika ada yang tidak jelas atau ragu, **TANYAKAN** - jangan asumsikan!

**Selamat mengimplementasikan! 🚀**
