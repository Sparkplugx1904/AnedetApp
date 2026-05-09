# 📦 Copy TFLite Models - WAJIB!

## ⚠️ CRITICAL: Aplikasi TIDAK AKAN BERJALAN tanpa model TFLite!

Jalankan command berikut di PowerShell dari root project:

## Windows PowerShell:

```powershell
# Copy Segmentation Model (INT8)
Copy-Item "AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite" -Destination "app/src/main/assets/models/segments/best_int8.tflite"

# Copy Classification Model (FLOAT32)
Copy-Item "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" -Destination "app/src/main/assets/models/classify/best_float32.tflite"
```

## Linux/Mac:

```bash
# Copy Segmentation Model (INT8)
cp "AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite" "app/src/main/assets/models/segments/best_int8.tflite"

# Copy Classification Model (FLOAT32)
cp "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" "app/src/main/assets/models/classify/best_float32.tflite"
```

## Verifikasi:

Setelah copy, pastikan file ada di:
- ✅ `app/src/main/assets/models/segments/best_int8.tflite`
- ✅ `app/src/main/assets/models/classify/best_float32.tflite`

## Ukuran File:

- Segmentation model: ~2-5 MB
- Classification model: ~20-50 MB

---

**Setelah copy models, jalankan:**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

**Atau via Android Studio:**
- Run → Run 'app' (Shift+F10)
