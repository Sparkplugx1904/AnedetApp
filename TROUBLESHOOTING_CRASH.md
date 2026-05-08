# 🐛 Troubleshooting - App Crash Issues

## Issue: App crashes setelah klik "Lewati" di Onboarding

### Root Cause:
**Model TFLite belum di-copy!**

Ketika aplikasi mencoba load model yang tidak ada, akan terjadi `FileNotFoundException` dan aplikasi crash.

---

## ✅ Solution: Copy TFLite Models

### Step 1: Copy Models (WAJIB!)

Jalankan di PowerShell dari root project:

```powershell
# Copy Segmentation Model
Copy-Item "AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite" -Destination "app/src/main/assets/models/segments/best_int8.tflite"

# Copy Classification Model
Copy-Item "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" -Destination "app/src/main/assets/models/classify/best_float32.tflite"
```

### Step 2: Verify Files Exist

Check bahwa file ada di:
- ✅ `app/src/main/assets/models/segments/best_int8.tflite`
- ✅ `app/src/main/assets/models/classify/best_float32.tflite`

### Step 3: Rebuild & Reinstall

```bash
# Clean build
./gradlew clean

# Build dengan models
./gradlew assembleDebug

# Install
./gradlew installDebug
```

---

## 📱 Alternative: Install via Android Studio

1. Copy models (lihat Step 1 di atas)
2. Open Android Studio
3. **Run → Run 'app'** (Shift+F10)
4. Select device
5. Wait for installation

---

## 🔍 How to Check Logcat

Jika masih crash, check logcat:

```bash
# Via ADB
adb logcat | grep -E "AndroidRuntime|FATAL|ModelUtils"

# Atau via Android Studio
# View → Tool Windows → Logcat
```

Look for:
- `FileNotFoundException` - Model tidak ditemukan
- `Model file not found` - Error message dari ModelUtils
- `FATAL EXCEPTION` - Crash details

---

## ⚠️ Common Errors

### 1. FileNotFoundException: models/segments/best_int8.tflite
**Cause:** Segmentation model belum di-copy
**Solution:** Copy `best_int8.tflite` ke `app/src/main/assets/models/segments/`

### 2. FileNotFoundException: models/classify/best_float32.tflite
**Cause:** Classification model belum di-copy
**Solution:** Copy `best_float32.tflite` ke `app/src/main/assets/models/classify/`

### 3. App crashes immediately after onboarding
**Cause:** Models tidak ada, Hilt mencoba inject model saat navigate ke CameraScreen
**Solution:** Copy kedua model, rebuild, reinstall

### 4. "An established connection was aborted"
**Cause:** Koneksi USB/ADB terputus
**Solution:** 
- Reconnect USB cable
- Enable USB Debugging lagi
- Restart ADB: `adb kill-server && adb start-server`
- Install via Android Studio

---

## ✅ Verification Steps

Setelah copy models dan reinstall:

1. ✅ App launches
2. ✅ Onboarding shows (3 pages)
3. ✅ Click "Lewati" or "Lanjutkan" → No crash
4. ✅ Camera screen appears
5. ✅ Permission dialog shows
6. ✅ After granting permission, camera preview works

---

## 📊 Expected Behavior

### With Models:
```
Onboarding → Click "Lewati" → Camera Screen → Permission Request → Camera Preview ✅
```

### Without Models:
```
Onboarding → Click "Lewati" → CRASH ❌
```

**Error in Logcat:**
```
E/ModelUtils: Model file not found: models/segments/best_int8.tflite
E/ModelUtils: Please copy TFLite models to assets folder!
E/AndroidRuntime: FATAL EXCEPTION: main
    java.lang.RuntimeException: Model file not found: models/segments/best_int8.tflite
```

---

## 🔧 Quick Fix Commands

```powershell
# 1. Copy models
Copy-Item "AnedetAI/Conjunctiva Segmentation/Models/best_int8.tflite" -Destination "app/src/main/assets/models/segments/best_int8.tflite"
Copy-Item "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" -Destination "app/src/main/assets/models/classify/best_float32.tflite"

# 2. Rebuild
./gradlew clean assembleDebug

# 3. Reinstall
./gradlew installDebug

# Or via Android Studio: Run → Run 'app'
```

---

## 📝 Notes

1. **Models MUST be copied BEFORE building** - Models are bundled into APK during build
2. **Rebuild required after copying** - Just copying models to existing APK won't work
3. **Both models required** - App needs both segmentation and classification models
4. **Check file sizes:**
   - Segmentation: ~2-5 MB
   - Classification: ~20-50 MB

---

## 🎯 Summary

**Problem:** App crash setelah onboarding
**Root Cause:** Model TFLite tidak ada
**Solution:** Copy models → Rebuild → Reinstall

**Time:** ~5 menit

---

**Setelah fix, aplikasi akan berjalan normal! ✅**
