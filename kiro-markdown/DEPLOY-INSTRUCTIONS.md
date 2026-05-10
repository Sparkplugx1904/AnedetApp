# 🚀 DEPLOYMENT INSTRUCTIONS — AUDIT-02 FIXES
> Quick guide untuk deploy dan test fix di device V2029 Android 12

---

## 📦 BUILD APK

```bash
cd C:\Users\Ananda\Documents\GitHub\AnedetApp
./gradlew clean assembleDebug --no-daemon
```

**Expected Output**:
```
BUILD SUCCESSFUL in 1m 48s
```

**APK Location**:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 INSTALL TO DEVICE

### 1. Connect Device
```bash
adb devices
```

**Expected**:
```
List of devices attached
V2029   device
```

### 2. Install APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Expected**:
```
Performing Streamed Install
Success
```

---

## 🔍 ENABLE LOGGING

### Terminal 1: Main Logs
```bash
adb logcat -c
adb logcat -s FrameDebug:D SegDebug:D ConjunctivaSegmentor:D RepoDebug:D ImageProxy:D
```

### Terminal 2: Error Logs
```bash
adb logcat *:E
```

---

## ✅ TEST SCENARIOS

### Test 1: Frame Quality
**Action**: Buka aplikasi
**Expected Log**:
```
D/ImageProxy: Slow path: rowStride=5184, expected=5120, padding=64 bytes/row
```
**Pass Criteria**: Log muncul, tidak ada crash

---

### Test 2: Letterbox Parameters
**Action**: Arahkan kamera ke konjungtiva
**Expected Log**:
```
D/ConjunctivaSegmentor: Letterbox params: scale=0.25, xOff=0.0, yOff=70.0
```
**Pass Criteria**: 
- scale = 0.25
- xOff = 0.0
- yOff = 70.0

---

### Test 3: Detection Success
**Action**: Tunggu 2-3 detik
**Expected Log**:
```
D/SegDebug: Detection 0: conf=0.87, x1=0.234, y1=0.156, x2=0.678, y2=0.543, class=0
D/ConjunctivaSegmentor: Bbox after inverse letterbox: RectF(234.5, 112.3, 867.2, 543.8)
D/ConjunctivaSegmentor: Detection found: confidence=0.87, polygon points=12
D/RepoDebug: Segmentation success: bbox=RectF(...), conf=0.87
```
**Pass Criteria**:
- conf > 0.35
- Bbox X: 0-1280
- Bbox Y: 0-720
- Polygon points > 4

---

### Test 4: Overlay Position
**Action**: Lihat layar device
**Expected**: Overlay biru muncul tepat di atas konjungtiva (kelopak mata bawah)
**Pass Criteria**: 
- ✅ Overlay muncul
- ✅ Posisi tepat (tidak displaced)
- ✅ Mengikuti bentuk konjungtiva

**Visual Reference**:
```
┌─────────────────┐
│                 │
│      👁️         │  ← Mata
│    ┌─────┐      │
│    │█████│      │  ← Overlay HARUS di sini
│    └─────┘      │
│                 │
└─────────────────┘
```

---

### Test 5: Capture & Classification
**Action**: Tekan tombol capture (kamera putih besar)
**Expected**: 
1. Processing indicator muncul
2. Result sheet muncul dalam < 3 detik
3. Gambar dengan overlay ditampilkan
4. Label: "Anemia" atau "Non Anemia"
5. Confidence: XX%

**Pass Criteria**: Semua step berhasil tanpa crash

---

### Test 6: Live Inference
**Action**: 
1. Tekan tombol lightning (⚡)
2. Konfirmasi warning dialog
3. Tunggu 5 detik

**Expected**:
1. Overlay berubah warna (hijau/merah)
2. Label chip muncul di atas: "Anemia 87%" atau "Non Anemia 92%"
3. Update setiap ~1 detik
4. Tidak ada lag

**Pass Criteria**: Update smooth, tidak freeze

---

## 🐛 TROUBLESHOOTING

### Issue: "No detection above threshold"

**Check Log**:
```bash
adb logcat -s SegDebug:D | grep "Detection 0"
```

**If `conf=0.0`**:
- Frame masih korup → Check ImageProxy log
- Model tidak cocok → Verify model file

**If `conf=0.2-0.3`**:
- Threshold terlalu tinggi → Lower to 0.25
- Pencahayaan kurang → Improve lighting

**Temporary Fix**:
```kotlin
// Di ConjunctivaSegmentor.kt line 27
const val CONF_THRESHOLD = 0.25f  // turunkan dari 0.35
```

---

### Issue: Overlay di posisi salah

**Check Log**:
```bash
adb logcat -s ConjunctivaSegmentor:D | grep "Letterbox params"
```

**Expected**:
```
scale=0.25, xOff=0.0, yOff=70.0
```

**If different**:
- Verify `calculateLetterboxParams()` implementation
- Check `modelToFrame()` logic

---

### Issue: Crash saat capture

**Check Error Log**:
```bash
adb logcat *:E
```

**Common Errors**:
1. `Can't call getPixels() on a recycled bitmap`
   - Bitmap recycled too early
   - Check bitmap lifecycle in CameraViewModel

2. `IllegalArgumentException: x + width must be <= bitmap.width()`
   - Crop coordinates out of bounds
   - Check `cropConjunctiva()` bounds checking

---

## 📊 SUCCESS REPORT TEMPLATE

### If ALL TESTS PASS ✅

```
✅ AUDIT-02 FIXES VERIFIED

Device: V2029 Android 12
Build: app-debug.apk (2026-05-11)
Test Date: [DATE]

Results:
✅ Frame quality: OK (rowStride handled)
✅ Letterbox params: OK (scale=0.25, yOff=70.0)
✅ Detection: OK (conf=0.XX)
✅ Bbox coordinates: OK (within bounds)
✅ Overlay position: OK (visual verification)
✅ Capture: OK (no crash)
✅ Live inference: OK (smooth update)

Notes:
- Average confidence: 0.XX
- Average latency: XXXms
- No crashes observed
- Overlay position accurate

Status: READY FOR PRODUCTION
```

---

### If ANY TEST FAILS ❌

```
❌ AUDIT-02 FIXES FAILED

Device: V2029 Android 12
Build: app-debug.apk (2026-05-11)
Test Date: [DATE]

Failed Test: [Test Number & Name]

Error:
[Error message from logcat]

Expected:
[Expected behavior]

Actual:
[Actual behavior]

Logcat:
[Paste relevant logcat lines]

Screenshots:
[Attach screenshots if applicable]

Status: NEEDS INVESTIGATION
```

---

## 📞 SUPPORT

### Documentation
- `AUDIT-FIX-02.md` - Root cause analysis
- `AUDIT-FIX-02-IMPLEMENTATION.md` - Implementation details
- `QUICK-TEST-AUDIT-02-FIXES.md` - Detailed test guide
- `FINAL-SUMMARY-AUDIT-02.md` - Complete summary

### Logcat Commands
```bash
# Clear logs
adb logcat -c

# Main logs
adb logcat -s FrameDebug:D SegDebug:D ConjunctivaSegmentor:D RepoDebug:D ImageProxy:D

# Errors only
adb logcat *:E

# Save to file
adb logcat -d > logcat.txt

# Filter specific tag
adb logcat -s ConjunctivaSegmentor:D

# Grep pattern
adb logcat | grep "Detection"
```

### Device Commands
```bash
# Check device
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uninstall app
adb uninstall com.example.anemiadetector

# Clear app data
adb shell pm clear com.example.anemiadetector

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4
```

---

## 🎯 QUICK CHECKLIST

Before testing:
- [ ] Build successful
- [ ] APK installed
- [ ] Logcat running
- [ ] Device connected

During testing:
- [ ] Frame quality log appears
- [ ] Letterbox params correct
- [ ] Detection confidence > 0.35
- [ ] Bbox coordinates valid
- [ ] Overlay position correct
- [ ] Capture works
- [ ] Live inference smooth

After testing:
- [ ] Save logcat output
- [ ] Take screenshots
- [ ] Document any issues
- [ ] Fill success/failure report

---

**Last Updated**: 2026-05-11
**Version**: 1.0
**Status**: Ready for deployment
