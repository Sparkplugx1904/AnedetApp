# 🧪 PANDUAN TESTING ANEDETAPP

> Quick reference untuk testing semua perbaikan bug FATAL & KRITIS

---

## 🎯 TESTING PRIORITY

### Priority 1: CRITICAL FUNCTIONALITY ⚠️
1. Camera capture & frame processing
2. Save to gallery & database
3. Memory leak verification

### Priority 2: CORE FEATURES 🔧
4. Mask decoding & polygon overlay
5. Live inference lifecycle
6. Crop accuracy

### Priority 3: UI/UX 🎨
7. Camera flip
8. Settings button
9. Result preview

---

## 📋 DETAILED TEST CASES

### 1. ✅ FATAL-1: ImageProxy.toBitmap()

**Test:** Camera Frame Processing
```
Steps:
1. Launch app
2. Grant camera permission
3. Observe camera preview

Expected:
✅ Camera preview tampil dengan benar
✅ Polygon overlay muncul di konjungtiva
✅ No crash, no black screen

Actual Result: [ ]
```

**Verification:**
- Check logcat untuk "Detection found: confidence=..."
- Jika tidak ada log → frame tidak masuk inference (BUG!)

---

### 2. ✅ FATAL-2: Mask Decoding

**Test:** Polygon Shape
```
Steps:
1. Launch app
2. Arahkan kamera ke konjungtiva
3. Tunggu polygon muncul
4. Observe bentuk polygon

Expected:
✅ Polygon mengikuti bentuk konjungtiva (bukan rectangle)
✅ Polygon memiliki 6-15 vertex points
✅ Vertex dots terlihat di edge

Actual Result: [ ]
```

**Verification:**
- Check logcat untuk "polygon_pts=X" (X should be 6-15)
- Jika X=4 → masih rectangle (proto masks tidak tersedia atau decoding gagal)
- Check logcat untuk "Proto masks shape: ..." atau "No proto masks available"

**Fallback Behavior:**
- Jika model tidak support proto masks → akan fallback ke rectangle (EXPECTED)
- Check log: "No proto masks available, using bbox rectangle"

---

### 3. ✅ FATAL-4: Camera Flip

**Test:** Flip Camera Button
```
Steps:
1. Launch app
2. Tap flip camera button (icon flip)
3. Observe camera switch

Expected:
✅ Camera beralih dari back ke front (atau sebaliknya)
✅ Preview update dengan kamera baru
✅ Segmentation tetap berjalan

Actual Result: [ ]
```

**Verification:**
- Camera harus benar-benar switch
- Jika tidak switch → bug masih ada (check key() implementation)

---

### 4. ✅ KRITIS-1: Save Functionality

**Test A:** Save to Gallery
```
Steps:
1. Launch app
2. Capture image (tap camera button)
3. Wait for result sheet
4. Tap "Simpan" button
5. Check snackbar message
6. Open Gallery app
7. Navigate to Pictures/AnemiaDetector

Expected:
✅ Snackbar: "Tersimpan di Galeri"
✅ File exists di Pictures/AnemiaDetector
✅ File format: anemia_YYYYMMDD_HHMMSS.jpg
✅ Image shows polygon overlay

Actual Result: [ ]
```

**Test B:** Save to Database
```
Steps:
1. After saving (Test A)
2. Navigate to History screen
3. Check for saved examination

Expected:
✅ New record appears in history
✅ Timestamp correct
✅ Result label correct (ANEMIA/NON_ANEMIA)
✅ Confidence score displayed

Actual Result: [ ]
```

**Verification:**
- Check logcat untuk "Examination saved successfully: ..."
- Jika error → check logcat untuk exception details
- Verify file exists: `adb shell ls /sdcard/Pictures/AnemiaDetector/`

---

### 5. ✅ KRITIS-2: Result Preview

**Test:** Result Sheet Image
```
Steps:
1. Launch app
2. Capture image
3. Wait for result sheet
4. Observe preview image

Expected:
✅ Image tampil (not empty/gray box)
✅ Polygon overlay visible
✅ Color: Red (anemia) or Green (non-anemia)
✅ Alpha transparency ~30%

Actual Result: [ ]
```

**Verification:**
- Check logcat untuk "generateMaskedBitmap" calls
- Jika image kosong → resultBitmap masih null (BUG!)

---

### 6. ✅ KRITIS-3: Crop Accuracy

**Test:** Classification Accuracy
```
Steps:
1. Launch app
2. Arahkan kamera ke konjungtiva yang jelas terlihat
3. Capture image
4. Check classification result

Expected:
✅ Classification result reasonable (not random)
✅ Confidence score > 0.5 untuk predicted class
✅ No "Area terlalu kecil" error

Actual Result: [ ]
```

**Verification:**
- Check logcat untuk crop coordinates
- Verify yOffset applied: "yOffset = 49px"
- Jika confidence selalu ~0.5 → crop mungkin salah (classify padding hitam)

**Debug:**
```kotlin
// Add log di cropConjunctiva():
Log.d("Crop", "scaledBbox: $scaledBbox, yOffset: $yOffset")
```

---

### 7. ✅ KRITIS-4: Memory Leak

**Test:** Long-running Live Inference
```
Steps:
1. Launch app
2. Enable live inference (tap bolt icon)
3. Confirm warning dialog
4. Let it run for 5 minutes
5. Monitor memory usage

Expected:
✅ Memory usage stable (~200-300MB)
✅ No gradual increase
✅ No crash/OOM
✅ App responsive

Actual Result: [ ]
```

**Verification:**
- Use Android Studio Profiler → Memory tab
- Watch for gradual increase (leak indicator)
- Check logcat untuk "OutOfMemoryError"

**Tools:**
```bash
# Monitor memory via adb
adb shell dumpsys meminfo com.example.anemiadetector

# Watch continuously
watch -n 1 'adb shell dumpsys meminfo com.example.anemiadetector | grep TOTAL'
```

---

### 8. ✅ KRITIS-5: Background Inference Stop

**Test:** App Lifecycle
```
Steps:
1. Launch app
2. Enable live inference
3. Confirm warning
4. Wait for inference running (status chip updates)
5. Press HOME button (app goes to background)
6. Wait 10 seconds
7. Return to app

Expected:
✅ Live inference STOPPED when app backgrounded
✅ Status chip no longer updating
✅ Bolt icon no longer highlighted
✅ Can re-enable manually

Actual Result: [ ]
```

**Verification:**
- Check logcat untuk "App stopped, pausing live inference"
- Monitor CPU usage saat di background (should drop to ~0%)
- Jika CPU masih tinggi → inference masih jalan (BUG!)

**Tools:**
```bash
# Monitor CPU usage
adb shell top | grep anemiadetector
```

---

### 9. ✅ KRITIS-6: Settings Button

**Test:** Settings Navigation
```
Steps:
1. Launch app
2. Look for settings button (gear icon, top-right)
3. Tap settings button
4. Observe navigation

Expected:
✅ Settings button visible
✅ Tap navigates to Settings screen
✅ Can navigate back to camera

Actual Result: [ ]
```

**Verification:**
- Settings button should be in top-right corner of BottomActionBar
- Icon: gear/cog (Icons.Default.Settings)

---

## 🔍 DEBUGGING TIPS

### Enable Verbose Logging:
```kotlin
// Add to CameraViewModel
companion object {
    private const val TAG = "CameraViewModel"
    private const val DEBUG = true
}

// Add logs
if (DEBUG) Log.d(TAG, "...")
```

### Check Logcat Filters:
```bash
# Filter by tag
adb logcat -s CameraViewModel:D ConjunctivaSegmentor:D

# Filter by package
adb logcat | grep com.example.anemiadetector

# Clear and watch
adb logcat -c && adb logcat
```

### Common Issues:

#### Issue: Polygon selalu rectangle
**Cause:** Model tidak support proto masks  
**Solution:** Expected behavior, fallback works correctly  
**Verify:** Check log "No proto masks available"

#### Issue: Save gagal
**Cause:** Permission storage tidak granted  
**Solution:** Check AndroidManifest.xml permissions  
**Verify:** `adb shell pm list permissions -g`

#### Issue: Memory leak masih ada
**Cause:** OpenCV Mat tidak di-release  
**Solution:** Check semua `.release()` calls  
**Verify:** Use LeakCanary library

#### Issue: Crop salah
**Cause:** Letterbox offset tidak applied  
**Solution:** Verify yOffset calculation  
**Verify:** Log scaledBbox coordinates

---

## 📊 TEST REPORT TEMPLATE

```markdown
# Test Report - AnedetApp Bug Fixes

**Tester:** [Name]
**Date:** [Date]
**Build:** [Commit Hash]
**Device:** [Device Model + Android Version]

## Test Results

| Test Case | Status | Notes |
|-----------|--------|-------|
| FATAL-1: Frame Processing | ✅/❌ | |
| FATAL-2: Mask Decoding | ✅/❌ | |
| FATAL-4: Camera Flip | ✅/❌ | |
| KRITIS-1: Save Gallery | ✅/❌ | |
| KRITIS-1: Save Database | ✅/❌ | |
| KRITIS-2: Result Preview | ✅/❌ | |
| KRITIS-3: Crop Accuracy | ✅/❌ | |
| KRITIS-4: Memory Leak | ✅/❌ | |
| KRITIS-5: Background Stop | ✅/❌ | |
| KRITIS-6: Settings Button | ✅/❌ | |

## Issues Found

1. [Issue description]
   - Steps to reproduce
   - Expected vs Actual
   - Logcat output

## Overall Assessment

- [ ] All critical tests passed
- [ ] Ready for production
- [ ] Needs fixes

**Recommendation:** [Deploy / Fix issues / More testing]
```

---

## 🚀 QUICK START

### Minimal Testing (5 minutes):
1. ✅ Camera preview works
2. ✅ Capture & save works
3. ✅ History shows saved item
4. ✅ Camera flip works
5. ✅ Settings button works

### Full Testing (30 minutes):
- Run all test cases above
- Monitor memory for 5 minutes
- Test background lifecycle
- Verify polygon shape
- Check crop accuracy

### Stress Testing (1 hour):
- Live inference for 30 minutes
- Capture 50+ images
- Fill history with 100+ records
- Test on low-end device
- Test on different Android versions

---

## 📞 REPORTING BUGS

If you find bugs:

1. **Capture logcat:**
   ```bash
   adb logcat > bug_report.txt
   ```

2. **Include:**
   - Device model & Android version
   - Steps to reproduce
   - Expected vs actual behavior
   - Logcat output
   - Screenshots/video

3. **Priority:**
   - 🔴 Critical: App crash, data loss
   - 🟠 High: Feature not working
   - 🟡 Medium: UI issue, performance
   - 🟢 Low: Minor cosmetic issue

---

**Happy Testing! 🧪**
