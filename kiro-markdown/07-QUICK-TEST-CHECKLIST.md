# ✅ QUICK TEST CHECKLIST - AnedetApp

> **Device:** V2029 - Android 12  
> **Build:** Debug (Round 2)  
> **Date:** 10 Mei 2026

---

## 🎯 TESTING INSTRUCTIONS

### Setup:
1. ✅ App installed: `./gradlew installDebug`
2. ✅ App launched: `adb shell am start -n com.example.anemiadetector/.MainActivity`
3. ⏳ Monitor logs: `./monitor-app.ps1` (PowerShell) atau `./monitor-app.sh` (Bash)

---

## 📋 ROUND 2 FIXES - PRIORITY TESTING

### 🔴 FATAL-NEW-1: Proto Mask Format [1,H,W,32]

**Test:** Capture image dan observe polygon shape

**Steps:**
1. Grant camera permission
2. Arahkan kamera ke konjungtiva (atau objek apapun untuk testing)
3. Tunggu polygon overlay muncul
4. Observe bentuk polygon

**Expected:**
- [ ] Polygon muncul (bukan kotak saja)
- [ ] Polygon mengikuti bentuk objek (bukan diagonal line)
- [ ] Log: `Proto masks: 32x160x160, format=CHANNELS_FIRST` atau `CHANNELS_LAST`
- [ ] Log: `Detection found: confidence=X.XX, polygon points=Y` (Y should be 6-15)

**Actual:**
```
[Write observations here]
```

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### 🔴 FATAL-NEW-2: Contour Extraction (OpenCV)

**Test:** Verify polygon bukan diagonal line

**Steps:**
1. Capture image (tap camera button)
2. Check logcat untuk contour extraction

**Expected:**
- [ ] Log: `Extracted contour with X points`
- [ ] Polygon terlihat smooth dan terhubung
- [ ] Bukan garis diagonal dari kiri-atas ke kanan-bawah

**Actual:**
```
[Write observations here]
```

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### 🟠 KRITIS-NEW-1: History Filter (Label Case)

**Test:** Filter tabs di History screen

**Steps:**
1. Capture & save 2-3 images
2. Navigate to History screen (tap history button)
3. Tap filter tabs: All, Anemia, Non-Anemia

**Expected:**
- [ ] "All" tab shows all saved items
- [ ] "Anemia" tab shows only anemia results (if any)
- [ ] "Non-Anemia" tab shows only non-anemia results (if any)
- [ ] No tab is always empty (unless no data for that category)

**Actual:**
```
All tab: X items
Anemia tab: Y items
Non-Anemia tab: Z items
```

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### 🟠 KRITIS-NEW-2: Live Inference Result Preview

**Test:** resultBitmap updates correctly

**Steps:**
1. Enable live inference (tap bolt icon)
2. Confirm warning dialog
3. Wait for classification (status chip updates)
4. Disable live inference
5. Tap capture button
6. Check result sheet image

**Expected:**
- [ ] Result sheet shows image from capture, NOT from live inference
- [ ] Image has polygon overlay
- [ ] Image is clear and correct

**Actual:**
```
[Write observations here]
```

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

## 📋 ROUND 1 FIXES - VERIFICATION

### ✅ Camera Preview & Frame Processing

**Test:** Basic camera functionality

**Expected:**
- [ ] Camera preview tampil
- [ ] Polygon overlay muncul
- [ ] No black screen
- [ ] No crash

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### ✅ Camera Flip

**Test:** Flip camera button

**Steps:**
1. Tap flip camera button (icon flip)

**Expected:**
- [ ] Camera switches (back ↔ front)
- [ ] Preview updates
- [ ] Segmentation continues

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### ✅ Save Functionality

**Test A:** Save to Gallery

**Steps:**
1. Capture image
2. Tap "Simpan" button
3. Check snackbar
4. Open Gallery → Pictures/AnemiaDetector

**Expected:**
- [ ] Snackbar: "Tersimpan di Galeri"
- [ ] File exists in gallery
- [ ] Image shows polygon overlay

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

**Test B:** Save to Database

**Steps:**
1. After saving
2. Navigate to History
3. Check for saved record

**Expected:**
- [ ] New record appears
- [ ] Timestamp correct
- [ ] Result label correct
- [ ] Confidence displayed

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### ✅ Result Preview

**Test:** Result sheet image

**Steps:**
1. Capture image
2. Check result sheet

**Expected:**
- [ ] Image tampil (not empty)
- [ ] Polygon overlay visible
- [ ] Color correct (red/green)

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### ✅ Memory Leak

**Test:** Long-running live inference

**Steps:**
1. Enable live inference
2. Let run for 5 minutes
3. Monitor memory usage

**Expected:**
- [ ] Memory stable (~200-300MB)
- [ ] No gradual increase
- [ ] No crash/OOM
- [ ] App responsive

**Command:**
```bash
# Monitor memory
adb shell dumpsys meminfo com.example.anemiadetector | grep TOTAL
```

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### ✅ Background Lifecycle

**Test:** App lifecycle handling

**Steps:**
1. Enable live inference
2. Press HOME button
3. Wait 10 seconds
4. Return to app

**Expected:**
- [ ] Live inference STOPPED when backgrounded
- [ ] Status chip no longer updating
- [ ] Bolt icon not highlighted
- [ ] Can re-enable manually

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

### ✅ Settings Button

**Test:** Settings navigation

**Steps:**
1. Look for settings button (top-right)
2. Tap settings button

**Expected:**
- [ ] Settings button visible
- [ ] Navigates to Settings screen
- [ ] Can navigate back

**Status:** ⏳ PENDING / ✅ PASS / ❌ FAIL

---

## 🐛 BUGS FOUND

### Bug #1:
**Description:**
```
[Describe bug here]
```

**Steps to Reproduce:**
```
1. 
2. 
3. 
```

**Expected vs Actual:**
```
Expected: 
Actual: 
```

**Logcat:**
```
[Paste relevant logcat here]
```

**Priority:** 🔴 Critical / 🟠 High / 🟡 Medium / 🟢 Low

---

## 📊 TEST SUMMARY

| Category | Total | Pass | Fail | Pending |
|----------|-------|------|------|---------|
| Round 2 Fixes | 4 | 0 | 0 | 4 |
| Round 1 Fixes | 8 | 0 | 0 | 8 |
| **TOTAL** | **12** | **0** | **0** | **12** |

**Pass Rate:** 0% (0/12)

---

## ✅ SIGN-OFF

**Tester:** [Name]  
**Date:** [Date]  
**Device:** V2029 - Android 12  
**Build:** Debug (Round 2)  

**Overall Assessment:**
- [ ] All critical tests passed
- [ ] Ready for production
- [ ] Needs fixes

**Recommendation:**
```
[Deploy / Fix issues / More testing]
```

---

## 📝 NOTES

```
[Additional notes, observations, or comments]
```

---

**Generated by:** Kiro AI Assistant  
**Template Version:** 1.0
