# TFLite Models

## Required Models

Please copy the following TFLite models to their respective directories:

### 1. Segmentation Model
**Source:** `AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite`

**Destination:** `app/src/main/assets/models/segments/best_int8.tflite`

**Specifications:**
- Format: INT8 quantized
- Input size: 320×320
- Task: Instance segmentation
- Class: conjunctiva (single class)
- NMS: Embedded in model

### 2. Classification Model
**Source:** `AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite`

**Destination:** `app/src/main/assets/models/classify/best_float32.tflite`

**Specifications:**
- Format: FLOAT32
- Input size: Dynamic (read from model at runtime)
- Task: Binary classification
- Classes: Anemia (index 0), Non-Anemia (index 1)

## Manual Copy Instructions

### Windows (PowerShell):
```powershell
# Copy segmentation model
Copy-Item "AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite" -Destination "app/src/main/assets/models/segments/best_int8.tflite"

# Copy classification model
Copy-Item "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" -Destination "app/src/main/assets/models/classify/best_float32.tflite"
```

### Linux/Mac:
```bash
# Copy segmentation model
cp "AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite" "app/src/main/assets/models/segments/best_int8.tflite"

# Copy classification model
cp "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" "app/src/main/assets/models/classify/best_float32.tflite"
```

## Verification

After copying, verify the files exist:
- `app/src/main/assets/models/segments/best_int8.tflite` (should be ~2-5 MB)
- `app/src/main/assets/models/classify/best_float32.tflite` (should be ~20-50 MB)

## Important Notes

1. **DO NOT** rename the models - the app expects these exact filenames
2. **DO NOT** use different model versions without updating the code
3. The segmentation model MUST be INT8 quantized
4. The classification model MUST be FLOAT32 (not INT8)
5. Both models must be exported with the correct input sizes as specified in CLAUDE.md
