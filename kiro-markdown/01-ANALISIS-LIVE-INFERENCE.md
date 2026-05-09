# 📋 ANALISIS LIVE_INFERENCE.PY - REFERENSI UTAMA

> **Dokumen ini adalah hasil analisis mendalam dari `AnedetAI/live_inference.py`**  
> Semua implementasi Android HARUS mengikuti logika yang ada di file ini.

---

## 🎯 OVERVIEW SISTEM

File `live_inference.py` adalah implementasi Python untuk deteksi anemia real-time menggunakan:
1. **Model Segmentasi Konjungtiva** (TFLite INT8)
2. **Model Klasifikasi Anemia** (TFLite FLOAT32)
3. **Preprocessing CLAHE** untuk meningkatkan kontras

---

## 📦 KONFIGURASI MODEL (Baris 12-15)

```python
MODEL_SEG_PATH = r"Conjunctiva Segmentation\Models\best_int8.tflite"
MODEL_CLS_PATH = r"Anemia Classify\Models\yolo26s-CLAHEv2\tflite\best_float32.tflite"
CAMERA_INDEX   = 0 
CLASS_NAMES    = {0: "Anemic", 1: "Non-Anemic"}
```

### ✅ MAPPING KE ANDROID:

| Python | Android |
|--------|---------|
| `MODEL_SEG_PATH` | `app/src/main/assets/models/segments/best_int8.tflite` |
| `MODEL_CLS_PATH` | `app/src/main/assets/models/classify/best_float32.tflite` |
| `CAMERA_INDEX = 0` | CameraX dengan `CameraSelector.LENS_FACING_BACK` |
| `CLASS_NAMES` | `mapOf(0 to "Anemia", 1 to "Non-Anemia")` |

**⚠️ PENTING:**
- Index 0 = "Anemic" (Anemia)
- Index 1 = "Non-Anemic" (Non-Anemia)
- Jangan terbalik mapping-nya!

---

## 🔧 PREPROCESSING: FUNGSI `apply_clahe()` (Baris 18-44)

### 📝 LOGIKA PYTHON:

```python
def apply_clahe(img):
    # 1. Konversi BGR → LAB
    lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)

    # 2. Cek CUDA availability
    if CUDA_AVAILABLE:
        # GPU processing
        gpu_l = cv2.cuda_GpuMat()
        gpu_l.upload(l)
        clahe = cv2.cuda.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        gpu_l = clahe.apply(gpu_l, cv2.cuda_Stream.Null())
        l = gpu_l.download()
    else:
        # CPU processing
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        l = clahe.apply(l)

    # 3. Merge kembali LAB → BGR
    lab = cv2.merge((l, a, b))
    return cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)
```

### ✅ IMPLEMENTASI ANDROID:

**File:** `AdaptiveCLAHEProcessor.kt`

```kotlin
object AdaptiveCLAHEProcessor {
    private const val CLAHE_CLIP_LIMIT = 2.0  // FIXED dari Python
    private val TILE_GRID = Size(8.0, 8.0)    // FIXED dari Python

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)  // RGBA
        
        // RGBA → BGR (OpenCV standard)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

        // BGR → LAB
        val labMat = Mat()
        Imgproc.cvtColor(mat, labMat, Imgproc.COLOR_BGR2Lab)

        // Split L, a, b channels
        val channels = mutableListOf<Mat>()
        Core.split(labMat, channels)
        val lChannel = channels[0]

        // Terapkan CLAHE pada L channel saja
        val clahe = Imgproc.createCLAHE(CLAHE_CLIP_LIMIT, TILE_GRID)
        val lEnhanced = Mat()
        clahe.apply(lChannel, lEnhanced)
        channels[0] = lEnhanced

        // Merge kembali
        Core.merge(channels, labMat)
        Imgproc.cvtColor(labMat, mat, Imgproc.COLOR_Lab2BGR)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)

        // Release memory
        mat.release()
        labMat.release()
        channels.forEach { it.release() }
        lEnhanced.release()
        
        return result
    }
}
```

**🔑 PARAMETER KRITIS:**
- `clipLimit = 2.0` (FIXED, jangan ubah)
- `tileGridSize = (8, 8)` (FIXED, jangan ubah)
- CLAHE hanya pada **L channel** dari LAB color space
- Channel a dan b tetap tidak berubah

---

## 🤖 KLASIFIKASI: CLASS `AnemiaClassifier` (Baris 46-60)

### 📝 LOGIKA PYTHON:

```python
class AnemiaClassifier:
    def __init__(self, model_path):
        self.interpreter = tflite.Interpreter(model_path=model_path)
        self.interpreter.allocate_tensors()
        self.input_details = self.interpreter.get_input_details()
        self.output_details = self.interpreter.get_output_details()
        self.input_size = self.input_details[0]['shape'][1]  # DINAMIS!

    def predict(self, crop_img):
        # 1. Resize ke input_size
        img_resized = cv2.resize(crop_img, (self.input_size, self.input_size))
        
        # 2. Normalize /255.0 → float32
        img_input = np.expand_dims(img_resized.astype(np.float32) / 255.0, axis=0)

        # 3. Run inference
        self.interpreter.set_tensor(self.input_details[0]['index'], img_input)
        self.interpreter.invoke()
        
        # 4. Get output
        output_data = self.interpreter.get_tensor(self.output_details[0]['index'])[0]
        idx = np.argmax(output_data)
        return CLASS_NAMES[idx], output_data[idx]
```

### ✅ IMPLEMENTASI ANDROID:

**File:** `AnemiaClassifier.kt`

```kotlin
class AnemiaClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {

    companion object {
        private const val MODEL_PATH = "models/classify/best_float32.tflite"
        val CLASS_NAMES = mapOf(0 to "Anemia", 1 to "Non-Anemia")
    }

    private val interpreter: Interpreter
    val inputSize: Int  // DINAMIS dari model

    init {
        val options = Interpreter.Options().apply {
            numThreads = 4  // CPU only
        }
        interpreter = Interpreter(loadModelBuffer(context, MODEL_PATH), options)
        
        // BACA INPUT SIZE DINAMIS (KRITIS!)
        inputSize = interpreter.getInputTensor(0).shape()[1]
    }

    data class ClassificationResult(
        val label: String,
        val confidence: Float,
        val allScores: FloatArray,  // [score_Anemia, score_NonAnemia]
        val isAnemic: Boolean
    )

    fun classify(conjunctivaCrop: Bitmap): ClassificationResult {
        // 1. Resize ke inputSize × inputSize
        val resized = Bitmap.createScaledBitmap(
            conjunctivaCrop, 
            inputSize, 
            inputSize, 
            true
        )

        // 2. Konversi ke float array, normalize /255.0
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

    override fun close() {
        interpreter.close()
    }
}
```

**🔑 POIN KRITIS:**
1. **Input size DINAMIS** - baca dari `interpreter.getInputTensor(0).shape()[1]`
2. **Normalisasi /255.0** - wajib untuk FLOAT32 model
3. **Output 2 elemen** - `[score_Anemia, score_NonAnemia]`
4. **Expose semua scores** - jangan hanya tampilkan argmax

---

## 🎥 MAIN LOOP: FUNGSI `start_live_inference()` (Baris 62-120)

### 📝 ALUR PYTHON:

```python
def start_live_inference():
    # 1. Load models
    model_seg = YOLO(MODEL_SEG_PATH, task="segment")
    classifier = AnemiaClassifier(MODEL_CLS_PATH)
    
    # 2. Setup camera
    cap = cv2.VideoCapture(CAMERA_INDEX)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
    
    while True:
        ret, frame = cap.read()
        
        # 3. PREPROCESSING - Apply CLAHE
        processed_frame = apply_clahe(frame)
        
        # 4. SEGMENTASI - Deteksi konjungtiva
        results = model_seg.predict(processed_frame, conf=0.35, verbose=False)
        
        if results[0].masks is not None:
            # 5. PILIH AREA TERBESAR
            areas = [cv2.contourArea(pts) for pts in results[0].masks.xy]
            best_idx = np.argmax(areas)  # ARGMAX AREA!
            
            polygon = results[0].masks.xy[best_idx].astype(np.int32)
            bbox = results[0].boxes.xyxy[best_idx].cpu().numpy().astype(int)
            
            # 6. MASKING & CROPPING
            mask = np.zeros((h_orig, w_orig), dtype=np.uint8)
            cv2.fillPoly(mask, [polygon], 255)
            silhouette_full = cv2.bitwise_and(processed_frame, processed_frame, mask=mask)
            
            x1, y1, x2, y2 = bbox
            crop_to_model = silhouette_full[y1:y2, x1:x2]
            
            # 7. KLASIFIKASI
            label, confidence = classifier.predict(crop_to_model)
            
            # 8. VISUALISASI
            color = (0, 0, 255) if label == "Anemic" else (0, 255, 0)
            cv2.polylines(display_frame, [polygon], True, color, 3)
```

### ✅ MAPPING KE ANDROID:

#### **STEP 1: Setup Camera (CameraX)**

```kotlin
// Resolusi WAJIB 1280×720 (identik Python)
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(1280, 720))
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
    .build()
```

#### **STEP 2: Preprocessing**

```kotlin
// Apply CLAHE (identik dengan Python)
val processedFrame = AdaptiveCLAHEProcessor.apply(originalFrame)
```

#### **STEP 3: Segmentasi**

```kotlin
// Confidence threshold = 0.35 (FIXED dari Python)
val segResult = segmentor.segment(
    processedBitmap = processedFrame,
    originalWidth = 1280,
    originalHeight = 720,
    confThreshold = 0.35f  // FIXED!
)
```

#### **STEP 4: Pilih Area Terbesar (KRITIS!)**

```python
# Python: np.argmax(areas)
areas = [cv2.contourArea(pts) for pts in results[0].masks.xy]
best_idx = np.argmax(areas)
```

```kotlin
// Android: WAJIB pilih polygon dengan area TERBESAR
fun selectLargestPolygon(polygons: List<List<PointF>>): List<PointF> {
    return polygons.maxByOrNull { polygon ->
        computePolygonArea(polygon)  // Shoelace formula
    } ?: emptyList()
}

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

#### **STEP 5: Masking & Cropping**

```python
# Python
mask = np.zeros((h, w), dtype=np.uint8)
cv2.fillPoly(mask, [polygon], 255)
silhouette = cv2.bitwise_and(frame, frame, mask=mask)
crop = silhouette[y1:y2, x1:x2]
```

```kotlin
// Android
fun cropConjunctiva(
    processedBitmap: Bitmap,
    polygon: List<PointF>,
    bbox: RectF
): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(processedBitmap, mat)
    
    // Create mask
    val mask = Mat.zeros(mat.rows(), mat.cols(), CvType.CV_8UC1)
    val points = MatOfPoint()
    points.fromList(polygon.map { Point(it.x.toDouble(), it.y.toDouble()) })
    
    val contours = listOf(points)
    Imgproc.fillPoly(mask, contours, Scalar(255.0))
    
    // Apply mask
    val masked = Mat()
    Core.bitwise_and(mat, mat, masked, mask)
    
    // Crop by bbox
    val rect = Rect(
        bbox.left.toInt(),
        bbox.top.toInt(),
        (bbox.right - bbox.left).toInt(),
        (bbox.bottom - bbox.top).toInt()
    )
    val cropped = Mat(masked, rect)
    
    val result = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(cropped, result)
    
    // Release
    mat.release()
    mask.release()
    masked.release()
    cropped.release()
    points.release()
    
    return result
}
```

#### **STEP 6: Klasifikasi**

```kotlin
val classResult = classifier.classify(croppedConjunctiva)
```

#### **STEP 7: Visualisasi**

```python
# Python: Warna berdasarkan label
color = (0, 0, 255) if label == "Anemic" else (0, 255, 0)  # BGR
cv2.polylines(frame, [polygon], True, color, 3)
```

```kotlin
// Android: Warna berdasarkan hasil
val color = when {
    classResult == null -> Color(0xFF007AFF)  // Biru - segmentasi saja
    classResult.isAnemic -> Color(0xFFFF3B30) // Merah - Anemia
    else -> Color(0xFF34C759)                  // Hijau - Non-Anemia
}

// Draw polygon dengan alpha fill
drawPath(path, color.copy(alpha = 0.30f), style = Fill)
drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
```

---

## 🎯 PARAMETER KRITIS YANG WAJIB IDENTIK

| Parameter | Nilai Python | Nilai Android | Keterangan |
|-----------|--------------|---------------|------------|
| **Camera Resolution** | 1280×720 | `Size(1280, 720)` | FIXED |
| **CLAHE clipLimit** | 2.0 | `2.0` | FIXED |
| **CLAHE tileGridSize** | (8, 8) | `Size(8.0, 8.0)` | FIXED |
| **Segmentation Confidence** | 0.35 | `0.35f` | FIXED |
| **Polygon Selection** | `np.argmax(areas)` | `maxByOrNull { area }` | AREA TERBESAR |
| **Classification Normalization** | `/255.0` | `/255.0f` | FLOAT32 |
| **Class Mapping** | `{0: "Anemic", 1: "Non-Anemic"}` | `mapOf(0 to "Anemia", 1 to "Non-Anemia")` | Index 0 = Anemia |

---

## ⚠️ PERBEDAAN PYTHON vs ANDROID

### 1. **Color Space**

| Python | Android |
|--------|---------|
| OpenCV default = **BGR** | Bitmap default = **RGB** |
| `cv2.imread()` → BGR | `Utils.bitmapToMat()` → **RGBA** |

**Solusi Android:**
```kotlin
// WAJIB konversi RGBA → BGR sebelum processing
Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

// Setelah selesai, konversi BGR → RGBA
Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)
```

### 2. **GPU Processing**

| Python | Android |
|--------|---------|
| `cv2.cuda.createCLAHE()` jika CUDA tersedia | OpenCV Android **TIDAK support CUDA** |
| Fallback ke CPU jika CUDA tidak ada | Gunakan CPU OpenCV saja |

**Implementasi Android:**
```kotlin
// Tidak perlu cek CUDA - langsung CPU
val clahe = Imgproc.createCLAHE(clipLimit, tileGrid)
clahe.apply(lChannel, lEnhanced)
```

### 3. **Model Loading**

| Python | Android |
|--------|---------|
| `tflite.Interpreter(model_path=path)` | `Interpreter(loadModelBuffer(context, path))` |
| Path langsung ke file | Load dari assets via `AssetManager` |

---

## 📊 ALUR LENGKAP PIPELINE

```
┌─────────────────────────────────────────────────────────────┐
│ 1. CAPTURE FRAME (1280×720)                                 │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. PREPROCESSING: apply_clahe()                             │
│    - BGR → LAB                                              │
│    - CLAHE pada L channel (clipLimit=2.0, tile=8×8)        │
│    - LAB → BGR                                              │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. SEGMENTASI: model_seg.predict()                          │
│    - Input: processed_frame                                 │
│    - Confidence: 0.35                                       │
│    - Output: masks, boxes                                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. PILIH AREA TERBESAR: np.argmax(areas)                   │
│    - Hitung area setiap polygon                             │
│    - Pilih yang TERBESAR (bukan confidence tertinggi!)     │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. MASKING & CROPPING                                       │
│    - Buat binary mask dari polygon                          │
│    - fillPoly(mask, polygon, 255)                           │
│    - bitwise_and untuk apply mask                           │
│    - Crop berdasarkan bounding box                          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. KLASIFIKASI: classifier.predict()                        │
│    - Resize crop ke input_size (dinamis)                    │
│    - Normalize /255.0 → float32                             │
│    - Run TFLite inference                                   │
│    - Output: [score_Anemia, score_NonAnemia]                │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. VISUALISASI                                              │
│    - Warna: Merah (Anemic) / Hijau (Non-Anemic)            │
│    - Draw polygon dengan polylines                          │
│    - Draw label + confidence                                │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ CHECKLIST IMPLEMENTASI ANDROID

- [ ] Camera resolution 1280×720 (identik Python)
- [ ] CLAHE clipLimit=2.0, tileGrid=(8,8) (identik Python)
- [ ] CLAHE hanya pada L channel LAB (identik Python)
- [ ] Segmentation confidence=0.35 (identik Python)
- [ ] Pilih polygon dengan **AREA TERBESAR** (bukan confidence)
- [ ] Masking dengan fillPoly + bitwise_and (identik Python)
- [ ] Crop berdasarkan bounding box (identik Python)
- [ ] Classification input size **DINAMIS** dari model
- [ ] Normalisasi /255.0 untuk FLOAT32 (identik Python)
- [ ] Output 2 scores: [Anemia, Non-Anemia] (identik Python)
- [ ] Mapping: index 0=Anemia, 1=Non-Anemia (identik Python)
- [ ] Visualisasi: Merah=Anemic, Hijau=Non-Anemic (identik Python)
- [ ] Color space conversion: RGBA↔BGR (Android specific)

---

**📌 CATATAN AKHIR:**

Dokumen ini adalah **REFERENSI UTAMA** untuk implementasi Android.  
Setiap parameter, setiap urutan, setiap logika HARUS identik dengan `live_inference.py`.  
Jangan berasumsi atau menggunakan metode di luar logika file ini.

**Mode A (Referensi live_inference.py) - AKTIF ✅**
