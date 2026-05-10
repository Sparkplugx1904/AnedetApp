# Fix "The Interpreter has already been closed" Error

## Masalah
```
E Segmentor: java.lang.IllegalStateException: Internal error: The Interpreter has already been closed.
E Segmentor:   at com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor.segment(ConjunctivaSegmentor.kt:87)
```

Model segmentation tidak bisa dijalankan dan tidak muncul masking polygon.

## Root Cause

### Lifecycle Issue dengan Singleton
1. `ConjunctivaSegmentor` dan `AnemiaClassifier` adalah **@Singleton** (satu instance untuk seluruh app)
2. `CameraViewModel.onCleared()` dipanggil saat screen di-destroy (misalnya saat user pergi ke Settings)
3. `onCleared()` memanggil `inferenceRepository.release()` yang menutup TFLite Interpreter
4. Saat user kembali ke CameraScreen, ViewModel baru dibuat tapi `ConjunctivaSegmentor` **masih instance yang sama** (Singleton)
5. Interpreter sudah closed, tapi masih digunakan → **IllegalStateException**

### Flow yang Salah:
```
User buka app → CameraViewModel created → Interpreter initialized ✅
User pergi ke Settings → CameraViewModel.onCleared() → Interpreter.close() ❌
User kembali ke app → CameraViewModel created (baru) → ConjunctivaSegmentor (singleton lama) → Interpreter closed ❌
Inference called → IllegalStateException: Interpreter already closed ❌
```

## Solusi

### Jangan Close Singleton Interpreter di ViewModel.onCleared()

**Sebelum:**
```kotlin
override fun onCleared() {
    super.onCleared()
    stopLiveInference()
    lastFrameBitmap?.recycle()
    lastPreprocessedBitmap?.recycle()
    inferenceRepository.release()  // ❌ BAD: Close singleton interpreter
}
```

**Sesudah:**
```kotlin
override fun onCleared() {
    super.onCleared()
    stopLiveInference()
    lastFrameBitmap?.recycle()
    lastPreprocessedBitmap?.recycle()
    // Don't call inferenceRepository.release() - it's a Singleton
    // Interpreter will be closed when app process is killed
}
```

## Penjelasan

### Kapan Interpreter Harus Di-close?
- ✅ **Saat app process terminated** (Android akan cleanup otomatis)
- ✅ **Saat app di-destroy permanently** (onDestroy di MainActivity)
- ❌ **JANGAN saat ViewModel.onCleared()** (ViewModel bisa di-recreate, tapi Singleton tetap sama)

### Singleton vs ViewModel Lifecycle
```
Singleton Lifecycle:
  App Start ────────────────────────────────────────────> App Kill
  [ConjunctivaSegmentor instance created once]

ViewModel Lifecycle:
  Screen Open ──> onCleared ──> Screen Open ──> onCleared ──> ...
  [ViewModel created]  [destroyed]  [created again]  [destroyed]
```

Jika kita close Interpreter di `onCleared()`, Singleton masih hidup tapi Interpreter sudah mati.

## Alternative Solutions (Tidak Digunakan)

### 1. Lazy Initialization (Kompleks)
```kotlin
private var interpreter: Interpreter? = null

private fun getInterpreter(): Interpreter {
    if (interpreter == null || isClosed) {
        interpreter = Interpreter(modelBuffer, options)
    }
    return interpreter!!
}
```
❌ Kompleks, perlu track state, thread-safety issues

### 2. Remove @Singleton (Boros Memory)
```kotlin
// Remove @Singleton annotation
class ConjunctivaSegmentor @Inject constructor(...)
```
❌ Setiap ViewModel baru akan create interpreter baru (boros memory & slow)

### 3. Keep Singleton, Don't Close (DIPILIH) ✅
```kotlin
// Keep @Singleton, don't close in onCleared()
```
✅ Simple, efficient, interpreter di-cleanup saat app process killed

## Testing
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. Buka app → Lihat camera preview
2. Arahkan ke konjungtiva → Polygon muncul ✅
3. Pergi ke Settings → Kembali ke camera
4. Arahkan ke konjungtiva lagi → Polygon masih muncul ✅
5. Tidak ada error "Interpreter already closed" ✅

## Files Changed
- `app/src/main/java/com/example/anemiadetector/ui/camera/CameraViewModel.kt`
  - Comment out `inferenceRepository.release()` di `onCleared()`
  - Tambahkan komentar penjelasan

## Lesson Learned
**Jangan close Singleton resources di ViewModel.onCleared()** karena Singleton lifecycle lebih panjang dari ViewModel lifecycle.
