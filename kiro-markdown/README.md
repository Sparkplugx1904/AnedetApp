# 📚 DOKUMENTASI IMPLEMENTASI ANDROID - ANEDET APP

> **Panduan lengkap implementasi aplikasi Android untuk deteksi anemia**  
> Berdasarkan `AnedetAI/live_inference.py` dan `AnedetAI/CLAUDE.md`

---

## 🎯 TENTANG DOKUMENTASI INI

Dokumentasi ini dibuat untuk membantu Anda mengimplementasikan aplikasi Android **AnedetApp** yang **IDENTIK** dengan sistem deteksi anemia Python yang sudah berjalan di `AnedetAI/live_inference.py`.

**Prinsip Utama:**
- ✅ **Mode A (Referensi live_inference.py)** - AKTIF
- ❌ **Mode B (Alternatif)** - TIDAK DIGUNAKAN
- 🚫 **Jangan berasumsi** atau menggunakan metode di luar logika `live_inference.py`

---

## 📖 DAFTAR ISI

### **00. Lokasi Model TFLite**
📄 [`00-LOKASI-MODEL-TFLITE.md`](./00-LOKASI-MODEL-TFLITE.md)

**Isi:**
- Lokasi model segmentasi di AnedetAI
- Lokasi model klasifikasi di AnedetAI
- Cara copy model ke project Android
- Verifikasi model berhasil di-copy
- Troubleshooting model loading

**Baca ini PERTAMA** sebelum mulai coding!

---

### **01. Analisis Live Inference Python**
📄 [`01-ANALISIS-LIVE-INFERENCE.md`](./01-ANALISIS-LIVE-INFERENCE.md)

**Isi:**
- Analisis mendalam `live_inference.py`
- Konfigurasi model (path, class names)
- Fungsi `apply_clahe()` - preprocessing CLAHE
- Class `AnemiaClassifier` - klasifikasi TFLite
- Fungsi `start_live_inference()` - main loop
- Mapping Python → Android untuk setiap bagian
- Parameter kritis yang WAJIB identik

**Dokumen ini adalah REFERENSI UTAMA!**

---

### **02. Kode Camera Processing Android**
📄 [`02-KODE-CAMERA-PROCESSING-ANDROID.md`](./02-KODE-CAMERA-PROCESSING-ANDROID.md)

**Isi:**
- `CameraViewModel.kt` - State management dan inference logic
- `CameraScreen.kt` - Jetpack Compose UI dengan CameraX
- `OverlayCanvas.kt` - Polygon overlay dengan alpha fill
- `CameraUtils.kt` - Helper untuk konversi ImageProxy → Bitmap
- `InferenceState.kt` - Sealed class untuk state management

**Kode lengkap siap pakai!**

---

### **03. Library & Dependencies**
📄 [`03-LIBRARY-DEPENDENCIES.md`](./03-LIBRARY-DEPENDENCIES.md)

**Isi:**
- `build.gradle.kts` (Project level)
- `build.gradle.kts` (App level) - LENGKAP dengan semua dependencies
- `gradle/libs.versions.toml` - Version catalog
- Penjelasan setiap library dan fungsinya
- ProGuard rules untuk TFLite, OpenCV, Hilt, Room
- Estimasi ukuran APK

**Copy-paste langsung ke project!**

---

### **04. Parameter Preprocessing Detail**
📄 [`04-PARAMETER-PREPROCESSING-DETAIL.md`](./04-PARAMETER-PREPROCESSING-DETAIL.md)

**Isi:**
- Parameter CLAHE dari `live_inference.py` (clipLimit=2.0, tileGrid=8×8)
- Parameter preprocessing lengkap dari `CLAUDE.md` (Mode B)
- Color space conversion (RGB vs BGR) - KRITIS!
- Input size dan normalisasi untuk model
- Polygon selection (AREA TERBESAR - bukan confidence!)
- Class label mapping (0=Anemia, 1=Non-Anemia)

**Setiap nilai dijelaskan dengan detail!**

---

### **05. Ringkasan & Checklist**
📄 [`05-RINGKASAN-DAN-CHECKLIST.md`](./05-RINGKASAN-DAN-CHECKLIST.md)

**Isi:**
- Struktur folder project lengkap
- Checklist dependencies
- Checklist preprocessing
- Checklist camera setup
- Checklist model inference
- Checklist visualisasi
- Checklist mode operasi
- Checklist fitur simpan
- Checklist lokalisasi (3 bahasa)
- Checklist UI/UX
- Checklist testing
- Checklist anti-patterns (LARANGAN)
- Performance targets
- Validasi akhir

**Gunakan sebagai panduan step-by-step!**

---

## 🚀 CARA MENGGUNAKAN DOKUMENTASI INI

### **Step 1: Persiapan Model**
1. Baca [`00-LOKASI-MODEL-TFLITE.md`](./00-LOKASI-MODEL-TFLITE.md)
2. Copy model segmentasi (`best_int8.tflite`) ke `app/src/main/assets/models/segments/`
3. Copy model klasifikasi (`best_float32.tflite`) ke `app/src/main/assets/models/classify/`
4. Verifikasi model berhasil di-copy

### **Step 2: Pahami Logika Python**
1. Baca [`01-ANALISIS-LIVE-INFERENCE.md`](./01-ANALISIS-LIVE-INFERENCE.md)
2. Pahami setiap fungsi di `live_inference.py`
3. Pahami mapping Python → Android
4. Catat parameter kritis yang WAJIB identik

### **Step 3: Setup Dependencies**
1. Baca [`03-LIBRARY-DEPENDENCIES.md`](./03-LIBRARY-DEPENDENCIES.md)
2. Copy `build.gradle.kts` (Project level)
3. Copy `build.gradle.kts` (App level)
4. Copy `gradle/libs.versions.toml`
5. Copy `proguard-rules.pro`
6. Sync project dengan Gradle

### **Step 4: Implementasi Kode**
1. Baca [`02-KODE-CAMERA-PROCESSING-ANDROID.md`](./02-KODE-CAMERA-PROCESSING-ANDROID.md)
2. Buat struktur folder sesuai [`05-RINGKASAN-DAN-CHECKLIST.md`](./05-RINGKASAN-DAN-CHECKLIST.md)
3. Implementasi `CameraViewModel.kt`
4. Implementasi `CameraScreen.kt`
5. Implementasi `OverlayCanvas.kt`
6. Implementasi preprocessing (CLAHE)
7. Implementasi segmentasi (`ConjunctivaSegmentor.kt`)
8. Implementasi klasifikasi (`AnemiaClassifier.kt`)

### **Step 5: Implementasi Preprocessing**
1. Baca [`04-PARAMETER-PREPROCESSING-DETAIL.md`](./04-PARAMETER-PREPROCESSING-DETAIL.md)
2. Implementasi `SimpleCLAHEProcessor.kt` (Mode A - dari live_inference.py)
3. Pastikan parameter identik: clipLimit=2.0, tileGrid=(8,8)
4. Pastikan CLAHE hanya pada L channel LAB
5. Pastikan color space conversion benar (RGBA → BGR → LAB → BGR → RGBA)

### **Step 6: Testing & Validasi**
1. Baca [`05-RINGKASAN-DAN-CHECKLIST.md`](./05-RINGKASAN-DAN-CHECKLIST.md)
2. Jalankan aplikasi
3. Test setiap fitur sesuai checklist
4. Validasi performance (FPS, latency, memory)
5. Validasi hasil identik dengan Python

---

## 📋 CHECKLIST CEPAT

Sebelum mulai coding, pastikan:

- [ ] ✅ Sudah membaca `live_inference.py` dan `CLAUDE.md`
- [ ] ✅ Sudah copy model TFLite ke folder assets
- [ ] ✅ Sudah setup dependencies di `build.gradle.kts`
- [ ] ✅ Sudah memahami parameter kritis (CLAHE, confidence, dll)
- [ ] ✅ Sudah memahami color space conversion (RGB vs BGR)
- [ ] ✅ Sudah memahami polygon selection (AREA TERBESAR)

---

## 🎯 PARAMETER KRITIS (QUICK REFERENCE)

| Parameter | Nilai | Source |
|-----------|-------|--------|
| **Camera Resolution** | 1280×720 | live_inference.py |
| **CLAHE clipLimit** | 2.0 | live_inference.py |
| **CLAHE tileGrid** | (8, 8) | live_inference.py |
| **CLAHE channel** | L only (LAB) | live_inference.py |
| **Segmentation conf** | 0.35 | live_inference.py |
| **Polygon selection** | argmax(area) | live_inference.py |
| **Classification input** | DINAMIS | live_inference.py |
| **Classification norm** | /255.0 | live_inference.py |
| **Class mapping** | 0=Anemia, 1=Non-Anemia | live_inference.py |

---

## 🎨 WARNA OVERLAY (QUICK REFERENCE)

| Status | Warna | Hex Code | Alpha Fill |
|--------|-------|----------|------------|
| **Segmentasi saja** | Biru | `#007AFF` | 0.25 |
| **Anemia** | Merah | `#FF3B30` | 0.30 |
| **Non-Anemia** | Hijau | `#34C759` | 0.30 |

---

## 🔧 LIBRARY UTAMA (QUICK REFERENCE)

| Library | Versi | Fungsi |
|---------|-------|--------|
| **CameraX** | 1.3.4 | Capture frame kamera |
| **TensorFlow Lite** | 2.16.1 | Inference model (CPU only) |
| **OpenCV Android** | 4.9.0 | Preprocessing (CLAHE, bilateral, masking) |
| **Jetpack Compose** | BOM 2024.06.00 | UI framework |
| **Hilt** | 2.51.1 | Dependency injection |
| **Room** | 2.6.1 | Database history |
| **Coroutines** | 1.8.1 | Async processing |

---

## ⚠️ LARANGAN KERAS (QUICK REFERENCE)

1. ❌ **JANGAN** gunakan GPU Delegate atau NNAPI
2. ❌ **JANGAN** hardcode input size model
3. ❌ **JANGAN** pilih polygon berdasarkan confidence (WAJIB area terbesar!)
4. ❌ **JANGAN** salah konversi color space (RGB vs BGR)
5. ❌ **JANGAN** tampilkan hanya satu class score (WAJIB keduanya)
6. ❌ **JANGAN** jalankan inference di Main Thread
7. ❌ **JANGAN** abaikan `imageProxy.close()`
8. ❌ **JANGAN** lewati dialog peringatan live inference

---

## 📊 PERFORMANCE TARGETS (QUICK REFERENCE)

| Metrik | Target | Minimum |
|--------|--------|---------|
| **Camera FPS** | ≥ 30 FPS | ≥ 25 FPS |
| **Segmentasi** | < 100ms | < 200ms |
| **Klasifikasi** | < 150ms | < 300ms |
| **Full pipeline** | < 400ms | < 700ms |
| **Memory** | < 200MB | < 350MB |
| **APK size** | < 40MB | < 60MB |

---

## 🌍 LOKALISASI (QUICK REFERENCE)

| Bahasa | Locale | Status |
|--------|--------|--------|
| **Bahasa Indonesia** | `in` | Default |
| **English** | `en` | Supported |
| **ภาษาไทย (Thai)** | `th` | Supported |

---

## 📞 BANTUAN & SUPPORT

Jika ada yang tidak jelas atau ragu:

1. **Baca ulang** dokumen yang relevan
2. **Cek** `live_inference.py` untuk referensi
3. **Cek** `CLAUDE.md` untuk spesifikasi lengkap
4. **TANYAKAN** - jangan asumsikan!

---

## 📝 CATATAN PENTING

### **Mode A vs Mode B**

**Mode A (live_inference.py):**
- ✅ **AKTIF** - Gunakan ini!
- Preprocessing: Hanya CLAHE (clipLimit=2.0, tileGrid=8×8)
- Sederhana dan sudah terbukti berjalan di Python

**Mode B (CLAUDE.md full pipeline):**
- ❌ **TIDAK DIGUNAKAN**
- Preprocessing: Gray World WB → Gamma → Letterbox → Bilateral → Adaptive CLAHE
- Lebih kompleks, gunakan hanya jika diminta

**REKOMENDASI:** Ikuti **Mode A** (live_inference.py) untuk implementasi awal!

---

## ✅ VALIDASI AKHIR

Sebelum menyatakan implementasi selesai, pastikan:

- [ ] ✅ Semua parameter identik dengan `live_inference.py`
- [ ] ✅ Semua library terinstall dengan benar
- [ ] ✅ Model TFLite berhasil di-load
- [ ] ✅ Preview kamera ≥30 FPS
- [ ] ✅ Segmentasi real-time smooth
- [ ] ✅ Klasifikasi < 500ms
- [ ] ✅ Overlay polygon tampil dengan benar
- [ ] ✅ Warna sesuai hasil (Merah/Hijau/Biru)
- [ ] ✅ Bottom sheet tampil dengan benar
- [ ] ✅ Dialog warning tampil dengan benar
- [ ] ✅ Fitur simpan berfungsi
- [ ] ✅ History berfungsi
- [ ] ✅ Lokalisasi 3 bahasa berfungsi
- [ ] ✅ Tidak ada memory leak
- [ ] ✅ Tidak crash dalam kondisi apapun

---

## 🎉 SELESAI!

Jika semua checklist di atas sudah ✅, maka implementasi Android Anda **IDENTIK** dengan `live_inference.py`!

**Selamat mengimplementasikan! 🚀**

---

**📌 STRUKTUR DOKUMENTASI:**

```
kiro-markdown/
├── README.md                              ← Anda di sini
├── 00-LOKASI-MODEL-TFLITE.md             ← Copy model TFLite
├── 01-ANALISIS-LIVE-INFERENCE.md         ← Analisis Python (REFERENSI UTAMA)
├── 02-KODE-CAMERA-PROCESSING-ANDROID.md  ← Kode lengkap CameraX
├── 03-LIBRARY-DEPENDENCIES.md            ← Dependencies lengkap
├── 04-PARAMETER-PREPROCESSING-DETAIL.md  ← Parameter detail
└── 05-RINGKASAN-DAN-CHECKLIST.md         ← Checklist lengkap
```

---

**Dibuat dengan ❤️ berdasarkan:**
- `AnedetAI/live_inference.py` (Referensi Utama)
- `AnedetAI/CLAUDE.md` (Spesifikasi Lengkap)

**Mode A (Referensi live_inference.py) - AKTIF ✅**
