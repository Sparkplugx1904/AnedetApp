# 🔬 PARAMETER PREPROCESSING DETAIL

> **Spesifikasi lengkap parameter preprocessing dari `live_inference.py` dan `CLAUDE.md`**  
> Setiap nilai WAJIB identik dengan Python untuk hasil yang konsisten

---

## 🎯 OVERVIEW

Dokumen ini menjelaskan secara detail:
1. **Parameter CLAHE** dari `live_inference.py`
2. **Parameter preprocessing lengkap** dari `CLAUDE.md`
3. **Color space conversion** (RGB vs BGR)
4. **Input size dan normalisasi** untuk model
5. **Polygon selection** (area terbesar)

---

## 📋 1. PARAMETER DARI LIVE_INFERENCE.PY

### 🖼️ **CLAHE (Contrast Limited Adaptive Histogram Equalization)**

**Lokasi di Python:** Fungsi `apply_clahe()` (baris 18-44)

```python
def apply_clahe(img):
    lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)
    
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    l = clahe.apply(l)
    
    lab = cv2.merge((l, a, b))
    return cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)
```

#### **Parameter Kritis:**

| Parameter | Nilai Python | Nilai Android | Tipe | Keterangan |
|-----------|--------------|---------------|------|------------|
| `clipLimit` | `2.0` | `2.0` | Double | FIXED - Batas kontras |
| `tileGridSize` | `(8, 8)` | `Size(8.0, 8.0)` | Size | FIXED - Ukuran grid tile |
| **Channel yang diproses** | **L channel saja** | **L channel saja** | - | a dan b tetap tidak berubah |
| **Color space** | LAB | LAB | - | BUKAN RGB atau HSV |

#### **Implementasi Android:**

```kotlin
object SimpleCLAHEProcessor {
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
        val lChannel = channels[0]  // L channel
        val aChannel = channels[1]  // a channel (tidak diubah)
        val bChannel = channels[2]  // b channel (tidak diubah)

        // Terapkan CLAHE HANYA pada L channel
        val clahe = Imgproc.createCLAHE(CLAHE_CLIP_LIMIT, TILE_GRID)
        val lEnhanced = Mat()
        clahe.apply(lChannel, lEnhanced)
        
        // Replace L channel dengan yang sudah di-enhance
        channels[0] = lEnhanced

        // Merge kembali
        Core.merge(channels, labMat)
        
        // LAB → BGR
        Imgproc.cvtColor(labMat, mat, Imgproc.COLOR_Lab2BGR)
        
        // BGR → RGBA (untuk Bitmap Android)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)

        // Release memory
        mat.release()
        labMat.release()
        lChannel.release()
        aChannel.release()
        bChannel.release()
        lEnhanced.release()
        
        return result
    }
}
```

---

### 🎥 **Camera Resolution**

**Lokasi di Python:** Fungsi `start_live_inference()` (baris 68-70)

```python
cap = cv2.VideoCapture(CAMERA_INDEX)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| Width | `1280` | FIXED - Jangan ubah |
| Height | `720` | FIXED - Jangan ubah |
| Aspect Ratio | `16:9` | Standard HD |

#### **Implementasi Android:**

```kotlin
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(android.util.Size(1280, 720))  // FIXED
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
    .build()
```

---

### 🎯 **Segmentation Confidence Threshold**

**Lokasi di Python:** Fungsi `start_live_inference()` (baris 82)

```python
results = model_seg.predict(processed_frame, conf=0.35, verbose=False)
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| `conf` | `0.35` | FIXED - Confidence threshold |

#### **Implementasi Android:**

```kotlin
class ConjunctivaSegmentor {
    companion object {
        const val CONF_THRESHOLD = 0.35f  // FIXED dari Python
    }
    
    fun segment(
        processedBitmap: Bitmap,
        originalWidth: Int,
        originalHeight: Int
    ): SegmentationResult? {
        // Filter deteksi dengan confidence < CONF_THRESHOLD
        // ...
    }
}
```

---

### 📐 **Polygon Selection: AREA TERBESAR**

**Lokasi di Python:** Fungsi `start_live_inference()` (baris 84-86)

```python
areas = [cv2.contourArea(pts.astype(np.float32)) for pts in results[0].masks.xy]
best_idx = np.argmax(areas)  # PILIH AREA TERBESAR!
```

#### **KRITIS:**

**BUKAN** pilih berdasarkan confidence tertinggi!  
**WAJIB** pilih berdasarkan **AREA POLYGON TERBESAR**!

#### **Implementasi Android:**

```kotlin
fun selectLargestPolygon(polygons: List<List<PointF>>): List<PointF>? {
    if (polygons.isEmpty()) return null
    
    return polygons.maxByOrNull { polygon ->
        computePolygonArea(polygon)  // Shoelace formula
    }
}

private fun computePolygonArea(polygon: List<PointF>): Float {
    if (polygon.size < 3) return 0f
    
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

---

### 🔢 **Classification Input Size (DINAMIS)**

**Lokasi di Python:** Class `AnemiaClassifier.__init__()` (baris 51)

```python
self.input_size = self.input_details[0]['shape'][1]  # DINAMIS!
```

#### **KRITIS:**

**JANGAN hardcode** input size (misal 224 atau 448)!  
**WAJIB baca** dari model saat runtime!

#### **Implementasi Android:**

```kotlin
class AnemiaClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {
    
    private val interpreter: Interpreter
    val inputSize: Int  // DINAMIS dari model
    
    init {
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(loadModelBuffer(context, MODEL_PATH), options)
        
        // BACA INPUT SIZE DINAMIS (KRITIS!)
        inputSize = interpreter.getInputTensor(0).shape()[1]
        
        Log.d("AnemiaClassifier", "Model input size: $inputSize")
    }
}
```

---

### 🎨 **Classification Normalization**

**Lokasi di Python:** Class `AnemiaClassifier.predict()` (baris 55)

```python
img_input = np.expand_dims(img_resized.astype(np.float32) / 255.0, axis=0)
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| Normalisasi | `/255.0` | Pixel value 0-255 → 0.0-1.0 |
| Data type | `float32` | BUKAN int8 atau uint8 |

#### **Implementasi Android:**

```kotlin
fun classify(conjunctivaCrop: Bitmap): ClassificationResult {
    val resized = Bitmap.createScaledBitmap(
        conjunctivaCrop, 
        inputSize,  // DINAMIS
        inputSize, 
        true
    )

    // Konversi ke float array, normalize /255.0
    val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
    inputBuffer.order(ByteOrder.nativeOrder())

    val pixels = IntArray(inputSize * inputSize)
    resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
    
    for (pixel in pixels) {
        // Normalize /255.0 → float32
        inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)  // R
        inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)   // G
        inputBuffer.putFloat((pixel and 0xFF) / 255.0f)            // B
    }

    // Run inference
    val outputBuffer = Array(1) { FloatArray(2) }
    interpreter.run(inputBuffer, outputBuffer)
    
    // ...
}
```

---

### 🏷️ **Class Label Mapping**

**Lokasi di Python:** Konstanta `CLASS_NAMES` (baris 15)

```python
CLASS_NAMES = {0: "Anemic", 1: "Non-Anemic"}
```

#### **Parameter:**

| Index | Label Python | Label Android | Keterangan |
|-------|--------------|---------------|------------|
| `0` | `"Anemic"` | `"Anemia"` | Positif anemia |
| `1` | `"Non-Anemic"` | `"Non-Anemia"` | Negatif anemia |

#### **Implementasi Android:**

```kotlin
class AnemiaClassifier {
    companion object {
        val CLASS_NAMES = mapOf(
            0 to "Anemia",      // Index 0 = Anemia
            1 to "Non-Anemia"   // Index 1 = Non-Anemia
        )
    }
    
    fun classify(crop: Bitmap): ClassificationResult {
        // ...
        val scores = outputBuffer[0]  // [score_Anemia, score_NonAnemia]
        val predictedIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
        val label = CLASS_NAMES[predictedIdx] ?: "Unknown"
        
        return ClassificationResult(
            label = label,
            confidence = scores[predictedIdx],
            allScores = scores,
            isAnemic = predictedIdx == 0  // Index 0 = Anemia
        )
    }
}
```

---

## 📋 2. PARAMETER DARI CLAUDE.MD (PREPROCESSING LENGKAP)

### 🌈 **Gray World White Balance**

**Lokasi di CLAUDE.md:** Bagian 3.2

```kotlin
object GrayWorldWhiteBalance {
    private const val WB_STRENGTH = 0.8f
    private const val SCALE_MIN = 0.5f
    private const val SCALE_MAX = 1.8f
}
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| `WB_STRENGTH` | `0.8` | Strength blending |
| `SCALE_MIN` | `0.5` | Minimum scale factor |
| `SCALE_MAX` | `1.8` | Maximum scale factor |

#### **Formula:**

```
mean_gray = (mean_R + mean_G + mean_B) / 3.0
scale_ch = mean_gray / mean_ch
scale_ch_clipped = clip(scale_ch, SCALE_MIN, SCALE_MAX)
scale_final = 1.0 + (scale_ch_clipped - 1.0) * WB_STRENGTH
```

---

### 🌟 **Adaptive Gamma Correction**

**Lokasi di CLAUDE.md:** Bagian 3.1 Step 2

```kotlin
object AdaptiveGammaCorrector {
    private const val GAMMA_MIN = 0.5f
    private const val GAMMA_MAX = 1.2f
}
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| `GAMMA_MIN` | `0.5` | Minimum gamma value |
| `GAMMA_MAX` | `1.2` | Maximum gamma value |

#### **Formula:**

```
mean_L = mean(L_channel dari LAB)
gamma = GAMMA_MIN + (GAMMA_MAX - GAMMA_MIN) * (mean_L / 0.9)
gamma_clipped = clip(gamma, GAMMA_MIN, GAMMA_MAX)
output = pixel^gamma
```

---

### 📦 **Letterbox Resize**

**Lokasi di CLAUDE.md:** Bagian 3.1 Step 3

```kotlin
object LetterboxResizer {
    private const val TARGET_SIZE = 224
}
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| `TARGET_SIZE` | `224` | Output size (224×224) |
| Padding color | `BLACK (0, 0, 0)` | Padding hitam |

#### **Formula:**

```
scale = TARGET_SIZE / max(width, height)
new_width = width * scale
new_height = height * scale
x_offset = (TARGET_SIZE - new_width) / 2
y_offset = (TARGET_SIZE - new_height) / 2
```

---

### 🔍 **Bilateral Filter**

**Lokasi di CLAUDE.md:** Bagian 3.1 Step 4

```kotlin
object BilateralFilterProcessor {
    private const val KERNEL_DIAMETER = 9
    private const val SIGMA_COLOR = 25.5   // 0.1 * 255
    private const val SIGMA_SPACE = 1.5
}
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| `KERNEL_DIAMETER` | `9` | Kernel size 9×9 |
| `SIGMA_COLOR` | `25.5` | 0.1 dalam skala 0-255 |
| `SIGMA_SPACE` | `1.5` | Spatial sigma |

**⚠️ CATATAN:**
- Python (Kornia): `sigma_color=0.1` dalam range [0, 1]
- Android (OpenCV): `sigma_color=25.5` dalam range [0, 255]
- Konversi: `0.1 * 255 = 25.5`

---

### 🎨 **Adaptive CLAHE (dari CLAUDE.md)**

**Lokasi di CLAUDE.md:** Bagian 3.1 Step 5

```kotlin
object AdaptiveCLAHEProcessor {
    private const val CLAHE_CLIP_MIN = 8.0
    private const val CLAHE_CLIP_MAX = 25.0
    private val TILE_GRID = Size(8.0, 8.0)
}
```

#### **Parameter:**

| Parameter | Nilai | Keterangan |
|-----------|-------|------------|
| `CLAHE_CLIP_MIN` | `8.0` | Minimum clip limit |
| `CLAHE_CLIP_MAX` | `25.0` | Maximum clip limit |
| `TILE_GRID` | `(8, 8)` | Grid tile size |

#### **Formula Adaptive Clip:**

```
std_L = standard_deviation(L_channel)
clip_limit = max(8, min(25, 25 - (25-8) * (std_L / (0.20 * 255))))
```

**⚠️ PERBEDAAN:**
- `live_inference.py` menggunakan **FIXED clip=2.0**
- `CLAUDE.md` menggunakan **ADAPTIVE clip 8-25**

**SOLUSI:**
- Untuk **Mode A (live_inference.py)**: Gunakan `clipLimit=2.0` (FIXED)
- Untuk **Mode B (CLAUDE.md full)**: Gunakan adaptive clip 8-25

**REKOMENDASI:** Ikuti `live_inference.py` → **clipLimit=2.0 FIXED**

---

## 🎨 3. COLOR SPACE CONVERSION

### **Python (OpenCV) vs Android (Bitmap)**

| Aspek | Python | Android |
|-------|--------|---------|
| **Default color space** | BGR | RGB |
| `cv2.imread()` | BGR | - |
| `Bitmap` | - | RGB (ARGB_8888) |
| `Utils.bitmapToMat()` | - | **RGBA** (bukan BGR!) |

### **Konversi yang WAJIB:**

```kotlin
// 1. Bitmap → Mat (RGBA)
val mat = Mat()
Utils.bitmapToMat(bitmap, mat)  // Output: RGBA Mat

// 2. RGBA → BGR (untuk processing OpenCV)
Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

// 3. Processing (CLAHE, bilateral, dll)
// ...

// 4. BGR → RGBA (sebelum kembali ke Bitmap)
Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)

// 5. Mat → Bitmap
val result = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
Utils.matToBitmap(mat, result)
```

### **❌ KESALAHAN UMUM:**

```kotlin
// SALAH - Menggunakan COLOR_RGB2BGR
Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR)  // ❌

// BENAR - Menggunakan COLOR_RGBA2BGR
Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)  // ✅
```

---

## 📊 4. RINGKASAN PARAMETER KRITIS

### **Tabel Lengkap:**

| Parameter | Nilai | Source | Keterangan |
|-----------|-------|--------|------------|
| **Camera Resolution** | 1280×720 | live_inference.py | FIXED |
| **CLAHE clipLimit** | 2.0 | live_inference.py | FIXED (Mode A) |
| **CLAHE tileGrid** | (8, 8) | live_inference.py | FIXED |
| **CLAHE channel** | L only | live_inference.py | FIXED |
| **Segmentation conf** | 0.35 | live_inference.py | FIXED |
| **Polygon selection** | argmax(area) | live_inference.py | AREA TERBESAR |
| **Classification input size** | DINAMIS | live_inference.py | Baca dari model |
| **Classification normalization** | /255.0 | live_inference.py | float32 |
| **Class mapping** | 0=Anemia, 1=Non-Anemia | live_inference.py | FIXED |
| **White Balance strength** | 0.8 | CLAUDE.md | Mode B |
| **Gamma range** | 0.5-1.2 | CLAUDE.md | Mode B |
| **Letterbox size** | 224×224 | CLAUDE.md | Mode B |
| **Bilateral kernel** | 9×9 | CLAUDE.md | Mode B |
| **Bilateral sigma_color** | 25.5 | CLAUDE.md | Mode B (0.1*255) |
| **Bilateral sigma_space** | 1.5 | CLAUDE.md | Mode B |

---

## ✅ CHECKLIST VALIDASI

### **Preprocessing:**
- [ ] CLAHE clipLimit = 2.0 (FIXED dari live_inference.py)
- [ ] CLAHE tileGrid = (8, 8) (FIXED)
- [ ] CLAHE hanya pada L channel LAB (bukan RGB atau HSV)
- [ ] Color space conversion: RGBA → BGR → LAB → BGR → RGBA

### **Camera:**
- [ ] Resolution 1280×720 (FIXED)
- [ ] Output format RGBA_8888

### **Segmentation:**
- [ ] Confidence threshold = 0.35 (FIXED)
- [ ] Polygon selection = AREA TERBESAR (bukan confidence)

### **Classification:**
- [ ] Input size DINAMIS (baca dari model)
- [ ] Normalization /255.0 → float32
- [ ] Class mapping: 0=Anemia, 1=Non-Anemia
- [ ] Output 2 scores (expose keduanya)

### **Visualization:**
- [ ] Warna: Biru (segmentasi), Merah (Anemia), Hijau (Non-Anemia)
- [ ] Alpha fill 0.25-0.30
- [ ] Stroke width 3dp

---

**📌 CATATAN AKHIR:**

Semua parameter di atas adalah **NON-NEGOTIABLE**.

Setiap perubahan nilai akan menyebabkan hasil yang berbeda dari Python.

Jika ada keraguan, **TANYAKAN** - jangan asumsikan!
