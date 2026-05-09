# 📍 LOKASI MODEL TFLITE - COPY GUIDE

> **Panduan untuk meng-copy model TFLite dari AnedetAI ke AnedetApp**

---

## 🎯 OVERVIEW

Dokumen ini menjelaskan:
1. Lokasi model TFLite di repositori AnedetAI
2. Lokasi tujuan di project Android AnedetApp
3. Cara meng-copy model dengan benar

---

## 📦 1. MODEL SEGMENTASI KONJUNGTIVA

### **Lokasi Source (AnedetAI):**

```
AnedetAI/Conjunctiva Segmentation/Conjunctiva Segmentation Model/conjunctiva_segmentation_model_YOLOv11-seg/best_saved_model/best_full_integer_quant.tflite
```

**Atau alternatif:**

```
AnedetAI/Conjunctiva Segmentation/Conjunctiva Segmentation Model/conjunctiva_segmentation_model_YOLOv26-seg/best_full_integer_quant.tflite
```

### **Spesifikasi Model:**

| Property | Value |
|----------|-------|
| **Nama File** | `best_int8.tflite` (rename dari `best_full_integer_quant.tflite`) |
| **Format** | TFLite INT8 Quantized |
| **Input Size** | 320×320 px |
| **Task** | Instance Segmentation |
| **Arsitektur** | YOLOv26n-seg atau YOLOv11n-seg |
| **Training Size** | 640px (dieksport ke 320px) |
| **NMS** | Embedded dalam model |
| **Confidence Threshold** | 0.35 (FIXED) |
| **Class** | Single class - "conjunctiva" (index 0) |

### **Lokasi Tujuan (AnedetApp):**

```
AnedetApp/app/src/main/assets/models/segments/best_int8.tflite
```

### **Cara Copy:**

**Windows PowerShell:**
```powershell
# Buat folder tujuan
New-Item -ItemType Directory -Path "app\src\main\assets\models\segments" -Force

# Copy dan rename model
Copy-Item "AnedetAI\Conjunctiva Segmentation\Conjunctiva Segmentation Model\conjunctiva_segmentation_model_YOLOv11-seg\best_saved_model\best_full_integer_quant.tflite" -Destination "app\src\main\assets\models\segments\best_int8.tflite"
```

---

## 📦 2. MODEL KLASIFIKASI ANEMIA

### **Lokasi Source (AnedetAI):**

```
AnedetAI/Anemia Classify/Models/yolo26s-CLAHEv2/tflite/best_float32.tflite
```

**Sesuai dengan `live_inference.py` baris 14:**
```python
MODEL_CLS_PATH = r"Anemia Classify\Models\yolo26s-CLAHEv2\tflite\best_float32.tflite"
```

### **Spesifikasi Model:**

| Property | Value |
|----------|-------|
| **Nama File** | `best_float32.tflite` |
| **Format** | TFLite FLOAT32 |
| **Input Size** | DINAMIS (baca dari model saat runtime) |
| **Task** | Binary Classification |
| **Arsitektur** | YOLOv26m-cls |
| **Training Size** | 448px dengan CLAHE-augmented dataset |
| **Classes** | 2 classes: "Anemia" (index 0), "Non-Anemia" (index 1) |
| **Output** | Array 2 elemen `[score_Anemia, score_NonAnemia]` |
| **Normalisasi** | pixel / 255.0 → float32 |

### **Lokasi Tujuan (AnedetApp):**

```
AnedetApp/app/src/main/assets/models/classify/best_float32.tflite
```

### **Cara Copy:**

**Windows PowerShell:**
```powershell
# Buat folder tujuan
New-Item -ItemType Directory -Path "app\src\main\assets\models\classify" -Force

# Copy model (tidak perlu rename)
Copy-Item "AnedetAI\Anemia Classify\Models\yolo26s-CLAHEv2\tflite\best_float32.tflite" -Destination "app\src\main\assets\models\classify\best_float32.tflite"
```

---

## 📋 3. STRUKTUR FOLDER ASSETS LENGKAP

Setelah copy, struktur folder `assets` harus seperti ini:

```
app/src/main/assets/
└── models/
    ├── segments/
    │   └── best_int8.tflite          ← Model segmentasi (INT8)
    └── classify/
        └── best_float32.tflite       ← Model klasifikasi (FLOAT32)
```

---

## 🔍 4. VERIFIKASI MODEL

### **Cek Ukuran File:**

**Model Segmentasi:**
- Ukuran: ~2-5 MB (INT8 quantized)
- Jika lebih besar (>10 MB), mungkin bukan INT8

**Model Klasifikasi:**
- Ukuran: ~8-15 MB (FLOAT32)
- Jika lebih kecil (<5 MB), mungkin bukan FLOAT32

### **Cek di Android Studio:**

1. Buka Android Studio
2. Klik kanan pada folder `assets` → Refresh
3. Pastikan kedua file muncul di:
   - `app/src/main/assets/models/segments/best_int8.tflite`
   - `app/src/main/assets/models/classify/best_float32.tflite`

### **Test Loading Model:**

```kotlin
// Test di Application class atau MainActivity
class AnemiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Test load segmentation model
        try {
            val segModelBuffer = loadModelBuffer(this, "models/segments/best_int8.tflite")
            Log.d("ModelTest", "Segmentation model loaded: ${segModelBuffer.capacity()} bytes")
        } catch (e: Exception) {
            Log.e("ModelTest", "Failed to load segmentation model", e)
        }
        
        // Test load classification model
        try {
            val clsModelBuffer = loadModelBuffer(this, "models/classify/best_float32.tflite")
            Log.d("ModelTest", "Classification model loaded: ${clsModelBuffer.capacity()} bytes")
        } catch (e: Exception) {
            Log.e("ModelTest", "Failed to load classification model", e)
        }
    }
    
    private fun loadModelBuffer(context: Context, modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
```

---

## ⚠️ 5. TROUBLESHOOTING

### **Error: FileNotFoundException**

**Penyebab:**
- Model tidak ada di folder `assets`
- Path salah (typo)
- Folder `assets` belum di-sync

**Solusi:**
1. Pastikan model sudah di-copy ke folder yang benar
2. Refresh project di Android Studio (File → Sync Project with Gradle Files)
3. Clean dan rebuild project (Build → Clean Project → Rebuild Project)

### **Error: Model loading failed**

**Penyebab:**
- Model corrupt saat copy
- Format model salah (bukan TFLite)

**Solusi:**
1. Copy ulang model dari source
2. Verifikasi ukuran file sama dengan source
3. Cek ekstensi file `.tflite` (bukan `.pt` atau `.onnx`)

### **Error: Input tensor shape mismatch**

**Penyebab:**
- Model yang di-copy bukan model yang benar
- Model versi lama

**Solusi:**
1. Pastikan copy dari path yang benar sesuai `live_inference.py`
2. Cek tanggal modifikasi file (gunakan model terbaru)

---

## 📊 6. INFORMASI TAMBAHAN

### **Model Segmentasi - Detail:**

**Path di Python:**
```python
MODEL_SEG_PATH = r"Conjunctiva Segmentation\Models\best_int8.tflite"
```

**Catatan:**
- Di repositori AnedetAI, model segmentasi ada di beberapa folder
- Pilih yang **INT8** (full integer quantized)
- Jangan gunakan FLOAT16 atau FLOAT32 untuk segmentasi

### **Model Klasifikasi - Detail:**

**Path di Python:**
```python
MODEL_CLS_PATH = r"Anemia Classify\Models\yolo26s-CLAHEv2\tflite\best_float32.tflite"
```

**Catatan:**
- Model ini dilatih dengan CLAHE-augmented dataset
- WAJIB gunakan FLOAT32 (bukan INT8) untuk akurasi lebih tinggi
- Input size dinamis (baca dari model)

---

## ✅ CHECKLIST COPY MODEL

- [ ] Folder `app/src/main/assets/models/segments/` sudah dibuat
- [ ] Folder `app/src/main/assets/models/classify/` sudah dibuat
- [ ] Model segmentasi `best_int8.tflite` sudah di-copy
- [ ] Model klasifikasi `best_float32.tflite` sudah di-copy
- [ ] Ukuran file segmentasi: ~2-5 MB
- [ ] Ukuran file klasifikasi: ~8-15 MB
- [ ] Refresh project di Android Studio
- [ ] Test loading model berhasil (tidak ada exception)
- [ ] Logcat menampilkan ukuran model dalam bytes

---

## 🚀 LANGKAH SELANJUTNYA

Setelah model berhasil di-copy:

1. ✅ Baca dokumen `01-ANALISIS-LIVE-INFERENCE.md`
2. ✅ Implementasi kode dari `02-KODE-CAMERA-PROCESSING-ANDROID.md`
3. ✅ Setup dependencies dari `03-LIBRARY-DEPENDENCIES.md`
4. ✅ Implementasi preprocessing dari `04-PARAMETER-PREPROCESSING-DETAIL.md`
5. ✅ Validasi dengan `05-RINGKASAN-DAN-CHECKLIST.md`

---

**📌 CATATAN PENTING:**

Model TFLite adalah **CORE** dari aplikasi ini.

Tanpa model yang benar, aplikasi tidak akan berfungsi.

Pastikan copy dari path yang **PERSIS SAMA** dengan `live_inference.py`!

**Selamat meng-copy model! 📦**
