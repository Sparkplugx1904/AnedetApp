# 🔍 AUDIT REPORT v2 — AnedetApp
> Analisis ulang setelah commit `b916cda` — "Kiro #3 With Claude Audit"
> Membandingkan status sebelum dan sesudah perbaikan.

---

## ✅ YANG BERHASIL DIPERBAIKI (dari audit sebelumnya)

| ID Lama | Deskripsi | Status |
|---|---|---|
| FATAL-1 | `ImageProxy.toBitmap()` → sekarang pakai `copyPixelsFromBuffer()` ✔ | **FIXED** |
| FATAL-4 | Camera flip → `key(cameraSelector)` membungkus `CameraPreview` ✔ | **FIXED** |
| KRITIS-1 | Save ke MediaStore → `MediaStoreUtils.kt` baru, `saveExamination()` di ViewModel ✔ | **FIXED** |
| KRITIS-2 | `resultBitmap` selalu null → sekarang di-generate via `generateMaskedBitmap()` ✔ | **FIXED** |
| KRITIS-3 | Letterbox offset diabaikan → `yOffset = 49px` sekarang ditambahkan ✔ | **FIXED** |
| KRITIS-4 | `lChannel` Mat leak → `lChannelOriginal.release()` ditambahkan ✔ | **FIXED** |
| KRITIS-5 | Live inference jalan di background → `ON_STOP` lifecycle handler ditambahkan ✔ | **FIXED** |
| KRITIS-6 | Tidak ada tombol settings → tombol `Settings` di `BottomActionBar` ditambahkan ✔ | **FIXED** |
| FATAL-2 (parsial) | Rectangle polygon → framework decode mask coefficients ditambahkan ✔ | **PARSIAL** |

---

## 🔴 BUG FATAL BARU (Diintroduksi di Commit Ini)

---

### 🔴 [FATAL-NEW-1] `decodeMaskToPolygon` — Format [1,H,W,32] Ditangani Salah → Mask Sampah

**File:** `ConjunctivaSegmentor.kt`

**Masalah dua bagian:**

**Bagian A — Alokasi buffer sudah benar, tapi decode-nya salah:**
```kotlin
// Alokasi: model output [1, H, W, 32] dialokasikan sebagai:
Array(1) { Array(protoShape[1]) { Array(protoShape[2]) { FloatArray(32) } } }
// Bentuk: [1][H][W][32] ← BENAR untuk alokasi

// Tapi di decodeMaskToPolygon, HANYA ada satu branch:
is Array<*> -> {
    val arr = protoMasks as Array<Array<Array<FloatArray>>>
    val h = arr[0][0].size      // ← ini membaca DIM KE-2 sebagai H
    val w = arr[0][0][0].size   // ← ini membaca DIM KE-3 sebagai W
    // Loop matrix multiplication:
    protoData[c][y][x]  // ← arr[0][c][y][x]
                        //   jika format [1][H][W][32]: arr[0][c] = channel c dalam H-array
                        //   artinya c dipakai sebagai index HEIGHT, bukan CHANNEL
}
```

**Dampak:** Karena type erasure di Kotlin/JVM, `as Array<Array<Array<FloatArray>>>` tidak crash saat runtime untuk kedua format. Untuk format [1,H,W,32], indeks `arr[0][c][y][x]` membaca:
- `c` (seharusnya channel 0–31) sebagai index ke dimensi H → jika model punya H=160 dan c max 31, tidak crash
- Tapi datanya sepenuhnya salah — mengambil data dari baris yang berbeda bukan channel yang berbeda

Hasil: sigmoid mask yang dihasilkan adalah noise random, polygon yang di-decode tidak merepresentasikan konjungtiva sama sekali.

**Fix yang diperlukan:**
```kotlin
// Deteksi format saat alokasi dan simpan flag-nya
// Di decodeMaskToPolygon gunakan flag tersebut untuk memilih cara indexing:
// Format [1, 32, H, W]: protoData[c][y][x] = arr[0][c][y][x]  ← sekarang
// Format [1, H, W, 32]: protoData[c][y][x] = arr[0][y][x][c]  ← PERLU ditambahkan
```

---

### 🔴 [FATAL-NEW-2] `extractContourFromMask` — Menghasilkan Set Pixel Acak, Bukan Polygon

**File:** `ConjunctivaSegmentor.kt` baris ±300–340

```kotlin
private fun extractContourFromMask(...): List<PointF> {
    val contour = mutableListOf<PointF>()
    // Scan raster (kiri→kanan, atas→bawah):
    for (y in y1..y2) {
        for (x in x1..x2) {
            if (mask[y][x] && isEdge) {
                contour.add(PointF(x.toFloat(), y.toFloat()))
            }
        }
    }
    return contour  // urutan: baris per baris, kiri→kanan
}
```

**Masalah inti:** Hasil adalah daftar pixel tepi yang diurutkan secara **raster** (baris per baris), bukan kontur yang terhubung. Ketika `PolygonUtils.getAdaptivePolygon()` (Douglas-Peucker) menerima titik-titik ini:

- Point pertama: pixel tepi paling **kiri-atas**
- Point terakhir: pixel tepi paling **kanan-bawah**
- Douglas-Peucker akan mencari titik terjauh dari garis lurus antara kedua ujung ini
- Hasilnya adalah **diagonal jagged line** dari pojok atas-kiri ke pojok bawah-kanan, bukan bentuk konjungtiva

Polygon dengan 6–15 titik yang dihasilkan bukan merupakan outline konjungtiva, melainkan sekumpulan titik di sepanjang diagonal bounding box.

**Solusi yang benar:** Gunakan algoritma Moore Neighborhood Tracing untuk mendapatkan kontur yang terurut dan terhubung:
```kotlin
// Atau gunakan OpenCV via JNI:
val contours = mutableListOf<MatOfPoint>()
val hierarchy = Mat()
Imgproc.findContours(binaryMaskMat, contours, hierarchy,
    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
// Ambil kontur dengan area terbesar, konversi ke List<PointF>
```

---

## 🟠 BUG KRITIS BARU (Diintroduksi di Commit Ini)

---

### 🟠 [KRITIS-NEW-1] `predictedLabel` Case Mismatch → History Filter Selalu Kosong

**File:** `CameraViewModel.kt` baris 428 vs `HistoryViewModel.kt` baris 36–37

```kotlin
// CameraViewModel.kt — saat SAVE:
predictedLabel = if (classification.isAnemic) "ANEMIA" else "NON_ANEMIA"
//                                              ↑ UPPERCASE

// HistoryViewModel.kt — saat FILTER:
FilterType.ANEMIA -> exams.filter { it.predictedLabel == "Anemia" }
//                                                        ↑ Title case
FilterType.NON_ANEMIA -> exams.filter { it.predictedLabel == "Non-Anemia" }
//                                                            ↑ Title case

// AnemiaClassifier.kt — CLASS_NAMES:
mapOf(0 to "Anemia", 1 to "Non-Anemia")
//          ↑ Title case
```

**Dampak:** Setiap rekaman tersimpan dengan label `"ANEMIA"` atau `"NON_ANEMIA"`. Filter `"Anemia"` dan `"Non-Anemia"` tidak akan pernah cocok. Tab filter di HistoryScreen selalu menampilkan list kosong meskipun ada data di database.

**Fix:** Pilih satu format dan konsisten. Rekomendasi: gunakan `classification.label` langsung (sudah title case dari `CLASS_NAMES`):
```kotlin
predictedLabel = classification.label  // "Anemia" atau "Non-Anemia"
```

---

### 🟠 [KRITIS-NEW-2] `runFullPipeline` (Live Inference) Tidak Update `_resultBitmap`

**File:** `CameraViewModel.kt` — fungsi `runFullPipeline`

```kotlin
private suspend fun runFullPipeline(frame: Bitmap) {
    // ...
    val classification = inferenceRepository.classify(crop)
    _inferenceState.value = InferenceState.Success(detection, classification)
    // ← TIDAK ada: generateMaskedBitmap() dan _resultBitmap.value = ...
    preprocessed.recycle()
    crop.recycle()
}
```

Dibandingkan dengan `captureAndClassify()` yang sudah benar:
```kotlin
val maskedBitmap = generateMaskedBitmap(frame, detection, classification)
_resultBitmap.value = maskedBitmap  // ← ada di captureAndClassify, tidak di runFullPipeline
```

**Dampak:** `_resultBitmap` tetap dari sesi capture terakhir atau null saat mode live inference. Jika user beralih dari live inference ke single capture, result sheet akan menampilkan gambar dari sesi sebelumnya, bukan frame saat ini.

---

## 🟡 BUG YANG MASIH TERSISA (Dari Audit Sebelumnya, Belum Diperbaiki)

---

### 🟡 [BUG-1-LAMA] Output Shape [1,300,38] Diasumsikan Tanpa Runtime Guard

**File:** `ConjunctivaSegmentor.kt` baris 82

```kotlin
val output0 = Array(1) { Array(300) { FloatArray(38) } }
```

Kode sudah menambahkan pengecekan proto masks, tapi buffer `output0` untuk deteksi masih di-hardcode `[1,300,38]`. Jika model aktual menghasilkan shape berbeda (misalnya `[1,8400,38]` untuk model tanpa NMS), inference akan crash dengan `IllegalArgumentException`.

**Saran:** Baca shape output tensor 0 dari `interpreter.getOutputTensor(0).shape()` dan alokasikan buffer secara dinamis, atau tambahkan validasi:
```kotlin
val outputShape = interpreter.getOutputTensor(0).shape()
require(outputShape[1] == 300 && outputShape[2] == 38) {
    "Unexpected output shape: ${outputShape.contentToString()}"
}
```

---

### 🟡 [BUG-5-LAMA] `StatusChip` Hanya Tampilkan Satu Score

**File:** `CameraScreen.kt` — `StatusChip`

```kotlin
Text(
    text = "${classificationResult.label} ${(classificationResult.confidence * 100).toInt()}%",
)
```

Spesifikasi: chip harus menampilkan **kedua score** — `"🔴 Anemia 87% | 🟢 Non-Anemia 13%"`. Sekarang hanya menampilkan label pemenang.

---

### 🟡 [MINOR] Error Screen Masih Menyebutkan `best_int8.tflite`, File Sebenarnya `best_float16.tflite`

**File:** `CameraScreen.kt` baris 576–577

```kotlin
"• app/src/main/assets/models/segments/best_int8.tflite\n" +
```

File aktual di assets adalah `best_float16.tflite`. Teks error screen menyesatkan.

---

## 🟢 FITUR YANG MASIH BELUM DIIMPLEMENTASIKAN

---

### 🟢 [MISSING-7] Thumbnail Gambar di History Screen

Tidak ada `AsyncImage` atau Coil di `HistoryScreen.kt`. `imagePath` dari `ExaminationEntity` tersimpan sebagai URI string tapi tidak pernah diload untuk ditampilkan. Setiap item history hanya menampilkan warna solid, tanpa preview gambar.

---

### 🟢 [MISSING-8] Fitur Share di History Screen

Tidak ada implementasi `Intent(Intent.ACTION_SEND)` di `HistoryViewModel` maupun `HistoryScreen`. Tombol share ada di spec tapi tidak diimplementasikan.

---

## 📊 STATUS LENGKAP SEMUA ISSUE

| ID | Deskripsi | Status Sebelum | Status Sekarang |
|---|---|---|---|
| FATAL-1 | `ImageProxy.toBitmap()` salah | 🔴 Fatal | ✅ Fixed |
| FATAL-2 | Polygon hanya rectangle | 🔴 Fatal | 🟡 Parsial (framework ada, decode salah) |
| FATAL-3 | Nama model tidak konsisten | 🔴 Fatal | 🟡 Minor (kode load file benar, error text salah) |
| FATAL-4 | Camera flip tidak bekerja | 🔴 Fatal | ✅ Fixed |
| FATAL-NEW-1 | Proto mask [1,H,W,32] di-decode salah | — | 🔴 **BARU** |
| FATAL-NEW-2 | Contour extraction raster order, bukan polygon | — | 🔴 **BARU** |
| KRITIS-1 | Save tidak menyimpan apapun | 🟠 Kritis | ✅ Fixed |
| KRITIS-2 | `resultBitmap` selalu null | 🟠 Kritis | ✅ Fixed |
| KRITIS-3 | Crop offset letterbox diabaikan | 🟠 Kritis | ✅ Fixed |
| KRITIS-4 | `lChannel` Mat leak | 🟠 Kritis | ✅ Fixed |
| KRITIS-5 | Live inference jalan di background | 🟠 Kritis | ✅ Fixed |
| KRITIS-6 | Tidak ada tombol Settings | 🟠 Kritis | ✅ Fixed |
| KRITIS-NEW-1 | `predictedLabel` case mismatch → filter kosong | — | 🟠 **BARU** |
| KRITIS-NEW-2 | `runFullPipeline` tidak update `_resultBitmap` | — | 🟠 **BARU** |
| BUG-1 | Output shape [1,300,38] tidak divalidasi | 🟡 Bug | 🟡 Masih ada |
| BUG-5 | StatusChip hanya satu score | 🟡 Bug | 🟡 Masih ada |
| MISSING-7 | Thumbnail history | 🟢 Missing | 🟢 Masih missing |
| MISSING-8 | Fitur Share | 🟢 Missing | 🟢 Masih missing |

---

## 🎯 PRIORITAS PERBAIKAN BERIKUTNYA

**Urutan wajib (harus selesai sebelum testing):**

1. **[FATAL-NEW-1]** — Tambahkan branch `[1,H,W,32]` di `decodeMaskToPolygon` dengan indexing `arr[0][y][x][c]`
2. **[FATAL-NEW-2]** — Ganti `extractContourFromMask` dengan OpenCV `Imgproc.findContours()` agar polygon terurut dan terhubung
3. **[KRITIS-NEW-1]** — Ganti `predictedLabel = if(...) "ANEMIA" else "NON_ANEMIA"` dengan `predictedLabel = classification.label`

**Urutan perbaikan berikutnya:**

4. **[BUG-1]** — Baca output shape dinamis dari `interpreter.getOutputTensor(0).shape()`
5. **[KRITIS-NEW-2]** — Tambahkan `generateMaskedBitmap()` + `_resultBitmap.value = ...` di `runFullPipeline()`
6. **[BUG-5]** — Update `StatusChip` untuk tampilkan kedua score
7. **[MISSING-7]** — Tambahkan Coil `AsyncImage` di `HistoryScreen` untuk load thumbnail dari `imagePath`
8. **[MISSING-8]** — Implementasikan share via `Intent.ACTION_SEND` di `HistoryViewModel`
9. **[MINOR]** — Fix teks error screen dari `best_int8.tflite` ke `best_float16.tflite`