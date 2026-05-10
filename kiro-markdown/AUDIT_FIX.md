# 🔍 AUDIT REPORT — AnedetApp
> Hasil analisis menyeluruh kode di repo `Sparkplugx1904/AnedetApp`
> Dikelompokkan berdasarkan tingkat keparahan: **🔴 Fatal → 🟠 Kritis → 🟡 Bug Nyata → 🟢 Fitur Belum Diimplementasikan**

---

## 🔴 BUG FATAL (App Tidak Bisa Jalan / Crash / Silent Fail Total)

---

### 🔴 [FATAL-1] `ImageProxy.toBitmap()` Salah Total → Kamera Tidak Memberikan Frame Apapun

**File:** `CameraScreen.kt` baris 538–542

```kotlin
// IMPLEMENTASI SEKARANG — SALAH TOTAL:
private fun ImageProxy.toBitmap(): android.graphics.Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) // ← INI SELALU RETURN NULL
}
```

**Kenapa fatal:** CameraX dikonfigurasi dengan `OUTPUT_IMAGE_FORMAT_RGBA_8888`. Format ini mengisi buffer `planes[0]` dengan **raw pixel bytes mentah** (R,G,B,A berurutan). `BitmapFactory.decodeByteArray()` hanya bisa decode **file gambar terenkoding** (JPEG/PNG). Memberikannya raw pixel bytes akan selalu menghasilkan `null` atau crash `NullPointerException`. Artinya **tidak ada satu frame pun yang berhasil masuk ke inference pipeline**.

**Fix wajib:**
```kotlin
private fun ImageProxy.toBitmap(): android.graphics.Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val buffer = planes[0].buffer
    buffer.rewind()
    bitmap.copyPixelsFromBuffer(buffer)
    return bitmap
}
```

---

### 🔴 [FATAL-2] Segmentasi Hanya Menghasilkan Rectangle, Bukan Polygon Mask Nyata

**File:** `ConjunctivaSegmentor.kt` baris 106–112

```kotlin
// IMPLEMENTASI SEKARANG — HARDCODED RECTANGLE:
// Convert bbox to polygon (simple rectangle for now)
// TODO: Decode mask coefficients untuk polygon yang lebih akurat
val polygon = listOf(
    PointF(x1Denorm, y1Denorm),
    PointF(x2Denorm, y1Denorm),
    PointF(x2Denorm, y2Denorm),
    PointF(x1Denorm, y2Denorm)
)
```

**Kenapa fatal:** Ini adalah fitur inti aplikasi. Seluruh algoritma Adaptive Epsilon di `PolygonUtils.kt` (ratusan baris kode) adalah **dead code** — tidak pernah dipanggil. Model segmentasi menghasilkan mask coefficients di `detection[6..37]` (32 koefisien) yang harus didecode bersama proto mask dari output tensor kedua. Tanpa ini, overlay yang tampil hanya kotak saja, bukan bentuk konjungtiva yang sebenarnya. Crop untuk klasifikasi juga jadi kotak, bukan area konjungtiva yang presisi.

**Yang harus dilakukan:**
- Periksa output tensor model di runtime (log shape-nya)
- Decode mask coefficients `detection[6..37]` dengan proto mask tensor output kedua
- Jalankan `sigmoid(proto @ coeff.T)` untuk reconstruct binary mask
- Ekstrak kontur dari binary mask
- Baru terapkan `PolygonUtils.getAdaptivePolygon()` ke kontur tersebut

---

### 🔴 [FATAL-3] Nama File Model di Kode vs File Aktual Tidak Konsisten

**File:** `ConjunctivaSegmentor.kt` baris 26

```kotlin
// KODE:
private const val MODEL_PATH = "models/segments/best_float16.tflite"

// FILE DI ASSETS:
app/src/main/assets/models/segments/best_float16.tflite  ← ada
app/src/main/assets/models/segments/best_int8.tflite     ← TIDAK ADA
```

**Masalah:** Model yang ada di assets adalah `best_float16.tflite` tapi kode di `ModelNotFoundScreen` (baris 576–577) mengatakan user harus menyediakan `best_int8.tflite`. Juga, comment di kode menyebutkan "FP16 model" tapi output shape yang diasumsikan `[1, 300, 38]` adalah format NMS-embedded yang biasanya untuk INT8. Harus diverifikasi apakah output shape `[1, 300, 38]` benar-benar sesuai model FP16 yang ada.

---

### 🔴 [FATAL-4] Camera Flip Tidak Bekerja

**File:** `CameraScreen.kt` baris 290–325

```kotlin
// AndroidView hanya punya factory — TIDAK ADA update lambda dan TIDAK ADA key():
AndroidView(
    factory = { ctx ->
        // ini hanya berjalan SEKALI saat pertama komposisi
        val cameraProvider = ...
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, ...)
        // saat cameraSelector state berubah → factory TIDAK dijalankan ulang
    },
    // update = {} ← TIDAK ADA
    modifier = modifier
)
```

**Kenapa fatal:** Jetpack Compose `AndroidView` tanpa `key()` atau `update` lambda hanya menjalankan `factory` satu kali. Ketika user menekan tombol flip, `cameraSelector` state berubah, rekomposisi terjadi, tapi factory tidak re-run. Kamera tidak beralih. Tombol flip adalah dead button.

**Fix:**
```kotlin
// Opsi 1: Wrap dengan key()
key(cameraSelector) {
    AndroidView(factory = { ctx -> ... })
}

// Opsi 2: Tambahkan update lambda yang rebind camera
AndroidView(
    factory = { ctx -> previewView },
    update = { _ ->
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
    }
)
```

---

## 🟠 BUG KRITIS (Fitur Tidak Jalan / Data Hilang / Memory Leak)

---

### 🟠 [KRITIS-1] Fitur Simpan Tidak Menyimpan Apapun ke Galeri Maupun Database

**File:** `CameraScreen.kt` baris 258–264

```kotlin
onSave = {
    // TODO: Implement save functionality  ← MASIH TODO
    scope.launch {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.saved_to_gallery) // ← Snackbar bohong
        )
        showResultSheet = false
    }
},
```

**Dampak:** Tombol "Simpan" di result sheet menampilkan snackbar "Tersimpan di Galeri" tapi:
1. **Tidak ada file yang ditulis ke MediaStore** — tidak ada kode `ContentValues`, `MediaStore.Images.Media.insert()`, atau `ContentResolver.insert()` di seluruh project
2. **Tidak ada rekaman yang disimpan ke Room** — `SaveExaminationUseCase` tidak pernah dipanggil dari `CameraScreen`
3. **History selalu kosong** — karena tidak ada yang pernah di-insert ke database

`SaveExaminationUseCase`, `ExaminationRepository`, `ExaminationDao`, `ExaminationEntity` — semua sudah ada tapi tidak pernah dipanggil dari mana pun.

---

### 🟠 [KRITIS-2] Preview Gambar di Result Sheet Selalu Kosong (null)

**File:** `CameraScreen.kt` baris 78, 233–235

```kotlin
var resultBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
// ...
// Generate masked bitmap for display
// TODO: Get original frame from viewModel  ← MASIH TODO
// For now, we'll handle this in the result sheet
```

**Dampak:** `CaptureResultSheet` menerima `resultBitmap = null`. Card preview gambar di result sheet selalu kosong — kotak abu-abu tanpa gambar. User tidak bisa melihat area konjungtiva yang dideteksi.

---

### 🟠 [KRITIS-3] Crop Konjungtiva Menggunakan Koordinat yang Salah (Letterbox Offset Diabaikan)

**File:** `CameraViewModel.kt` baris 281–295

```kotlin
private fun cropConjunctiva(preprocessed: Bitmap, bbox: RectF, ...): Bitmap {
    // MASALAH: Scaling langsung dari FRAME ke 224x224 tanpa memperhitungkan letterbox offset
    val scaleX = 224f / FRAME_WIDTH   // = 224/1280 = 0.175
    val scaleY = 224f / FRAME_HEIGHT  // = 224/720  = 0.3111
    ...
}
```

**Kenapa salah:** Setelah letterbox resize dari 1280×720 ke 224×224:
- `scale = 224 / 1280 = 0.175`
- `new_height = 720 × 0.175 = 126px`
- `y_offset = (224 - 126) / 2 = 49px` ← padding hitam di atas dan bawah

Konten gambar nyata ada di koordinat y: `[49, 175]` dalam bitmap 224×224.
Kode sekarang mengabaikan offset ini → crop mengambil area yang salah termasuk padding hitam → klasifikasi berjalan pada kotak hitam, bukan konjungtiva.

---

### 🟠 [KRITIS-4] Memory Leak: `lChannel` Mat Tidak Pernah Di-release di CLAHE Processor

**File:** `AdaptiveCLAHEProcessor.kt` baris 24–45

```kotlin
val lChannel = channels[0]           // ambil referensi ke channel L asli
clahe.apply(lChannel, lEnhanced)
channels[0] = lEnhanced              // channels[0] sekarang menunjuk ke lEnhanced
// ...
channels.forEach { it.release() }    // release lEnhanced (via channels[0]), a, b
// TAPI lChannel (Mat asli yang direferens tadi) TIDAK pernah di-release!
```

**Dampak:** Setiap frame preprocessing bocor satu OpenCV Mat ke native heap. Di mode live segmentation (10 FPS), ini 10 Mat bocor per detik → crash `OutOfMemoryError` setelah beberapa menit.

**Fix:** Tambahkan `lChannel.release()` setelah `channels[0] = lEnhanced`.

---

### 🟠 [KRITIS-5] Live Inference Tidak Berhenti Saat App Di-background

**File:** Tidak ada kode lifecycle observer di ViewModel maupun CameraScreen untuk stop live inference.

`liveInferenceJob` hanya berhenti di `onCleared()` (ViewModel destroyed) dan saat user manual toggle off. Tidak ada handler `Lifecycle.Event.ON_STOP` atau `ON_PAUSE`. Ketika user menekan tombol home dengan live inference aktif, inference tetap berjalan di background — mengonsumsi CPU, baterai, dan mencegah kamera di-release dengan benar.

---

### 🟠 [KRITIS-6] Tidak Ada Tombol Akses Settings dari Camera Screen

**File:** `CameraScreen.kt` — `BottomActionBar`

`onNavigateToSettings` diterima sebagai parameter `CameraScreen` tapi **tidak pernah dipanggil dari manapun** di `BottomActionBar`. Tidak ada tombol settings di tampilan kamera. User tidak bisa mengganti bahasa atau tema tanpa menavigasi ke history dulu (yang juga tidak ada tombol settings-nya).

---

### 🟠 [KRITIS-7] `AdaptiveGammaCorrector` — `channels` Mat Tidak Di-release

**File:** `AdaptiveGammaCorrector.kt` baris akhir

```kotlin
channels.forEach { it.release() }
```

Baris ini ada tapi `channels` adalah hasil `Core.split(lab, channels)` yang hanya berisi referensi. Variabel `lab` di-release, tapi setiap `Mat` dalam `channels` (L, a, b dari LAB) perlu di-release secara eksplisit. Cek bahwa `channels.forEach { it.release() }` memang ada — ya ada. **Ini sebenarnya OK.** Tapi ada bug berbeda: `rgba` Mat dikonversi RGBA→BGR in-place, kemudian LUT diterapkan ke `rgba` (yang sekarang BGR) menghasilkan `corrected`. Tapi `corrected` tidak di-release dengan benar jika exception terjadi — tidak ada try/finally.

---

## 🟡 BUG NYATA (Perilaku Salah / UX Rusak)

---

### 🟡 [BUG-1] Output Shape Model Diasumsikan Tanpa Verifikasi

**File:** `ConjunctivaSegmentor.kt` baris 82–84

```kotlin
// Model FP16 NMS-embedded output shape: [1, 300, 38]
// Format: [batch, max_detections, data]
val output0 = Array(1) { Array(300) { FloatArray(38) } }
```

Shape `[1, 300, 38]` adalah asumsi spekulatif. Jika shape aktual model berbeda (misalnya `[1, 38, 300]` atau `[1, 116, 8400]`), inference akan crash dengan `IllegalArgumentException: Cannot copy to a TensorFlowLite tensor`. Kode sudah ada log output shape di init tapi tidak digunakan untuk validasi buffer.

---

### 🟡 [BUG-2] History Screen — Thumbnail Gambar Tidak Ada

**File:** `HistoryScreen.kt`

`ExaminationEntity` menyimpan `imagePath: String` tapi karena save ke MediaStore tidak pernah diimplementasikan (KRITIS-1), `imagePath` selalu kosong string. Tidak ada kode load gambar (Coil/AsyncImage) di HistoryScreen untuk menampilkan thumbnail — hanya warna solid yang ditampilkan.

---

### 🟡 [BUG-3] Tombol Share di History Screen — Tidak Diimplementasikan

**File:** `HistoryScreen.kt`, `HistoryViewModel.kt`

Tidak ada fungsi share di `HistoryViewModel`. Tidak ada Intent share. Berkaitan dengan KRITIS-1 — tidak bisa share karena file gambar tidak pernah disimpan.

---

### 🟡 [BUG-4] `CameraPreview` — `cameraExecutor` Bocor Saat Rekomposisi

**File:** `CameraScreen.kt` baris 288, 332–336

```kotlin
val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
// ...
DisposableEffect(Unit) {
    onDispose { cameraExecutor.shutdown() }
}
```

`DisposableEffect(Unit)` hanya dispose saat composable meninggalkan komposisi sepenuhnya. Jika CameraPreview direkomposisi (misalnya karena flip kamera menggunakan `key()`), executor lama tidak di-shutdown, executor baru dibuat. Ini membuat thread leak.

---

### 🟡 [BUG-5] Status Chip Live Inference Hanya Menampilkan Satu Score

**File:** `CameraScreen.kt` — `StatusChip`

```kotlin
Text(
    text = "${classificationResult.label} ${(classificationResult.confidence * 100).toInt()}%",
    ...
)
```

Spesifikasi menyebutkan status chip harus menampilkan kedua score: `"🔴 ANEMIA 87.4% | 🟢 Non-Anemia 12.6%"`. Sekarang hanya menampilkan label pemenang saja. Score kedua tidak ditampilkan di overlay live inference.

---

### 🟡 [BUG-6] Warning Dialog Live Inference Hanya Muncul Sekali — Tidak Persistent

**File:** `CameraScreen.kt` baris 124

Spesifikasi menyebutkan dialog peringatan harus muncul **setiap kali** mode live inference diaktifkan. Implementasi sekarang menggunakan `showWarningDialog` state yang direset — ini sudah benar sebenarnya, tapi perlu dikonfirmasi bahwa tidak ada persisted "jangan tampilkan lagi" yang tidak sesuai spec.

---

### 🟡 [BUG-7] `processFrameForSegmentation` Membuat Dua Copy Bitmap Secara Bersamaan

**File:** `CameraViewModel.kt` baris 91–93

```kotlin
val frameCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
lastFrameBitmap?.recycle()
lastFrameBitmap = frameCopy.copy(Bitmap.Config.ARGB_8888, false)  // Copy KEDUA dari copy pertama
```

`frameCopy` dibuat, kemudian di-copy lagi untuk `lastFrameBitmap`. Setelah itu `frameCopy` digunakan untuk inference. Ini membuat 2 full-resolution bitmap (1280×720×4 = ~3.7MB tiap) untuk setiap frame, padahal satu saja cukup.

---

### 🟡 [BUG-8] `cropConjunctiva` Hardcode 224 — Tidak Konsisten dengan `LetterboxResizer`

**File:** `CameraViewModel.kt` baris 287–288

```kotlin
val scaleX = 224f / FRAME_WIDTH
val scaleY = 224f / FRAME_HEIGHT
```

Ukuran 224 di-hardcode di ViewModel padahal `LetterboxResizer` menggunakan `targetSize: Int` sebagai parameter. Jika suatu saat ukuran letterbox diubah, angka 224 di ViewModel tidak akan ikut berubah → silent mismatch.

---

## 🟢 FITUR YANG BELUM DIIMPLEMENTASIKAN SAMA SEKALI

---

### 🟢 [MISSING-1] Save ke MediaStore (Galeri Android)

Tidak ada kode yang menulis file gambar ke galeri Android. Yang diperlukan:
```kotlin
// Menggunakan MediaStore API (kompatibel Android 10+/minSdk 30):
val contentValues = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, "anemia_${timestamp}.jpg")
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnemiaDetector")
}
val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
uri?.let { contentResolver.openOutputStream(it)?.use { stream -> bitmap.compress(JPEG, 95, stream) } }
```

---

### 🟢 [MISSING-2] Pemanggilan `SaveExaminationUseCase` Setelah Klasifikasi

`SaveExaminationUseCase` → `ExaminationRepository` → `ExaminationDao` sudah lengkap, tapi tidak pernah dipanggil. Perlu dipanggil dari `CameraViewModel` saat `onSave` di result sheet ditekan.

---

### 🟢 [MISSING-3] Decode Mask Coefficients → Polygon Nyata dari Model Segmentasi

Output tensor model segmentasi `detection[6..37]` berisi 32 mask coefficients.
Ada output tensor kedua berisi proto masks. Keduanya harus di-decode untuk mendapatkan polygon konjungtiva yang sesungguhnya, bukan rectangle.

---

### 🟢 [MISSING-4] `resultBitmap` — Generate dan Kirim ke Result Sheet

`CameraViewModel` perlu menyimpan reference ke original frame (non-CLAHE), kemudian saat `captureAndClassify()` selesai, generate `generateMaskedBitmap()` dan expose-nya via StateFlow. `CameraScreen` perlu membaca bitmap ini dan meneruskannya ke `CaptureResultSheet`.

---

### 🟢 [MISSING-5] Tombol Settings di Camera Screen

Tidak ada tombol di `BottomActionBar` yang mengarahkan ke settings screen. `onNavigateToSettings` parameter ada tapi tidak terhubung ke tombol apapun. Bisa tambahkan icon `Settings` atau pindahkan via menu top bar.

---

### 🟢 [MISSING-6] Stop Live Inference Saat App Di-background

Perlu tambahkan `LifecycleEventObserver` di CameraScreen atau `DefaultLifecycleObserver` di ViewModel:
```kotlin
// Di CameraScreen:
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) viewModel.toggleLiveInference(false)
        if (event == Lifecycle.Event.ON_START && wasLiveInference) viewModel.toggleLiveInference(true)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

---

### 🟢 [MISSING-7] Thumbnail Gambar di History Screen

History screen tidak menampilkan thumbnail gambar dari pemeriksaan tersimpan. Perlu implementasi Coil `AsyncImage` dengan path dari `ExaminationEntity.imagePath`. Tergantung [MISSING-1] selesai dulu.

---

### 🟢 [MISSING-8] Fitur Share di History Screen

Tombol/aksi share tersebut direncanakan tapi tidak ada implementasi `Intent(Intent.ACTION_SEND)` di `HistoryViewModel` maupun `HistoryScreen`.

---

### 🟢 [MISSING-9] Vertex Dots di Overlay Canvas — Tidak Relevan Sebelum FATAL-2 Fix

`OverlayCanvas.kt` sudah menggambar vertex dots, tapi karena polygon selalu 4 titik (rectangle dari FATAL-2), dots hanya muncul di pojok kotak, bukan di kontur konjungtiva. Fix setelah FATAL-2 selesai.

---

## 📊 RINGKASAN PRIORITAS PERBAIKAN

| # | ID | Deskripsi Singkat | Dampak |
|---|---|---|---|
| 1 | FATAL-1 | `ImageProxy.toBitmap()` salah → null bitmap | Tidak ada inference sama sekali |
| 2 | FATAL-2 | Polygon hanya rectangle, mask tidak di-decode | Fitur inti tidak jalan |
| 3 | FATAL-3 | Nama model FP16 vs INT8 tidak konsisten | Crash atau silent wrong inference |
| 4 | FATAL-4 | Camera flip tidak bekerja | Tombol dead |
| 5 | KRITIS-1 | Save tidak benar-benar menyimpan | Data hilang, history selalu kosong |
| 6 | KRITIS-2 | `resultBitmap` selalu null di result sheet | Preview kosong |
| 7 | KRITIS-3 | Crop offset letterbox diabaikan | Klasifikasi pada area salah |
| 8 | KRITIS-4 | Mat `lChannel` leak per frame | Crash OOM dalam beberapa menit |
| 9 | KRITIS-5 | Live inference jalan di background | Baterai + resource boros |
| 10 | KRITIS-6 | Tidak ada akses ke settings dari camera | UX buntu |
| 11 | BUG-1 | Output shape diasumsikan [1,300,38] | Potensial crash saat inference |
| 12 | BUG-4 | Camera executor leak saat rekomposisi | Thread leak |
| 13 | MISSING-1 | MediaStore write tidak ada | Simpan tidak berfungsi |
| 14 | MISSING-2 | SaveExaminationUseCase tidak dipanggil | History tidak pernah terisi |
| 15 | MISSING-3 | Mask coefficients tidak di-decode | Hanya ada di FATAL-2 |
| 16 | MISSING-4 | resultBitmap tidak di-generate | Hanya ada di KRITIS-2 |
| 17 | MISSING-5 | Tombol settings belum ada | Setting tidak accessible |
| 18 | MISSING-6 | Live inference tidak berhenti di background | Hanya ada di KRITIS-5 |
| 19 | MISSING-7 | Thumbnail history belum ada | History kurang informatif |
| 20 | MISSING-8 | Share history belum ada | Fitur tidak tersedia |

---

## ✅ YANG SUDAH BENAR DAN BERFUNGSI BAIK

- ✅ Preprocessing pipeline lengkap dan benar: WB → Gamma → Letterbox → Bilateral → CLAHE
- ✅ Color space conversion RGBA→BGR di semua preprocessor sudah benar
- ✅ `AnemiaClassifier` sudah benar: input size dinamis, normalisasi float32/255, expose allScores
- ✅ Mutex thread-safety di `InferenceRepositoryImpl` untuk kedua interpreter
- ✅ `PolygonUtils` — Shoelace area, argmax, Douglas-Peucker sudah diimplementasikan dengan benar
- ✅ `OverlayCanvas` — warna biru/merah/hijau + fill alpha + stroke + vertex dots sudah benar
- ✅ `CaptureResultSheet` — dual score bar, medical disclaimer sudah ada
- ✅ `LiveInferenceWarningDialog` sudah ada dan muncul sebelum mode aktif
- ✅ `OnboardingScreen` — 3 halaman, skip button, DataStore persistence sudah ada
- ✅ `HistoryScreen` — filter, sort, swipe-delete sudah ada di ViewModel
- ✅ Room database schema dan DAO sudah lengkap
- ✅ Hilt DI setup sudah benar
- ✅ String resources trilingual (ID, EN, TH) sudah ada
- ✅ Dark mode via `values-night/themes.xml` sudah ada
- ✅ Permission handling dengan deep-link ke settings sudah ada
- ✅ `imageProxy.close()` selalu dipanggil (baris 313)
- ✅ `liveInferenceJob?.cancel()` di `onCleared()` sudah ada
- ✅ Frame buffer copy (`frameCopy`) sebelum coroutine launch sudah benar