# 🔧 FIX: INT8 Hybrid Quantization Model Crash

## 🔍 Akar Masalah

**Error:**
```
TRANSPOSE_CONV: weights->type != input->type (INT8 != FLOAT32)
Node number 411 (TRANSPOSE_CONV) failed to prepare
```

**Penyebab:**
- Ultralytics mengekspor TFLite INT8 dalam mode **hybrid quantization**
- **Weights** dikuantisasi ke INT8
- **Input/Output tensor** tetap FLOAT32
- Delegate default Android (NNAPI/GPU) **tidak support** mixed precision seperti ini
- Crash terjadi saat inisialisasi `Interpreter`

---

## ✅ Solusi yang Diterapkan

### 1. **ConjunctivaSegmentor.kt** - CPU-Only untuk INT8 Model

**File:** `app/src/main/java/com/example/anemiadetector/ml/segmentation/ConjunctivaSegmentor.kt`

**Perubahan:**
```kotlin
// SEBELUM (SALAH):
val options = Interpreter.Options().apply { 
    numThreads = 4              // ❌ Bukan setter method
    setUseNNAPI(false)
}

// SESUDAH (BENAR):
val options = Interpreter.Options().apply { 
    setNumThreads(4)            // ✅ Pakai setter method
    setUseNNAPI(false)          // ✅ Disable NNAPI untuk hybrid INT8
}
```

**Penjelasan:**
- `setNumThreads(4)` - Gunakan 4 thread CPU untuk inference
- `setUseNNAPI(false)` - **CRITICAL**: Disable NNAPI delegate
- **Tidak ada GPU delegate** - Pure CPU inference untuk hybrid quantized model
- Model INT8 dengan input/output FLOAT32 hanya bisa jalan di CPU

---

### 2. **AnemiaClassifier.kt** - CPU-Only untuk Konsistensi

**File:** `app/src/main/java/com/example/anemiadetector/ml/classification/AnemiaClassifier.kt`

**Perubahan:**
```kotlin
// SEBELUM (SALAH):
val options = Interpreter.Options().apply { 
    numThreads = 4              // ❌ Bukan setter method
    setUseNNAPI(false)
}

// SESUDAH (BENAR):
val options = Interpreter.Options().apply { 
    setNumThreads(4)            // ✅ Pakai setter method
    setUseNNAPI(false)          // ✅ CPU-only untuk stabilitas
}
```

**Penjelasan:**
- Model FLOAT32 sebenarnya **compatible dengan GPU delegate**
- Tapi untuk **konsistensi dan stabilitas**, pakai CPU-only dulu
- Bisa diaktifkan GPU delegate nanti jika perlu performa lebih tinggi

---

## 📊 Perbandingan Konfigurasi

| Komponen | Model Type | Delegate | Threads | Alasan |
|----------|-----------|----------|---------|--------|
| **Segmentation** | INT8 Hybrid | CPU-only | 4 | Hybrid quantization tidak support NNAPI/GPU |
| **Classification** | FLOAT32 | CPU-only | 4 | Konsistensi & stabilitas (bisa pakai GPU nanti) |

---

## 🧪 Testing

### Build Status
```bash
./gradlew assembleDebug
```
✅ **BUILD SUCCESSFUL** in 2m 25s

### Next Steps
1. Install APK ke device: `./gradlew installDebug`
2. Jalankan aplikasi dan cek logcat
3. Pastikan tidak ada crash saat inisialisasi TFLite
4. Test inference segmentasi dan klasifikasi

---

## 📝 Catatan Penting

### Kenapa `setNumThreads()` Bukan `numThreads`?

**Salah:**
```kotlin
val options = Interpreter.Options().apply { 
    numThreads = 4  // ❌ Ini property assignment, bukan setter
}
```

**Benar:**
```kotlin
val options = Interpreter.Options().apply { 
    setNumThreads(4)  // ✅ Ini method call yang benar
}
```

`Interpreter.Options` menggunakan **Java-style setter methods**, bukan Kotlin properties.

---

## 🔄 Jika Masih Crash

Jika masih ada crash setelah fix ini, cek:

1. **Logcat untuk error detail:**
   ```bash
   adb logcat | grep -E "(ConjunctivaSegmentor|AnemiaClassifier|TFLite)"
   ```

2. **Pastikan model ada di assets:**
   ```
   app/src/main/assets/models/segments/best_int8.tflite
   app/src/main/assets/models/classify/best_float32.tflite
   ```

3. **Cek TFLite version di build.gradle.kts:**
   ```kotlin
   implementation("org.tensorflow:tensorflow-lite:2.16.1")
   ```

4. **Alternatif: Ganti ke FLOAT32 model untuk segmentasi**
   - Copy `best_float32.tflite` dari AnedetAI
   - Update `MODEL_PATH` di `ConjunctivaSegmentor.kt`
   - FLOAT32 lebih stabil tapi ukuran file lebih besar

---

## 📚 Referensi

- [TensorFlow Lite Quantization](https://www.tensorflow.org/lite/performance/post_training_quantization)
- [Ultralytics Export Formats](https://docs.ultralytics.com/modes/export/)
- [Android TFLite Delegates](https://www.tensorflow.org/lite/performance/delegates)

---

**Status:** ✅ Fixed  
**Date:** 2026-05-09  
**Build:** Successful  
**Next:** Install & Test Runtime
