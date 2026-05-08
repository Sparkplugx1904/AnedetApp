# ⚡ Quick Start Guide

## 🚀 Get Started in 3 Steps

### Step 1: Copy Models (5 minutes) ⚠️ REQUIRED

```powershell
# Windows PowerShell - Run from project root
Copy-Item "AnedetAI/Conjunctiva Segmentation/Conjunctiva Segmentation Model/conjunctiva_segmentation_model_YOLOv11-seg/best_saved_model/best_full_integer_quant.tflite" -Destination "app/src/main/assets/models/segments/best_int8.tflite"

Copy-Item "AnedetAI/Anemia Classify/Models/yolo26m-CLAHE/tflite/best_float32.tflite" -Destination "app/src/main/assets/models/classify/best_float32.tflite"
```

### Step 2: Build (5 minutes)

```bash
# Open in Android Studio
# File → Open → Select AnedetApp folder

# Sync Gradle
# File → Sync Project with Gradle Files

# Build APK
./gradlew assembleDebug
```

### Step 3: Install & Test (10 minutes)

```bash
# Install to device
./gradlew installDebug

# Or via Android Studio
# Run → Run 'app' (Shift+F10)
```

---

## ✅ Verification Checklist

After installation, verify:

- [ ] App launches without crash
- [ ] Onboarding shows (3 pages)
- [ ] Camera preview works
- [ ] Polygon overlay appears when detecting conjunctiva
- [ ] Capture button works
- [ ] Classification result shows both scores
- [ ] Save button works
- [ ] History screen shows saved examinations
- [ ] Settings allows language change
- [ ] Dark mode works

---

## 🐛 Quick Troubleshooting

### Model not found error
```
✅ Solution: Copy models to:
   - app/src/main/assets/models/segments/best_int8.tflite
   - app/src/main/assets/models/classify/best_float32.tflite
```

### Gradle sync failed
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Camera permission denied
```
Settings → Apps → Anemia Detector → Permissions → Enable Camera
```

---

## 📚 Full Documentation

- 📖 [README.md](README.md) - Project overview
- 📖 [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) - Detailed build guide
- 📖 [FINAL_SUMMARY.md](FINAL_SUMMARY.md) - Implementation summary
- 📖 [CLAUDE.md](CLAUDE.md) - Complete specification

---

## 🎯 What's Implemented

✅ **100% Complete:**
- Real-time camera with segmentation
- Single capture mode
- Live inference mode
- Classification with both scores
- Save with mask overlay
- History with filter & sort
- Settings (language, theme)
- Onboarding
- Trilingual (ID/EN/TH)
- Dark mode

---

## 📱 Device Requirements

- Android 11+ (API 30+)
- Camera (rear recommended)
- 2GB RAM minimum
- 100MB free storage

---

**Ready to go! 🚀**
