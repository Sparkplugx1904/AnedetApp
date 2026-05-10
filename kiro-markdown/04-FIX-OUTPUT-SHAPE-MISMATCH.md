# Fix Output Shape Mismatch - Model FP16 NMS-Embedded

## Masalah
```
E Segmentor: java.lang.IllegalArgumentException: Cannot copy from a TensorFlowLite tensor (Identity) 
with shape [1, 300, 38] to a Java object with shape [1, 50400].
```

Model inference gagal karena output buffer shape tidak sesuai dengan model output.

## Root Cause

### Model Output Shape Berbeda
**Kode lama (salah):**
```kotlin
val output0 = Array(1) { FloatArray(8400 * 6) }  // [1, 50400]
```

**Model FP16 actual output:**
```
Shape: [1, 300, 38]
```

### Penjelasan Shape

#### Model Lama (Non-NMS):
- Output: `[1, 8400, 6]`
- Format: `[batch, anchors, data]`
- Data: `[x, y, w, h, confidence, class]`
- Perlu NMS post-processing manual

#### Model FP16 (NMS-Embedded):
- Output: `[1, 300, 38]`
- Format: `[batch, max_detections, data]`
- Data: `[x1, y1, x2, y2, confidence, class_id, mask_coeffs(32)]`
- NMS sudah dilakukan di dalam model
- Max 300 detections (sudah di-filter)

### Breakdown Output [1, 300, 38]:
```
Dimension 0: Batch size = 1
Dimension 1: Max detections = 300 (top 300 after NMS)
Dimension 2: Data per detection = 38
  - [0:4]   → Bounding box (x1, y1, x2, y2) normalized [0-1]
  - [4]     → Confidence score [0-1]
  - [5]     → Class ID (0 = conjunctiva)
  - [6:38]  → Mask coefficients (32 values) untuk segmentation mask
```

## Solusi

### Update Output Buffer Shape

**Sebelum:**
```kotlin
val output0 = Array(1) { FloatArray(8400 * 6) }  // ❌ Wrong shape
```

**Sesudah:**
```kotlin
val output0 = Array(1) { Array(300) { FloatArray(38) } }  // ✅ Correct shape
```

### Kode Lengkap:
```kotlin
fun segment(preprocessedBitmap: Bitmap, originalWidth: Int, originalHeight: Int): SegmentationResult? {
    val resized = Bitmap.createScaledBitmap(preprocessedBitmap, INPUT_SIZE, INPUT_SIZE, true)
    val input = toFloatBuffer(resized)

    // Model FP16 NMS-embedded output shape: [1, 300, 38]
    val output0 = Array(1) { Array(300) { FloatArray(38) } }
    
    try {
        interpreter.run(input, output0)
        Log.d("ConjunctivaSegmentor", "Inference success, output shape: [1, 300, 38]")
        
        // TODO: Parse output untuk extract polygon
        // Untuk sementara return null agar tidak crash
    } catch (e: Exception) {
        Log.e("Segmentor", "Inference failed", e)
    } finally {
        resized.recycle()
    }
    return null
}
```

## Keuntungan Model NMS-Embedded

### 1. **Lebih Efisien**
- NMS dilakukan di GPU/NPU (di dalam model)
- Tidak perlu NMS post-processing di CPU (Kotlin)
- Lebih cepat untuk real-time inference

### 2. **Output Lebih Bersih**
- Hanya 300 detections terbaik (bukan 8400)
- Sudah di-filter berdasarkan confidence threshold
- Sudah di-filter berdasarkan IoU (overlap)

### 3. **Lebih Mudah Di-parse**
- Tidak perlu loop 8400 anchors
- Hanya perlu loop max 300 detections
- Detections sudah sorted by confidence

## Next Steps

### 1. Implement Parser untuk Output [1, 300, 38]
```kotlin
fun parseOutput(output: Array<Array<FloatArray>>): SegmentationResult? {
    val detections = output[0]  // [300, 38]
    
    for (i in 0 until 300) {
        val detection = detections[i]
        
        val x1 = detection[0]
        val y1 = detection[1]
        val x2 = detection[2]
        val y2 = detection[3]
        val confidence = detection[4]
        val classId = detection[5].toInt()
        val maskCoeffs = detection.sliceArray(6..37)  // 32 values
        
        // Filter by confidence threshold
        if (confidence < CONF_THRESHOLD) continue
        
        // Filter by class (0 = conjunctiva)
        if (classId != 0) continue
        
        // Convert bbox to polygon
        // Decode mask using maskCoeffs
        // Return SegmentationResult
    }
    
    return null
}
```

### 2. Decode Segmentation Mask
Model YOLO segmentation menggunakan **mask coefficients** + **prototype masks**:
- 32 mask coefficients per detection
- Prototype masks dari model (perlu extract dari output lain)
- Final mask = sigmoid(coeffs @ prototypes)

**Catatan:** Model ini mungkin perlu output tambahan untuk prototype masks. Perlu check model architecture.

### 3. Alternative: Gunakan Bounding Box Saja
Jika mask decoding terlalu kompleks, bisa gunakan bounding box saja:
```kotlin
// Convert bbox to simple rectangle polygon
val polygon = listOf(
    PointF(x1, y1),
    PointF(x2, y1),
    PointF(x2, y2),
    PointF(x1, y2)
)
```

## Testing
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -d | Select-String -Pattern "ConjunctivaSegmentor|Segmentor"
```

**Expected log:**
```
D ConjunctivaSegmentor: Inference success, output shape: [1, 300, 38]
```

**No more error:**
```
❌ E Segmentor: Cannot copy from a TensorFlowLite tensor (Identity) with shape [1, 300, 38]...
```

## Files Changed
- `app/src/main/java/com/example/anemiadetector/ml/segmentation/ConjunctivaSegmentor.kt`
  - Update output buffer: `Array(1) { Array(300) { FloatArray(38) } }`
  - Add log untuk success inference
  - Add comment untuk TODO parser

## Summary

| Aspect | Old Model | New Model (FP16 NMS) |
|--------|-----------|----------------------|
| Output Shape | `[1, 8400, 6]` | `[1, 300, 38]` |
| NMS | Manual (CPU) | Embedded (GPU) |
| Max Detections | 8400 (all anchors) | 300 (filtered) |
| Data per Detection | 6 | 38 |
| Mask Info | None | 32 coefficients |
| Performance | Slower | Faster |
| Parsing Complexity | Simple | Medium |

**Status:** ✅ Inference berhasil, tinggal implement parser
