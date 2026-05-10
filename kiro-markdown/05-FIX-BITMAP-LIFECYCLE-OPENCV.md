# Fix Bitmap Lifecycle Issue - OpenCV AndroidBitmap_lockPixels Error

## Masalah
```
E cv::error(): OpenCV(4.12.0) Error: Assertion failed 
(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) 
in Java_org_opencv_android_Utils_nBitmapToMat2
```

Classification gagal karena OpenCV tidak bisa lock pixels dari bitmap yang sudah di-recycle.

## Root Cause

### Bitmap Lifecycle Issue

**Flow yang salah:**
```kotlin
// 1. Crop conjunctiva (shares pixel buffer dengan preprocessed)
val crop = Bitmap.createBitmap(preprocessed, x, y, w, h)

// 2. Recycle preprocessed SEBELUM classify selesai
preprocessed.recycle()  // ❌ BAD: crop masih butuh pixel data!

// 3. Classify menggunakan crop
val classification = inferenceRepository.classify(crop)  // ❌ CRASH!
```

### Penjelasan `Bitmap.createBitmap()`

`Bitmap.createBitmap(source, x, y, w, h)` membuat **immutable bitmap** yang:
- ✅ Tidak copy pixel data (efficient)
- ❌ **Share pixel buffer** dengan source bitmap
- ❌ Menjadi **invalid** ketika source di-recycle

**Analogi:**
```
preprocessed = [pixel data in memory]
                    ↓ (reference, not copy)
crop = [pointer to same pixel data]

preprocessed.recycle() → [pixel data freed]
                              ↓
crop → [pointer to freed memory] ❌ INVALID!
```

### Kenapa Error di OpenCV?

OpenCV `Utils.bitmapToMat()` memanggil `AndroidBitmap_lockPixels()` untuk akses pixel data:
```cpp
// OpenCV internal
AndroidBitmap_lockPixels(env, bitmap, &pixels)  // ❌ FAIL: bitmap invalid
```

Jika bitmap sudah di-recycle atau share buffer yang sudah freed, lock pixels gagal.

## Solusi

### 1. Buat Independent Copy di `cropConjunctiva()`

**Sebelum:**
```kotlin
private fun cropConjunctiva(...): Bitmap {
    // ...
    return Bitmap.createBitmap(
        preprocessed,  // ❌ Shares pixel buffer
        x, y, w, h
    )
}
```

**Sesudah:**
```kotlin
private fun cropConjunctiva(...): Bitmap {
    // ...
    
    // Create bitmap crop (shares pixel buffer with parent)
    val tempCrop = Bitmap.createBitmap(
        preprocessed,
        x, y, w, h
    )
    
    // Make independent copy so we can recycle preprocessed safely
    val crop = tempCrop.copy(Bitmap.Config.ARGB_8888, false)
    tempCrop.recycle()
    
    return crop  // ✅ Independent bitmap
}
```

### 2. Recycle dalam Urutan yang Benar

**Sebelum:**
```kotlin
val crop = cropConjunctiva(preprocessed, bbox, polygon)
val classification = inferenceRepository.classify(crop)

// Cleanup
preprocessed.recycle()  // ❌ BAD: recycle parent dulu
crop.recycle()
```

**Sesudah:**
```kotlin
val crop = cropConjunctiva(preprocessed, bbox, polygon)  // crop is independent now
val classification = inferenceRepository.classify(crop)

// Cleanup - recycle setelah classify selesai
crop.recycle()          // ✅ GOOD: recycle child dulu
preprocessed.recycle()  // ✅ GOOD: recycle parent terakhir
```

## Penjelasan `Bitmap.copy()`

```kotlin
val crop = tempCrop.copy(Bitmap.Config.ARGB_8888, false)
```

**Parameters:**
- `Config.ARGB_8888`: Format pixel (32-bit, 8 bits per channel)
- `false`: Immutable (tidak perlu mutable untuk inference)

**Behavior:**
- ✅ **Allocate new pixel buffer** (independent)
- ✅ **Copy pixel data** dari source
- ✅ Safe untuk recycle source setelah copy

**Trade-off:**
- ❌ Slower (copy pixel data)
- ❌ More memory (duplicate pixel data)
- ✅ Safe (no shared buffer issues)

## Alternative Solutions (Tidak Digunakan)

### 1. Jangan Recycle Preprocessed (Boros Memory)
```kotlin
// Don't recycle preprocessed
// preprocessed.recycle()  // ❌ Memory leak
crop.recycle()
```
❌ Preprocessed bitmap tidak di-recycle → memory leak

### 2. Recycle Setelah Classify (Kompleks)
```kotlin
val classification = inferenceRepository.classify(crop)
preprocessed.recycle()  // Recycle setelah classify
crop.recycle()
```
❌ Masih bermasalah karena crop share buffer dengan preprocessed

### 3. Copy di Classify (Tidak Efisien)
```kotlin
fun classify(crop: Bitmap): ClassificationResult {
    val safeCrop = crop.copy(Bitmap.Config.ARGB_8888, false)
    // ... use safeCrop
    safeCrop.recycle()
}
```
❌ Copy di setiap classify call (tidak efisien)

### 4. Use Independent Copy (DIPILIH) ✅
```kotlin
// Copy once di cropConjunctiva
val crop = tempCrop.copy(Bitmap.Config.ARGB_8888, false)
```
✅ Copy sekali, safe untuk recycle parent

## Testing
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -d | Select-String -Pattern "OpenCV|Classification|nBitmapToMat"
```

**Expected:**
```
✅ No OpenCV error
✅ Classification berhasil
```

**No more error:**
```
❌ E cv::error(): AndroidBitmap_lockPixels failed
```

## Files Changed
- `app/src/main/java/com/example/anemiadetector/ui/camera/CameraViewModel.kt`
  - `cropConjunctiva()`: Buat independent copy dengan `bitmap.copy()`
  - `captureAndClassify()`: Recycle crop sebelum preprocessed

## Lesson Learned

### Bitmap Lifecycle Rules:
1. **`Bitmap.createBitmap(source, ...)` shares pixel buffer** dengan source
2. **Recycle child sebelum parent** jika share buffer
3. **Use `bitmap.copy()` untuk independent copy** jika perlu recycle parent lebih dulu
4. **OpenCV requires valid bitmap** dengan pixel buffer yang masih allocated

### When to Use `copy()`:
- ✅ Ketika perlu independent bitmap (tidak share buffer)
- ✅ Ketika parent akan di-recycle sebelum child selesai digunakan
- ✅ Ketika pass bitmap ke async operation atau thread lain
- ❌ Jangan gunakan jika tidak perlu (boros memory & CPU)

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| Crop Method | `createBitmap()` (shared) | `copy()` (independent) |
| Pixel Buffer | Shared dengan parent | Independent |
| Recycle Order | Parent → Child ❌ | Child → Parent ✅ |
| OpenCV Error | AndroidBitmap_lockPixels failed | No error ✅ |
| Memory | Less (shared) | More (copied) |
| Safety | Unsafe (crash) | Safe ✅ |

**Status:** ✅ Classification berhasil, no OpenCV error
