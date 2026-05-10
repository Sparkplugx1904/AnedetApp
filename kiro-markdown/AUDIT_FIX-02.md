# 🚨 ROOT CAUSE ANALYSIS — Inference Tidak Jalan Sama Sekali
> Analisis mendalam setelah membaca seluruh source code + logcat dari device V2029 Android 12

---

## KESIMPULAN EKSEKUTIF

Ada **4 bug independent** yang menyebabkan inference benar-benar tidak menghasilkan output apapun.
Bug #1 sendirian sudah cukup untuk memastikan zero detection. Bug #2 dan #3 menyebabkan
koordinat polygon salah meskipun deteksi berhasil. Bug #4 adalah kerusakan arsitektur pipeline.

---

## 🔴 BUG #1 — ROOT CAUSE UTAMA: `toBitmap()` Tidak Handle `rowStride`

**File:** `CameraScreen.kt` baris 588–600

### Kode Sekarang (SALAH)
```kotlin
private fun ImageProxy.toBitmap(): android.graphics.Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val buffer = planes[0].buffer
    buffer.rewind()
    bitmap.copyPixelsFromBuffer(buffer)  // ← CRASH DIAM-DIAM
    return bitmap
}
```

### Mengapa Ini Mematikan

Pada device Android nyata (termasuk V2029 Android 12), CameraX dengan format `RGBA_8888`
memberikan buffer dengan **row padding** di ujung setiap baris. Nilai `planes[0].rowStride`
hampir selalu **lebih besar** dari `width * 4`.

Contoh konkret untuk resolusi 1280×720:
```
Expected per row : 1280 × 4 bytes = 5120 bytes
Actual rowStride : 5120 bytes (atau 5120 + 64 = 5184, atau 6144, dll. — tergantung GPU)
Buffer total     : rowStride × 720 bytes
```

Ketika `copyPixelsFromBuffer(buffer)` dijalankan:
- Bitmap mengharapkan `1280 × 720 × 4 = 3,686,400` bytes berturut-turut
- Buffer berisi `rowStride × 720` bytes — **lebih banyak** jika ada padding
- Hasilnya: pixel row ke-2 disalin ke posisi row ke-2 + sisa padding dari row ke-1
- Setiap baris BERGESER ke kanan sebesar padding bytes dari baris sebelumnya
- Hasil akhir: gambar **miring/shear** dan **korup total**

Model segmentasi kemudian menerima gambar yang korup → tidak ada deteksi valid di atas 0.35 →
`parseOutput` langsung `break` di iterasi pertama → return `null` → `NoDetection` state →
**tidak ada overlay apapun**.

Inilah kenapa tidak ada masking yang tampil sama sekali.

### Fix Wajib

```kotlin
private fun ImageProxy.toBitmap(): Bitmap {
    val plane = planes[0]
    val rowStride = plane.rowStride      // bytes per row termasuk padding
    val pixelStride = plane.pixelStride  // bytes per pixel (4 untuk RGBA_8888)
    val buffer = plane.buffer

    // Fast path: tidak ada padding, langsung copy
    if (rowStride == width * pixelStride) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    // Slow path: ada padding — copy row by row, lewati padding
    val cleanBuffer = ByteBuffer.allocateDirect(width * height * pixelStride)
    buffer.rewind()
    for (row in 0 until height) {
        // Set posisi ke awal row ini (termasuk offset padding dari row sebelumnya)
        buffer.position(row * rowStride)
        // Copy hanya pixel bytes yang valid (tanpa padding di ujung row)
        val rowData = ByteArray(width * pixelStride)
        buffer.get(rowData)
        cleanBuffer.put(rowData)
    }
    cleanBuffer.rewind()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(cleanBuffer)
    return bitmap
}
```

---

## 🔴 BUG #2 — Koordinat Y Polygon Salah Total (Letterbox Offset Tidak Di-invert)

**File:** `ConjunctivaSegmentor.kt` fungsi `parseOutput`, baris 200–203

### Masalah

`LetterboxResizer` mengubah frame 1280×720 menjadi 224×224 dengan:
- `scale = 224 / 1280 = 0.175`
- `newW = 1280 × 0.175 = 224px` (seluruh lebar terpakai — tidak ada X offset)
- `newH = 720 × 0.175 = 126px`
- `yOff = (224 - 126) / 2 = 49px` ← **padding hitam** 49px atas dan bawah

Bitmap 224×224 yang diterima segmentor:
```
Baris  0–48  : padding hitam (bukan isi gambar asli)
Baris 49–174 : isi gambar asli 1280×720 yang di-scale
Baris 175–223: padding hitam
```

Segmentor kemudian resize ke 320×320. Model output koordinat dinormalisasi [0,1]
**termasuk padding hitam**. Jadi:
- `y_norm = 0.0` → baris 0 dari 320px → padding hitam, bukan bagian atas gambar asli
- `y_norm = 49/224 = 0.219` → baris pertama gambar **asli** dalam 224
- `y_norm = 175/224 = 0.781` → baris terakhir gambar **asli** dalam 224
- `y_norm = 1.0` → baris 224 → padding hitam bawah

### Kode Sekarang (SALAH)
```kotlin
val y1Denorm = y1 * originalHeight  // y1 * 720
val y2Denorm = y2 * originalHeight  // y2 * 720
```

**Contoh nyata:** Konjungtiva di tepat tengah atas bola mata → `y_norm ≈ 0.35`

```
Kode sekarang: 0.35 × 720 = 252px dari atas layar
Seharusnya   : (0.35 × 224 - 49) / 0.175 = (78.4 - 49) / 0.175 = 168px dari atas layar
```

Selisih 84 pixel — polygon muncul di posisi salah, bisa di luar area konjungtiva, atau bahkan di luar batas layar.

### Fix Wajib

Tambahkan inverse letterbox mapping di `parseOutput` (atau berikan `yOff` dan `scale` ke segmentor):

```kotlin
// Letterbox parameters — harus konsisten dengan LetterboxResizer
val letterboxSize = 224f
val letterboxScale = letterboxSize / maxOf(originalWidth, originalHeight)  // 224/1280 = 0.175
val newH = (originalHeight * letterboxScale).toInt()   // 126
val newW = (originalWidth * letterboxScale).toInt()    // 224
val xOff = (letterboxSize - newW) / 2f                // 0.0
val yOff = (letterboxSize - newH) / 2f                // 49.0

// De-normalize dari model output [0,1] ke original frame coordinates
// Dengan inverse letterbox transform:
fun modelToFrame(xNorm: Float, yNorm: Float): PointF {
    // 1. Model norm → 224 space (termasuk padding)
    val x224 = xNorm * letterboxSize
    val y224 = yNorm * letterboxSize
    // 2. 224 space → original frame (inverse letterbox: subtract offset, divide by scale)
    val xOrig = (x224 - xOff) / letterboxScale
    val yOrig = (y224 - yOff) / letterboxScale
    return PointF(xOrig.coerceIn(0f, originalWidth.toFloat()), yOrig.coerceIn(0f, originalHeight.toFloat()))
}

val topLeft  = modelToFrame(x1, y1)
val botRight = modelToFrame(x2, y2)
val x1Denorm = topLeft.x
val y1Denorm = topLeft.y
val x2Denorm = botRight.x
val y2Denorm = botRight.y
```

---

## 🔴 BUG #3 — `decodeMaskToPolygon`: Konversi Balik Koordinat Contour Salah Ganda

**File:** `ConjunctivaSegmentor.kt` baris 350–355

### Kode Sekarang (SALAH)
```kotlin
val scaledContour = contourPoints.map { pt ->
    PointF(
        pt.x / scaleX / INPUT_SIZE * originalWidth,   // ← SALAH
        pt.y / scaleY / INPUT_SIZE * originalHeight    // ← SALAH
    )
}
```

### Mengapa Salah

`scaleX = protoWidth / INPUT_SIZE` (contoh: 160/320 = 0.5).
`pt` adalah titik dalam koordinat proto mask (range 0–160).

Trace matematika:
```
pt.x / scaleX         = pt.x / 0.5 = pt.x * 2
                      → convert dari proto space (0-160) ke INPUT_SIZE space (0-320) ✓
/ INPUT_SIZE           → normalize ke [0,1]
* originalWidth        → scale ke 1280
```

Secara X, ini matematis benar jika tidak ada letterbox X offset (xOff=0 ✓ untuk 1280×720).

Secara Y, ini **salah** karena tidak memperhitungkan `yOff = 49`. Masalah yang sama dengan Bug #2 — koordinat Y dari contour juga displaced.

**Ditambah lagi**: contour berasal dari binary mask dalam `bboxInProto` space yang sudah dioffset, tapi scaling baliknya tidak memperhitungkan bahwa bbox itu sendiri sudah dalam koordinat yang salah (dari Bug #2).

### Fix Wajib

Ganti seluruh `scaledContour` mapping dengan fungsi `modelToFrame` yang sama dari Bug #2.
Contour points dalam proto space → convert ke [0,1] normalized → apply inverse letterbox.

```kotlin
val scaledContour = contourPoints.map { pt ->
    // pt is in proto mask coordinates (e.g., 0-160)
    // Convert to normalized model coordinates [0,1]
    val xNorm = pt.x / protoWidth
    val yNorm = pt.y / protoHeight
    // Apply inverse letterbox to get original frame coordinates
    modelToFrame(xNorm, yNorm)
}
```

---

## 🟠 BUG #4 — Arsitektur Pipeline Rusak: Preprocessing 224px Untuk Segmentasi Bukan 320px

**File:** `InferenceRepositoryImpl.kt` dan `CameraViewModel.kt`

### Masalah

`RunPreprocessingUseCase` melakukan letterbox ke **224×224** — ini dirancang untuk model **klasifikasi** yang dilatih pada 448px (dengan letterbox ke 224 sebagai input). 

Tapi **segmentasi model** dilatih pada 640px dan dieksport ke **320×320**. Ketika pipeline yang sama (letterbox ke 224) dipakai untuk segmentasi:

```
Frame 1280×720
    ↓ LetterboxResizer (targetSize=224) 
224×224 bitmap  ← UNTUK KLASIFIKASI ✓
    ↓ Segmentor.segment(bitmap, originalWidth=1280, originalHeight=720)
    ↓ createScaledBitmap(bitmap, 320, 320)  ← resize 224→320
Model input 320×320 ← double-resized dan letterbox-dalam-letterbox, kualitas turun
```

Pipeline yang benar seharusnya:
```
Frame 1280×720
    ├── Segmentation path: CLAHE only (tanpa letterbox) → Segmentor → resize internal ke 320×320
    └── Classification path: Full preprocessing → Letterbox 224 → Classifier
```

### Dampak Saat Ini

Segmentor menerima gambar yang:
1. Sudah dikompres dari 1280×720 ke 224×224 (kehilangan resolusi)
2. Kemudian di-scale-up ke 320×320 (artifak blur)
3. Koordinat model harus di-inverse melalui dua layer letterbox

Ini menurunkan akurasi deteksi secara signifikan — model mungkin gagal mendeteksi konjungtiva sama sekali karena kualitas gambar input terlalu buruk.

### Fix Wajib

Buat dua preprocessing path terpisah:

```kotlin
// InferenceRepositoryImpl.kt
override suspend fun segment(originalFrame: Bitmap, originalWidth: Int, originalHeight: Int): DetectionResult? {
    // Segmentation: CLAHE only, NO letterbox
    // Segmentor handles its own resize to 320x320 internally
    return withContext(Dispatchers.Default) {
        segmentationMutex.withLock {
            val claheOnly = applySegmentationPreprocessing(originalFrame)  // WB + Gamma + Bilateral + CLAHE
            segmentor.segment(claheOnly, originalWidth, originalHeight)
        }
    }
}

// CameraViewModel.kt — ubah processFrameForSegmentation:
viewModelScope.launch {
    // Pass ORIGINAL frame to segment (not preprocessed 224x224)
    val detection = inferenceRepository.segment(frameCopy, FRAME_WIDTH, FRAME_HEIGHT)
    
    // For classification: preprocess fully (with letterbox to 224)
    // Only if capture is pressed, not for live segmentation
}
```

Atau minimal: ubah `LetterboxResizer` di `RunPreprocessingUseCase` agar menerima `targetSize` sebagai parameter, dan buat `RunSegmentationPreprocessingUseCase` terpisah yang letterbox ke **320** bukan 224.

---

## 🟡 BUG #5 — `captureAndClassify` Menggunakan Preprocessed Cache Yang Dibuat Dari Frame Korup

**File:** `CameraViewModel.kt` baris 157–159

```kotlin
// Saat capture ditekan:
val preprocessed = lastPreprocessedBitmap?.copy(Bitmap.Config.ARGB_8888, false)
    ?: inferenceRepository.preprocess(frame)
```

`lastPreprocessedBitmap` diisi dari `processFrameForSegmentation` — yang menggunakan bitmap dari `toBitmap()` yang korup (Bug #1). Jadi meskipun capture ditekan, `preprocessed` yang digunakan berasal dari **frame korup yang sama**.

Setelah Bug #1 di-fix, ini otomatis ikut benar.

---

## 📋 URUTAN PERBAIKAN WAJIB

### Langkah 1 — Fix `toBitmap()` (handle rowStride)
Ini adalah fix **single most important**. Tanpa ini tidak ada yang bisa jalan.
Implementasi di atas adalah fix yang benar dan production-ready.

### Langkah 2 — Split Preprocessing Path
Buat `RunSegmentationPreprocessingUseCase` yang hanya jalankan:
WB → Gamma → Bilateral → CLAHE (tanpa LetterboxResizer).
Segmentor kemudian resize internal ke 320×320.

Ubah `CameraViewModel.processFrameForSegmentation` untuk:
- Kirim **original frame** ke segmentor (bukan preprocessed 224×224)
- Baru kirim **preprocessed 224×224** ke classifier saat capture

### Langkah 3 — Fix Inverse Letterbox di Koordinat Model

Di `ConjunctivaSegmentor.parseOutput`, tambahkan fungsi `modelToFrame(xNorm, yNorm)`
yang memperhitungkan letterbox offset saat mengkonversi koordinat model ke frame asli.

Setelah Step 2 dilakukan (segmentor terima frame asli), letterbox offset menjadi:
- `scale = min(320/1280, 320/720) = 320/1280 = 0.25`
- `newW = 320, newH = 180`
- `xOff = 0, yOff = (320-180)/2 = 70`

Dan konversi balik:
```kotlin
fun modelToFrame(xNorm: Float, yNorm: Float, inputSize: Int, origW: Int, origH: Int): PointF {
    val scale = inputSize.toFloat() / maxOf(origW, origH)
    val scaledW = (origW * scale).toInt()
    val scaledH = (origH * scale).toInt()
    val xOff = (inputSize - scaledW) / 2f
    val yOff = (inputSize - scaledH) / 2f
    val xOrig = (xNorm * inputSize - xOff) / scale
    val yOrig = (yNorm * inputSize - yOff) / scale
    return PointF(xOrig.coerceIn(0f, origW.toFloat()), yOrig.coerceIn(0f, origH.toFloat()))
}
```

### Langkah 4 — Fix `scaledContour` di `decodeMaskToPolygon`
Gunakan `modelToFrame()` yang sama untuk konversi balik dari proto space ke original frame.

---

## 🔍 CARA VERIFIKASI SETELAH FIX

Tambahkan log ini di CameraViewModel untuk confirm tiap langkah berjalan:

```kotlin
// Di processFrameForSegmentation, setelah toBitmap():
Log.d("FrameDebug", "Frame size: ${bitmap.width}×${bitmap.height}, tidak null: ${!bitmap.isRecycled}")

// Di ConjunctivaSegmentor.segment():
Log.d("SegDebug", "Input bitmap: ${preprocessedBitmap.width}×${preprocessedBitmap.height}")
Log.d("SegDebug", "Inference success, scanning detections...")

// Di parseOutput, di awal loop:
Log.d("SegDebug", "Detection $i: conf=${detection[4]}, x1=${detection[0]}, y1=${detection[1]}")

// Di InferenceRepositoryImpl, setelah segment():
Log.d("RepoDebug", "Segmentation result: $result")
```

Jika setelah fix, log menunjukkan `conf=0.0` untuk semua 300 detections → model tidak mendeteksi apapun (masalah kualitas gambar atau threshold terlalu tinggi, coba turunkan ke 0.15).
Jika log menunjukkan `conf > 0.35` tapi tidak ada overlay → masalah di koordinat scaling (Bug #2/#3).