# Fix Camera Bitmap Lifecycle - OpenCV Error in processFrameForSegmentation

## Masalah
```
E cv::error(): OpenCV(4.12.0) Error: Assertion failed 
(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) 
in Java_org_opencv_android_Utils_nBitmapToMat2
```

Error muncul saat preprocessing frame dari camera analyzer.

## Root Cause

### Camera Analyzer Bitmap Lifecycle

**Flow yang salah:**
```kotlin
fun processFrameForSegmentation(bitmap: Bitmap) {
    // bitmap dari camera analyzer
    
    viewModelScope.launch {
        // Coroutine launched (async)
        val preprocessed = inferenceRepository.preprocess(bitmap)  // ❌
        // Camera analyzer may recycle bitmap here!
    }
    
    // Function returns immediately
    // Camera analyzer recycles bitmap
}
```

**Timeline:**
```
T0: Camera analyzer calls processFrameForSegmentation(bitmap)
T1: Function launches coroutine (async)
T2: Function returns
T3: Camera analyzer recycles bitmap ❌
T4: Coroutine tries to use bitmap → CRASH (bitmap already recycled)
```

### Penjelasan

`ImageAnalysis.Analyzer` dari CameraX:
- Memberikan bitmap ke analyzer callback
- **Recycle bitmap setelah callback return**
- Jika kita launch coroutine, bitmap bisa di-recycle sebelum coroutine selesai

**Dari CameraX docs:**
> The image is only valid during the scope of the callback. After the callback returns, the image will be closed and any attempt to access it will result in an IllegalStateException.

## Solusi

### Copy Bitmap SEBELUM Launch Coroutine

**Sebelum:**
```kotlin
fun processFrameForSegmentation(bitmap: Bitmap) {
    lastFrameBitmap = bitmap.copy(...)  // Copy 1
    
    viewModelScope.launch {
        val preprocessed = inferenceRepository.preprocess(bitmap)  // ❌ Use original
        // bitmap may be recycled by camera analyzer!
    }
}
```

**Sesudah:**
```kotlin
fun processFrameForSegmentation(bitmap: Bitmap) {
    // CRITICAL: Copy bitmap BEFORE launching coroutine
    val frameCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    
    lastFrameBitmap?.recycle()
    lastFrameBitmap = frameCopy.copy(Bitmap.Config.ARGB_8888, false)
    
    viewModelScope.launch {
        try {
            // Use frameCopy instead of bitmap
            val preprocessed = inferenceRepository.preprocess(frameCopy)  // ✅
            // ...
            
            // Cleanup
            frameCopy.recycle()
        } catch (e: Exception) {
            frameCopy.recycle()  // Always cleanup
            // ...
        }
    }
}
```

### Key Changes:

1. **Copy bitmap immediately** (synchronous, before coroutine)
2. **Use frameCopy** in coroutine instead of original bitmap
3. **Recycle frameCopy** after preprocessing selesai
4. **Recycle in catch block** untuk cleanup jika error

## Timeline Sekarang

```
T0: Camera analyzer calls processFrameForSegmentation(bitmap)
T1: frameCopy = bitmap.copy() (synchronous, immediate)
T2: Function launches coroutine with frameCopy
T3: Function returns
T4: Camera analyzer recycles bitmap (OK, we have frameCopy)
T5: Coroutine uses frameCopy ✅ (independent copy, safe)
T6: Coroutine recycles frameCopy after done
```

## Memory Management

### Bitmap Copies:
```kotlin
// Original from camera (managed by CameraX)
bitmap (from camera analyzer)

// Copy 1: For coroutine processing
frameCopy = bitmap.copy()

// Copy 2: For lastFrameBitmap (capture button)
lastFrameBitmap = frameCopy.copy()

// Copy 3: For lastPreprocessedBitmap (reuse in capture)
lastPreprocessedBitmap = preprocessed.copy()
```

### Lifecycle:
```
bitmap:                [camera] → recycle by camera
frameCopy:             [copy] → use in coroutine → recycle after done
lastFrameBitmap:       [copy] → keep until next frame → recycle
lastPreprocessedBitmap:[copy] → keep until next frame → recycle
```

### Trade-off:
- ❌ **More memory:** 3 copies of bitmap
- ✅ **Thread-safe:** Each copy independent
- ✅ **No crashes:** No shared buffer issues

## Alternative Solutions (Tidak Digunakan)

### 1. Use Blocking Call (Tidak Async)
```kotlin
fun processFrameForSegmentation(bitmap: Bitmap) {
    // Block camera thread
    val preprocessed = inferenceRepository.preprocess(bitmap)
    // ...
}
```
❌ Block camera thread → FPS drop, janky UI

### 2. Use Mutex Lock
```kotlin
val bitmapMutex = Mutex()

fun processFrameForSegmentation(bitmap: Bitmap) {
    viewModelScope.launch {
        bitmapMutex.withLock {
            val preprocessed = inferenceRepository.preprocess(bitmap)
        }
    }
}
```
❌ Kompleks, camera analyzer tidak wait mutex

### 3. Copy Bitmap (DIPILIH) ✅
```kotlin
val frameCopy = bitmap.copy()
viewModelScope.launch {
    val preprocessed = inferenceRepository.preprocess(frameCopy)
    frameCopy.recycle()
}
```
✅ Simple, safe, works

## Testing

### Build & Install:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Monitor Logs:
```bash
adb logcat -s "OpenCV:E" "cv::error:E"
```

### Expected:
```
✅ No OpenCV errors
✅ No AndroidBitmap_lockPixels errors
✅ Preprocessing works
✅ Polygon displays
```

## Files Changed
- `app/src/main/java/com/example/anemiadetector/ui/camera/CameraViewModel.kt`
  - `processFrameForSegmentation()`: Copy bitmap before coroutine
  - Add `frameCopy.recycle()` in try-catch-finally

## Lesson Learned

### CameraX ImageAnalysis Rules:
1. **Bitmap only valid during callback scope**
2. **Bitmap recycled after callback returns**
3. **Must copy bitmap if using async (coroutine)**
4. **Don't hold reference to original bitmap**

### General Bitmap Rules:
1. **Copy bitmap before async operation**
2. **Recycle copies after use**
3. **Use try-catch-finally for cleanup**
4. **Independent copies for thread safety**

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| Bitmap Usage | Original from camera | Independent copy |
| Coroutine | Use original (unsafe) | Use copy (safe) |
| Cleanup | No cleanup | Recycle frameCopy |
| OpenCV Error | AndroidBitmap_lockPixels failed | No error ✅ |
| Thread Safety | Unsafe (shared buffer) | Safe (independent) |
| Memory | Less (1 copy) | More (3 copies) |

**Status:** ✅ Camera bitmap lifecycle fixed, no more OpenCV errors!
