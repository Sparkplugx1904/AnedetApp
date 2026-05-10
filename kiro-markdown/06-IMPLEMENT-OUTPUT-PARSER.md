# Implement Output Parser - Display Polygon & Detection

## Masalah
- Inference berhasil tapi tidak ada polygon yang muncul
- Tombol capture tidak menampilkan hasil
- Parser return `null` sehingga tidak ada detection

## Solusi

### Implement Parser untuk Output [1, 300, 38]

**Implementation:**
```kotlin
private fun parseOutput(
    output: Array<Array<FloatArray>>,
    originalWidth: Int,
    originalHeight: Int
): SegmentationResult? {
    val detections = output[0]  // [300, 38]
    
    // Find best detection above threshold
    for (i in 0 until 300) {
        val detection = detections[i]
        
        // Extract data
        val x1 = detection[0]
        val y1 = detection[1]
        val x2 = detection[2]
        val y2 = detection[3]
        val confidence = detection[4]
        val classId = detection[5].toInt()
        
        // Filter by confidence threshold
        if (confidence < CONF_THRESHOLD) {
            break  // Detections sorted by confidence
        }
        
        // Filter by class (0 = conjunctiva)
        if (classId != 0) continue
        
        // Denormalize coordinates from [0-1] to original frame size
        val x1Denorm = x1 * originalWidth
        val y1Denorm = y1 * originalHeight
        val x2Denorm = x2 * originalWidth
        val y2Denorm = y2 * originalHeight
        
        // Create bounding box
        val bbox = RectF(x1Denorm, y1Denorm, x2Denorm, y2Denorm)
        
        // Convert bbox to polygon (simple rectangle)
        val polygon = listOf(
            PointF(x1Denorm, y1Denorm),
            PointF(x2Denorm, y1Denorm),
            PointF(x2Denorm, y2Denorm),
            PointF(x1Denorm, y2Denorm)
        )
        
        return SegmentationResult(
            polygon = polygon,
            boundingBox = bbox,
            confidence = confidence
        )
    }
    
    return null  // No detection above threshold
}
```

## Output Format Breakdown

### Model Output: [1, 300, 38]
```
Dimension 0: Batch = 1
Dimension 1: Max detections = 300 (sorted by confidence, descending)
Dimension 2: Data per detection = 38
```

### Data Format (38 values):
```
Index 0-3:   Bounding box [x1, y1, x2, y2] (normalized 0-1)
Index 4:     Confidence score (0-1)
Index 5:     Class ID (0 = conjunctiva)
Index 6-37:  Mask coefficients (32 values) - untuk segmentation mask
```

## Key Features

### 1. Early Break Optimization
```kotlin
if (confidence < CONF_THRESHOLD) {
    break  // Detections sorted, no need to check rest
}
```
Karena detections sudah sorted by confidence (descending), kita bisa break early saat confidence < threshold.

### 2. Coordinate Denormalization
```kotlin
val x1Denorm = x1 * originalWidth
val y1Denorm = y1 * originalHeight
```
Model output normalized [0-1], perlu denormalize ke pixel coordinates.

### 3. Simple Rectangle Polygon
```kotlin
val polygon = listOf(
    PointF(x1, y1),  // Top-left
    PointF(x2, y1),  // Top-right
    PointF(x2, y2),  // Bottom-right
    PointF(x1, y2)   // Bottom-left
)
```
Untuk sementara gunakan rectangle. Bisa di-improve dengan decode mask coefficients.

### 4. Class Filtering
```kotlin
if (classId != 0) continue
```
Hanya ambil class 0 (conjunctiva), skip class lain.

## Flow Lengkap

### 1. Inference
```
Input: Bitmap 320x320 (preprocessed)
  ↓
Model: FP16 NMS-embedded
  ↓
Output: [1, 300, 38]
```

### 2. Parse
```
Loop 300 detections:
  - Extract bbox, confidence, class
  - Filter by confidence >= 0.35
  - Filter by class == 0
  - Denormalize coordinates
  - Create polygon
  - Return first valid detection
```

### 3. Display
```
SegmentationResult:
  - polygon: List<PointF> (4 corners)
  - boundingBox: RectF
  - confidence: Float
  ↓
ConjunctivaOverlay draws polygon
  ↓
User sees blue rectangle on conjunctiva
```

## Expected Behavior

### Live Segmentation Mode:
1. User arahkan kamera ke conjunctiva
2. Model detect conjunctiva (confidence >= 0.35)
3. **Blue rectangle polygon** muncul di screen
4. Polygon update setiap ~1-2 detik

### Capture Mode:
1. User klik tombol camera
2. Model detect conjunctiva
3. Crop conjunctiva region
4. Classify (Anemia/Non-Anemia)
5. **Result sheet** muncul dengan:
   - Masked image
   - Classification result
   - Confidence scores

## Logging

### Success Detection:
```
D ConjunctivaSegmentor: Inference success, output shape: [1, 300, 38]
D ConjunctivaSegmentor: Detection: bbox=RectF(...), conf=0.85, class=0
D ConjunctivaSegmentor: Detection found: confidence=0.85
```

### No Detection:
```
D ConjunctivaSegmentor: Inference success, output shape: [1, 300, 38]
D ConjunctivaSegmentor: No detection above threshold
```

## Future Improvements

### 1. Decode Mask Coefficients
Gunakan 32 mask coefficients (index 6-37) untuk generate polygon yang lebih akurat:
```kotlin
val maskCoeffs = detection.sliceArray(6..37)
// Decode mask using prototype masks
// Generate contour polygon
```

### 2. Multi-Detection Support
Jika ada multiple conjunctiva (left & right eye):
```kotlin
val allDetections = mutableListOf<SegmentationResult>()
for (i in 0 until 300) {
    // ... parse detection
    if (valid) allDetections.add(result)
}
return allDetections  // Return list instead of single
```

### 3. Confidence Threshold Tuning
Adjust `CONF_THRESHOLD` based on real-world testing:
```kotlin
const val CONF_THRESHOLD = 0.25f  // Lower for more detections
const val CONF_THRESHOLD = 0.50f  // Higher for more precision
```

## Testing

### Build & Install:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Monitor Logs:
```bash
adb logcat -s "ConjunctivaSegmentor:D"
```

### Expected Results:
1. ✅ Polygon muncul saat arahkan ke conjunctiva
2. ✅ Polygon update real-time
3. ✅ Capture button menampilkan result sheet
4. ✅ Classification result muncul

## Files Changed
- `app/src/main/java/com/example/anemiadetector/ml/segmentation/ConjunctivaSegmentor.kt`
  - Add `parseOutput()` function
  - Update `segment()` to call parser
  - Add logging untuk detection

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| Parser | Return `null` | Parse output [1, 300, 38] |
| Polygon | Not displayed | Blue rectangle displayed |
| Detection | No detection | Detect conjunctiva |
| Capture | No result | Result sheet displayed |
| Logging | "Inference success" | "Detection found: conf=X" |

**Status:** ✅ Parser implemented, polygon should display now!
