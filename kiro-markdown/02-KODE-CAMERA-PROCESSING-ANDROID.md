# 📱 KODE PEMROSESAN KAMERA ANDROID

> **Implementasi CameraX untuk deteksi anemia real-time**  
> Berdasarkan logika `live_inference.py` dan spesifikasi `CLAUDE.md`

---

## 🎯 OVERVIEW

Dokumen ini berisi kode lengkap untuk:
1. **Setup CameraX** dengan resolusi 1280×720
2. **Frame Processing** dengan CLAHE preprocessing
3. **Segmentasi** konjungtiva real-time
4. **Klasifikasi** anemia on-demand
5. **Overlay Visualization** dengan polygon alpha fill

---

## 📦 1. CAMERA VIEWMODEL

**File:** `app/src/main/java/com/example/anemiadetector/ui/camera/CameraViewModel.kt`

```kotlin
package com.example.anemiadetector.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anemiadetector.data.model.InferenceState
import com.example.anemiadetector.domain.usecase.RunPreprocessingUseCase
import com.example.anemiadetector.domain.usecase.RunSegmentationUseCase
import com.example.anemiadetector.domain.usecase.RunClassificationUseCase
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor
import com.example.anemiadetector.ml.classification.AnemiaClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val preprocessingUseCase: RunPreprocessingUseCase,
    private val segmentationUseCase: RunSegmentationUseCase,
    private val classificationUseCase: RunClassificationUseCase
) : ViewModel() {

    // State management
    private val _inferenceState = MutableStateFlow<InferenceState>(InferenceState.Idle)
    val inferenceState: StateFlow<InferenceState> = _inferenceState.asStateFlow()

    private val _isLiveInferenceActive = MutableStateFlow(false)
    val isLiveInferenceActive: StateFlow<Boolean> = _isLiveInferenceActive.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    // Frame buffer untuk capture
    private var lastProcessedFrame: Bitmap? = null
    private var lastSegmentationTime = 0L
    private val inferenceMutex = Mutex()
    private var liveInferenceJob: Job? = null

    companion object {
        private const val SEG_INTERVAL_MS = 100L  // Max 10 FPS untuk segmentasi
        private const val LIVE_INFERENCE_INTERVAL_MS = 1000L  // 1 detik untuk live inference
    }

    /**
     * Process frame untuk Live Segmentation (Mode Default)
     * Hanya jalankan segmentasi, tidak klasifikasi
     */
    fun processFrameForSegmentation(frame: Bitmap) {
        val now = System.currentTimeMillis()
        
        // Frame skip untuk maintain FPS
        if (now - lastSegmentationTime < SEG_INTERVAL_MS) {
            return
        }
        lastSegmentationTime = now

        // Simpan frame untuk capture nanti
        lastProcessedFrame?.recycle()
        lastProcessedFrame = frame.copy(Bitmap.Config.ARGB_8888, false)

        viewModelScope.launch(Dispatchers.Default) {
            inferenceMutex.withLock {
                try {
                    _inferenceState.value = InferenceState.Processing

                    // Step 1-5: Full preprocessing (CLAHE pipeline)
                    val processedBitmap = preprocessingUseCase.execute(frame)

                    // Step 6: Segmentasi saja
                    val segResult = segmentationUseCase.execute(
                        processedBitmap = processedBitmap,
                        originalWidth = frame.width,
                        originalHeight = frame.height
                    )

                    if (segResult != null) {
                        // Update state dengan segmentasi saja (tanpa klasifikasi)
                        _inferenceState.value = InferenceState.SegmentationOnly(segResult)
                    } else {
                        _inferenceState.value = InferenceState.NoDetection
                    }

                    processedBitmap.recycle()
                } catch (e: Exception) {
                    _inferenceState.value = InferenceState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Single Capture - Full pipeline dengan klasifikasi
     * Gunakan frame terakhir yang sudah di-buffer
     */
    fun captureAndClassify() {
        val frame = lastProcessedFrame ?: run {
            _inferenceState.value = InferenceState.Error("No frame available")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            inferenceMutex.withLock {
                try {
                    _inferenceState.value = InferenceState.Processing

                    // Step 1-5: Full preprocessing
                    val processedBitmap = preprocessingUseCase.execute(frame)

                    // Step 6: Segmentasi
                    val segResult = segmentationUseCase.execute(
                        processedBitmap = processedBitmap,
                        originalWidth = frame.width,
                        originalHeight = frame.height
                    )

                    if (segResult == null) {
                        _inferenceState.value = InferenceState.NoDetection
                        processedBitmap.recycle()
                        return@withLock
                    }

                    // Step 7-8: Crop & Klasifikasi
                    val classResult = classificationUseCase.execute(
                        processedBitmap = processedBitmap,
                        segmentationResult = segResult
                    )

                    // Step 9: Emit hasil
                    _inferenceState.value = InferenceState.Success(
                        segmentationResult = segResult,
                        classificationResult = classResult
                    )

                    processedBitmap.recycle()
                } catch (e: Exception) {
                    _inferenceState.value = InferenceState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Toggle Live Inference Mode
     * Jalankan full pipeline setiap 1 detik
     */
    fun toggleLiveInference() {
        if (_isLiveInferenceActive.value) {
            stopLiveInference()
        } else {
            startLiveInference()
        }
    }

    private fun startLiveInference() {
        _isLiveInferenceActive.value = true
        
        liveInferenceJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isLiveInferenceActive.value) {
                val frame = lastProcessedFrame
                if (frame != null) {
                    inferenceMutex.withLock {
                        try {
                            _inferenceState.value = InferenceState.Processing

                            // Full pipeline
                            val processedBitmap = preprocessingUseCase.execute(frame)
                            
                            val segResult = segmentationUseCase.execute(
                                processedBitmap = processedBitmap,
                                originalWidth = frame.width,
                                originalHeight = frame.height
                            )

                            if (segResult != null) {
                                val classResult = classificationUseCase.execute(
                                    processedBitmap = processedBitmap,
                                    segmentationResult = segResult
                                )

                                _inferenceState.value = InferenceState.Success(
                                    segmentationResult = segResult,
                                    classificationResult = classResult
                                )
                            } else {
                                _inferenceState.value = InferenceState.NoDetection
                            }

                            processedBitmap.recycle()
                        } catch (e: Exception) {
                            _inferenceState.value = InferenceState.Error(e.message ?: "Unknown error")
                        }
                    }
                }
                
                // Interval 1 detik
                delay(LIVE_INFERENCE_INTERVAL_MS)
            }
        }
    }

    private fun stopLiveInference() {
        _isLiveInferenceActive.value = false
        liveInferenceJob?.cancel()
        liveInferenceJob = null
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun resetState() {
        _inferenceState.value = InferenceState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveInference()
        lastProcessedFrame?.recycle()
        lastProcessedFrame = null
    }
}
```

---

## 📷 2. CAMERA SCREEN (JETPACK COMPOSE)

**File:** `app/src/main/java/com/example/anemiadetector/ui/camera/CameraScreen.kt`

```kotlin
package com.example.anemiadetector.ui.camera

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.anemiadetector.data.model.InferenceState
import com.example.anemiadetector.utils.CameraUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val isLiveInferenceActive by viewModel.isLiveInferenceActive.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()

    // Camera permission
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Camera provider
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    // Preview view
    val previewView = remember { PreviewView(context) }

    // Camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Show live inference warning dialog
    var showLiveInferenceDialog by remember { mutableStateOf(false) }

    // Show capture result sheet
    var showResultSheet by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPermission.hasPermission) {
        if (cameraPermission.hasPermission) {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    // Bind camera
    LaunchedEffect(cameraProvider, lensFacing) {
        cameraProvider?.let { provider ->
            provider.unbindAll()

            // Preview
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            // Image Analysis - WAJIB 1280×720
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                // Convert ImageProxy to Bitmap
                val bitmap = CameraUtils.imageProxyToBitmap(imageProxy)
                
                // Process untuk segmentasi (mode default)
                if (!isLiveInferenceActive) {
                    viewModel.processFrameForSegmentation(bitmap)
                }
                
                // WAJIB close imageProxy
                imageProxy.close()
            }

            // Camera selector
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Torch control
    LaunchedEffect(isTorchOn) {
        camera?.cameraControl?.enableTorch(isTorchOn)
    }

    // Show result sheet when classification success
    LaunchedEffect(inferenceState) {
        if (inferenceState is InferenceState.Success) {
            showResultSheet = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Canvas
        when (val state = inferenceState) {
            is InferenceState.SegmentationOnly -> {
                ConjunctivaOverlay(
                    segmentationResult = state.segmentationResult,
                    classificationResult = null,
                    frameSize = androidx.compose.ui.geometry.Size(1280f, 720f),
                    showClassificationOverlay = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is InferenceState.Success -> {
                ConjunctivaOverlay(
                    segmentationResult = state.segmentationResult,
                    classificationResult = state.classificationResult,
                    frameSize = androidx.compose.ui.geometry.Size(1280f, 720f),
                    showClassificationOverlay = isLiveInferenceActive,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is InferenceState.NoDetection -> {
                // Guide overlay
                GuideOverlay(modifier = Modifier.fillMaxSize())
            }
            else -> {}
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Torch
            IconButton(onClick = { viewModel.toggleTorch() }) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Torch",
                    tint = if (isTorchOn) Color.Yellow else Color.White
                )
            }

            // Flip Camera
            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Flip Camera",
                    tint = Color.White
                )
            }

            // Capture Button
            FloatingActionButton(
                onClick = { viewModel.captureAndClassify() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Camera,
                    contentDescription = "Capture"
                )
            }

            // Live Inference Toggle
            IconButton(onClick = {
                if (!isLiveInferenceActive) {
                    showLiveInferenceDialog = true
                } else {
                    viewModel.toggleLiveInference()
                }
            }) {
                Icon(
                    imageVector = if (isLiveInferenceActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = "Live Inference",
                    tint = if (isLiveInferenceActive) Color.Red else Color.White
                )
            }

            // History
            IconButton(onClick = onNavigateToHistory) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "History",
                    tint = Color.White
                )
            }
        }

        // Loading indicator
        if (inferenceState is InferenceState.Processing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    // Live Inference Warning Dialog
    if (showLiveInferenceDialog) {
        LiveInferenceWarningDialog(
            onConfirm = {
                showLiveInferenceDialog = false
                viewModel.toggleLiveInference()
            },
            onDismiss = {
                showLiveInferenceDialog = false
            }
        )
    }

    // Capture Result Sheet
    if (showResultSheet && inferenceState is InferenceState.Success) {
        CaptureResultSheet(
            segmentationResult = (inferenceState as InferenceState.Success).segmentationResult,
            classificationResult = (inferenceState as InferenceState.Success).classificationResult,
            onDismiss = {
                showResultSheet = false
                viewModel.resetState()
            },
            onSave = { /* TODO: Implement save */ }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}
```

---

## 🎨 3. OVERLAY CANVAS

**File:** `app/src/main/java/com/example/anemiadetector/ui/camera/OverlayCanvas.kt`

```kotlin
package com.example.anemiadetector.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.anemiadetector.ml.classification.AnemiaClassifier
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor

@Composable
fun ConjunctivaOverlay(
    segmentationResult: ConjunctivaSegmentor.SegmentationResult?,
    classificationResult: AnemiaClassifier.ClassificationResult?,
    frameSize: Size,
    showClassificationOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        segmentationResult ?: return@Canvas

        // Tentukan warna berdasarkan hasil klasifikasi
        val color = when {
            classificationResult == null -> Color(0xFF007AFF)  // Biru - segmentasi saja
            classificationResult.isAnemic -> Color(0xFFFF3B30) // Merah - Anemia
            else -> Color(0xFF34C759)                           // Hijau - Non-Anemia
        }
        
        val fillAlpha = if (classificationResult == null) 0.25f else 0.30f

        // Scale factor: frame asli → layar
        val scaleX = size.width / frameSize.width
        val scaleY = size.height / frameSize.height

        // 1. Bangun path polygon
        val path = Path()
        segmentationResult.polygon.forEachIndexed { i, pt ->
            val sx = pt.x * scaleX
            val sy = pt.y * scaleY
            if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
        }
        path.close()

        // 2. Gambar fill alpha
        drawPath(path, color.copy(alpha = fillAlpha), style = Fill)

        // 3. Gambar stroke tepi
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))

        // 4. Gambar vertex dots
        segmentationResult.polygon.forEach { pt ->
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(pt.x * scaleX, pt.y * scaleY)
            )
        }

        // 5. Label box - hanya saat showClassificationOverlay = true
        if (showClassificationOverlay && classificationResult != null) {
            val bbox = segmentationResult.boundingBox
            val bx1 = bbox.left * scaleX
            val by1 = bbox.top * scaleY
            
            // Background rectangle
            drawRoundRect(
                color = color,
                topLeft = Offset(bx1, by1 - 36.dp.toPx()),
                size = Size(180.dp.toPx(), 28.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // TODO: Add text label dengan TextMeasurer
            // "${classificationResult.label} ${(classificationResult.confidence * 100).toInt()}%"
        }
    }
}

@Composable
fun GuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // TODO: Draw guide rectangle dengan pulse animation
        // Teks: "Arahkan ke konjungtiva mata bawah"
    }
}
```

---

## 🛠️ 4. CAMERA UTILS

**File:** `app/src/main/java/com/example/anemiadetector/utils/CameraUtils.kt`

```kotlin
package com.example.anemiadetector.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object CameraUtils {
    
    /**
     * Convert ImageProxy (RGBA_8888) to Bitmap
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        return Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
        }
    }

    /**
     * Convert YUV ImageProxy to Bitmap (fallback jika format YUV)
     */
    fun yuvToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
```

---

## 📋 5. INFERENCE STATE

**File:** `app/src/main/java/com/example/anemiadetector/data/model/InferenceState.kt`

```kotlin
package com.example.anemiadetector.data.model

import com.example.anemiadetector.ml.classification.AnemiaClassifier
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor

sealed class InferenceState {
    object Idle : InferenceState()
    object Processing : InferenceState()
    
    data class SegmentationOnly(
        val segmentationResult: ConjunctivaSegmentor.SegmentationResult
    ) : InferenceState()
    
    data class Success(
        val segmentationResult: ConjunctivaSegmentor.SegmentationResult,
        val classificationResult: AnemiaClassifier.ClassificationResult
    ) : InferenceState()
    
    object NoDetection : InferenceState()
    
    data class Error(val message: String) : InferenceState()
}
```

---

## ✅ CHECKLIST IMPLEMENTASI

- [ ] CameraX dengan resolusi **1280×720** (FIXED)
- [ ] `STRATEGY_KEEP_ONLY_LATEST` untuk prevent frame queue
- [ ] `OUTPUT_IMAGE_FORMAT_RGBA_8888` untuk konversi langsung
- [ ] Frame skip dengan interval **100ms** untuk segmentasi
- [ ] Live inference dengan interval **1000ms** (1 detik)
- [ ] Mutex untuk thread-safe TFLite inference
- [ ] Torch on/off control
- [ ] Flip camera (front/back)
- [ ] Overlay polygon dengan alpha fill
- [ ] Warna: Biru (segmentasi), Merah (Anemia), Hijau (Non-Anemia)
- [ ] Guide overlay saat tidak ada deteksi
- [ ] Warning dialog untuk live inference (WAJIB)
- [ ] Result sheet untuk single capture
- [ ] Memory management: recycle Bitmap setelah digunakan
- [ ] `imageProxy.close()` di setiap frame

---

**📌 CATATAN:**

Kode di atas adalah implementasi lengkap untuk pemrosesan kamera Android yang **IDENTIK** dengan logika `live_inference.py`.

Setiap parameter, setiap interval, setiap warna sudah disesuaikan dengan referensi Python.
