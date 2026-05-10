# 🧪 QUICK TEST CHECKLIST — AUDIT-02 FIXES
> Panduan cepat untuk verifikasi fix di device fisik

---

## 📱 PERSIAPAN

### 1. Build & Install
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Enable Logcat Filtering
```bash
# Terminal 1: Frame & Segmentation logs
adb logcat -s FrameDebug:D SegDebug:D ConjunctivaSegmentor:D RepoDebug:D ImageProxy:D

# Terminal 2: Errors only
adb logcat *:E
```

---

## ✅ TEST CASE 1: Frame Quality (BUG #1 Fix)

### Objective
Verifikasi frame dari kamera tidak korup (rowStride handled correctly)

### Steps
1. Buka aplikasi
2. Arahkan kamera ke konjungtiva
3. Lihat logcat

### Expected Output
```
D/ImageProxy: Fast path: no padding, rowStride=5120
```
atau
```
D/ImageProxy: Slow path: rowStride=5184, expected=5120, padding=64 bytes/row
```

### ✅ PASS Criteria
- Log muncul setiap frame
- Tidak ada crash
- Tidak ada "recycled bitmap" error

### ❌ FAIL Indicators
- Aplikasi crash saat buka kamera
- Log tidak muncul sama sekali
- Error "Can't call getPixels() on a recycled bitmap"

---

## ✅ TEST CASE 2: Letterbox Parameters (BUG #2 Fix)

### Objective
Verifikasi letterbox parameters dihitung dengan benar untuk 1280×720 → 320×320

### Steps
1. Arahkan kamera ke konjungtiva
2. Tunggu deteksi pertama
3. Lihat logcat

### Expected Output
```
D/ConjunctivaSegmentor: Letterbox params: scale=0.25, xOff=0.0, yOff=70.0
```

### ✅ PASS Criteria
- `scale = 0.25` (320 / 1280)
- `xOff = 0.0` (no horizontal padding)
- `yOff = 70.0` ((320 - 180) / 2)

### ❌ FAIL Indicators
- `scale != 0.25`
- `yOff != 70.0`
- Log tidak muncul

---

## ✅ TEST CASE 3: Detection Confidence (BUG #1 Impact)

### Objective
Verifikasi model dapat mendeteksi konjungtiva dengan confidence > 0.35

### Steps
1. Arahkan kamera ke konjungtiva dengan pencahayaan baik
2. Tunggu 2-3 detik
3. Lihat logcat

### Expected Output
```
D/SegDebug: Detection 0: conf=0.87, x1=0.234, y1=0.156, x2=0.678, y2=0.543, class=0
D/ConjunctivaSegmentor: Detection found: confidence=0.87, polygon points=12
```

### ✅ PASS Criteria
- `conf > 0.35` (threshold)
- `class = 0` (conjunctiva)
- Polygon points > 4

### ❌ FAIL Indicators
```
D/SegDebug: Detection 0: conf=0.0, ...
D/ConjunctivaSegmentor: No detection above threshold
```
atau
```
D/ConjunctivaSegmentor: Stopped at detection 0, confidence 0.0 < 0.35
```

**Jika FAIL**: Gambar masih korup atau model tidak cocok

---

## ✅ TEST CASE 4: Bbox Coordinates (BUG #2 Fix)

### Objective
Verifikasi bbox coordinates dalam range yang benar setelah inverse letterbox

### Steps
1. Arahkan kamera ke konjungtiva
2. Tunggu deteksi
3. Lihat logcat

### Expected Output
```
D/ConjunctivaSegmentor: Bbox after inverse letterbox: RectF(234.5, 112.3, 867.2, 543.8)
```

### ✅ PASS Criteria
- X coordinates: 0 ≤ x ≤ 1280
- Y coordinates: 0 ≤ y ≤ 720
- Width > 100px (reasonable size)
- Height > 50px (reasonable size)

### ❌ FAIL Indicators
- X > 1280 atau X < 0
- Y > 720 atau Y < 0
- Width < 50px (terlalu kecil)
- Bbox di luar layar

---

## ✅ TEST CASE 5: Visual Overlay Position (BUG #2 & #3 Fix)

### Objective
Verifikasi overlay polygon muncul di posisi yang tepat di atas konjungtiva

### Steps
1. Arahkan kamera ke konjungtiva
2. Lihat overlay biru yang muncul
3. Ambil screenshot

### ✅ PASS Criteria
- Overlay muncul di layar
- Overlay tepat di atas area konjungtiva (kelopak mata bawah)
- Tidak ada offset vertikal atau horizontal
- Polygon mengikuti bentuk konjungtiva

### ❌ FAIL Indicators
- Overlay tidak muncul sama sekali
- Overlay muncul di posisi salah (displaced)
- Overlay terlalu tinggi atau terlalu rendah
- Overlay di luar area mata

**Visual Reference**:
```
┌─────────────────┐
│                 │
│      👁️         │  ← Mata
│    ┌─────┐      │
│    │█████│      │  ← Overlay HARUS di sini (konjungtiva bawah)
│    └─────┘      │
│                 │
└─────────────────┘
```

---

## ✅ TEST CASE 6: Capture & Classification (Full Pipeline)

### Objective
Verifikasi full pipeline berjalan dengan benar (segmentation + classification)

### Steps
1. Arahkan kamera ke konjungtiva
2. Tunggu overlay muncul
3. Tekan tombol capture (kamera putih besar)
4. Tunggu hasil

### Expected Behavior
1. Overlay biru muncul (segmentation)
2. Processing indicator muncul
3. Result sheet muncul dengan:
   - Gambar dengan overlay
   - Label: "Anemia" atau "Non Anemia"
   - Confidence: XX%

### ✅ PASS Criteria
- Result sheet muncul dalam < 3 detik
- Label dan confidence ditampilkan
- Gambar dengan overlay benar
- Tidak ada crash

### ❌ FAIL Indicators
- "Konjungtiva tidak terdeteksi" meskipun overlay muncul
- "Area konjungtiva terlalu kecil"
- Crash saat capture
- Result sheet tidak muncul

---

## ✅ TEST CASE 7: Live Inference Mode

### Objective
Verifikasi live inference (full pipeline setiap 1 detik) berjalan lancar

### Steps
1. Arahkan kamera ke konjungtiva
2. Tekan tombol lightning (⚡) di bawah
3. Konfirmasi warning dialog
4. Tunggu 5 detik

### Expected Behavior
1. Overlay muncul dengan warna hijau/merah (sesuai hasil)
2. Label chip muncul di atas: "Anemia 87%" atau "Non Anemia 92%"
3. Update setiap ~1 detik
4. Tidak ada lag atau freeze

### ✅ PASS Criteria
- Overlay update smooth
- Label update setiap 1 detik
- Tidak ada freeze
- FPS tetap stabil

### ❌ FAIL Indicators
- Aplikasi freeze
- FPS drop drastis
- Overlay tidak update
- Crash setelah beberapa detik

---

## 🐛 TROUBLESHOOTING

### Issue: "No detection above threshold"

**Possible Causes**:
1. Frame masih korup (BUG #1 belum fix)
2. Pencahayaan terlalu gelap
3. Model threshold terlalu tinggi

**Debug Steps**:
```bash
# Check frame quality
adb logcat -s ImageProxy:D

# Check detection confidence
adb logcat -s SegDebug:D

# If conf=0.0 for all detections → frame korup
# If conf=0.2-0.3 → turunkan threshold ke 0.25
```

**Temporary Fix**:
```kotlin
// Di ConjunctivaSegmentor.kt
const val CONF_THRESHOLD = 0.25f  // turunkan dari 0.35
```

---

### Issue: Overlay muncul di posisi salah

**Possible Causes**:
1. Inverse letterbox belum benar (BUG #2)
2. Letterbox parameters salah

**Debug Steps**:
```bash
# Check letterbox params
adb logcat -s ConjunctivaSegmentor:D | grep "Letterbox params"

# Expected: scale=0.25, xOff=0.0, yOff=70.0
# If different → check calculateLetterboxParams()
```

---

### Issue: Crash saat capture

**Possible Causes**:
1. Bitmap recycled prematurely
2. Crop coordinates out of bounds

**Debug Steps**:
```bash
# Check crash log
adb logcat *:E

# Look for:
# - "Can't call getPixels() on a recycled bitmap"
# - "IllegalArgumentException: x + width must be <= bitmap.width()"
```

---

## 📊 SUCCESS METRICS

### Minimum Requirements
- ✅ Frame quality log muncul
- ✅ Detection confidence > 0.35
- ✅ Overlay muncul di posisi benar
- ✅ Capture berhasil tanpa crash

### Optimal Performance
- ✅ Detection confidence > 0.70
- ✅ Overlay update smooth (10 FPS)
- ✅ Live inference tanpa lag
- ✅ Classification accuracy > 85%

---

## 📝 REPORTING

### If ALL TESTS PASS ✅
```
✅ AUDIT-02 FIXES VERIFIED
- Frame quality: OK
- Letterbox params: OK
- Detection: OK (conf=0.XX)
- Bbox coordinates: OK
- Overlay position: OK
- Capture: OK
- Live inference: OK

Device: V2029 Android 12
Build: app-debug.apk (2026-05-11)
```

### If ANY TEST FAILS ❌
```
❌ AUDIT-02 FIXES FAILED
Failed Test: [Test Case Number]
Error: [Error message from logcat]
Expected: [Expected behavior]
Actual: [Actual behavior]

Device: V2029 Android 12
Build: app-debug.apk (2026-05-11)

Logcat:
[Paste relevant logcat lines]
```

---

**Last Updated**: 2026-05-11
**Build**: app-debug.apk
**Target Device**: V2029 Android 12
