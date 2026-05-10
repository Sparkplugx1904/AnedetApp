package com.example.anemiadetector.ui.camera

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anemiadetector.data.model.ClassificationResult
import com.example.anemiadetector.data.model.DetectionResult
import com.example.anemiadetector.data.model.InferenceState
import com.example.anemiadetector.data.repository.InferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for Camera Screen
 * Manages three inference modes:
 * 1. Live Segmentation (default) - Shows polygon overlay only
 * 2. Single Capture - Full pipeline on button press
 * 3. Live Inference - Full pipeline every 1 second
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val inferenceRepository: InferenceRepository,
    private val examinationRepository: com.example.anemiadetector.data.repository.ExaminationRepository
) : ViewModel() {

    companion object {
        const val FRAME_WIDTH = 1280
        const val FRAME_HEIGHT = 720
        const val SEG_INTERVAL_MS = 100L  // Max 10x per second
        const val LIVE_INFERENCE_INTERVAL_MS = 1000L  // 1 second
    }

    // Inference state
    private val _inferenceState = MutableStateFlow<InferenceState>(InferenceState.Idle)
    val inferenceState: StateFlow<InferenceState> = _inferenceState.asStateFlow()

    // Camera state
    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _liveInferenceEnabled = MutableStateFlow(false)
    val liveInferenceEnabled: StateFlow<Boolean> = _liveInferenceEnabled.asStateFlow()

    // Frame buffer
    private var lastFrameBitmap: Bitmap? = null
    private var lastPreprocessedBitmap: Bitmap? = null
    private var lastDetectionResult: DetectionResult? = null
    
    // Result bitmap for display
    private val _resultBitmap = MutableStateFlow<Bitmap?>(null)
    val resultBitmap: StateFlow<Bitmap?> = _resultBitmap.asStateFlow()

    // Timing
    private var lastSegInferenceMs = 0L
    private var lastLiveInferenceMs = 0L

    // Jobs
    private var liveInferenceJob: Job? = null

    // Camera control references
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    /**
     * Set camera control references
     */
    fun setCameraControl(control: CameraControl, info: CameraInfo) {
        cameraControl = control
        cameraInfo = info
    }

    /**
     * Process frame for live segmentation (default mode)
     * Called from ImageAnalysis analyzer
     */
    fun processFrameForSegmentation(bitmap: Bitmap) {
        val now = SystemClock.elapsedRealtime()
        
        // Frame skip for FPS management
        if (now - lastSegInferenceMs < SEG_INTERVAL_MS) {
            return
        }
        
        lastSegInferenceMs = now
        
        // CRITICAL: Copy bitmap BEFORE launching coroutine
        // Camera analyzer may recycle bitmap after this function returns
        val frameCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        
        // Store frame for capture
        lastFrameBitmap?.recycle()
        lastFrameBitmap = frameCopy.copy(Bitmap.Config.ARGB_8888, false)

        viewModelScope.launch {
            try {
                // Preprocess - use frameCopy instead of bitmap
                val preprocessed = inferenceRepository.preprocess(frameCopy)
                lastPreprocessedBitmap?.recycle()
                lastPreprocessedBitmap = preprocessed.copy(Bitmap.Config.ARGB_8888, false)

                // Segment
                val detection = inferenceRepository.segment(
                    preprocessed,
                    FRAME_WIDTH,
                    FRAME_HEIGHT
                )

                if (detection != null) {
                    lastDetectionResult = detection
                    _inferenceState.value = InferenceState.Success(detection, null)
                } else {
                    lastDetectionResult = null
                    _inferenceState.value = InferenceState.NoDetection()
                }
                
                // Cleanup
                frameCopy.recycle()
            } catch (e: Exception) {
                frameCopy.recycle()
                _inferenceState.value = InferenceState.Error(
                    "Segmentation failed: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * Capture and classify (Single Capture mode)
     * Uses last buffered frame
     */
    fun captureAndClassify() {
        val frame = lastFrameBitmap ?: run {
            _inferenceState.value = InferenceState.Error("No frame available")
            return
        }

        viewModelScope.launch {
            try {
                _inferenceState.value = InferenceState.Processing

                // Use cached preprocessed bitmap if available
                val preprocessed = lastPreprocessedBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                    ?: inferenceRepository.preprocess(frame)

                // Segment
                val detection = inferenceRepository.segment(
                    preprocessed,
                    FRAME_WIDTH,
                    FRAME_HEIGHT
                ) ?: run {
                    _inferenceState.value = InferenceState.NoDetection("Konjungtiva tidak terdeteksi")
                    preprocessed.recycle()
                    return@launch
                }

                // Crop conjunctiva from preprocessed bitmap
                val crop = cropConjunctiva(preprocessed, detection.boundingBox, detection.polygon)
                
                // Validate crop
                if (crop.width <= 0 || crop.height <= 0 || crop.byteCount == 0) {
                    _inferenceState.value = InferenceState.NoDetection("Area konjungtiva terlalu kecil")
                    preprocessed.recycle()
                    crop.recycle()
                    return@launch
                }

                // Classify
                val classification = inferenceRepository.classify(crop)
                
                // Store detection result
                lastDetectionResult = detection

                // Generate masked bitmap for display
                val maskedBitmap = generateMaskedBitmap(frame, detection, classification)
                _resultBitmap.value = maskedBitmap

                _inferenceState.value = InferenceState.Success(detection, classification)

                // Cleanup - recycle setelah classify selesai
                crop.recycle()
                preprocessed.recycle()

            } catch (e: Exception) {
                _inferenceState.value = InferenceState.Error(
                    "Classification failed: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * Toggle live inference mode
     */
    fun toggleLiveInference(enabled: Boolean) {
        _liveInferenceEnabled.value = enabled

        if (enabled) {
            startLiveInference()
        } else {
            stopLiveInference()
        }
    }

    /**
     * Start live inference (every 1 second)
     */
    private fun startLiveInference() {
        liveInferenceJob?.cancel()
        liveInferenceJob = viewModelScope.launch {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                
                if (now - lastLiveInferenceMs >= LIVE_INFERENCE_INTERVAL_MS) {
                    lastLiveInferenceMs = now
                    
                    val frame = lastFrameBitmap
                    if (frame != null) {
                        runFullPipeline(frame)
                    }
                }
                
                delay(100) // Check every 100ms
            }
        }
    }

    /**
     * Stop live inference
     */
    private fun stopLiveInference() {
        liveInferenceJob?.cancel()
        liveInferenceJob = null
    }

    /**
     * Run full pipeline (preprocessing + segmentation + classification)
     */
    private suspend fun runFullPipeline(frame: Bitmap) {
        try {
            // Preprocess
            val preprocessed = inferenceRepository.preprocess(frame)

            // Segment
            val detection = inferenceRepository.segment(
                preprocessed,
                FRAME_WIDTH,
                FRAME_HEIGHT
            ) ?: run {
                _inferenceState.value = InferenceState.NoDetection()
                preprocessed.recycle()
                return
            }

            // Crop
            val crop = cropConjunctiva(preprocessed, detection.boundingBox, detection.polygon)
            
            if (crop.width <= 0 || crop.height <= 0) {
                _inferenceState.value = InferenceState.NoDetection("Area terlalu kecil")
                preprocessed.recycle()
                crop.recycle()
                return
            }

            // Classify
            val classification = inferenceRepository.classify(crop)

            _inferenceState.value = InferenceState.Success(detection, classification)

            // Cleanup
            preprocessed.recycle()
            crop.recycle()

        } catch (e: Exception) {
            _inferenceState.value = InferenceState.Error("Inference failed: ${e.message}", e)
        }
    }

    /**
     * Crop conjunctiva region from preprocessed bitmap
     * FIXED: Account for letterbox offset when scaling coordinates
     */
    private fun cropConjunctiva(
        preprocessed: Bitmap,
        bbox: RectF,
        polygon: List<android.graphics.PointF>
    ): Bitmap {
        // Letterbox resize calculation
        // Original frame: 1280x720
        // Target size: 224x224
        val scale = 224f / FRAME_WIDTH  // = 0.175
        val newHeight = (FRAME_HEIGHT * scale).toInt()  // = 126px
        val yOffset = (224 - newHeight) / 2  // = 49px (black padding top/bottom)
        
        // Scale bbox from frame coordinates to preprocessed bitmap coordinates
        // Account for letterbox offset
        val scaleX = 224f / FRAME_WIDTH
        val scaleY = newHeight.toFloat() / FRAME_HEIGHT  // Scale to actual content height
        
        val scaledBbox = RectF(
            bbox.left * scaleX,
            bbox.top * scaleY + yOffset,  // Add y offset for letterbox padding
            bbox.right * scaleX,
            bbox.bottom * scaleY + yOffset
        )

        // Ensure bbox is within bounds
        scaledBbox.left = scaledBbox.left.coerceIn(0f, 224f)
        scaledBbox.top = scaledBbox.top.coerceIn(0f, 224f)
        scaledBbox.right = scaledBbox.right.coerceIn(0f, 224f)
        scaledBbox.bottom = scaledBbox.bottom.coerceIn(0f, 224f)

        val width = (scaledBbox.right - scaledBbox.left).toInt().coerceAtLeast(1)
        val height = (scaledBbox.bottom - scaledBbox.top).toInt().coerceAtLeast(1)

        // Create bitmap crop (shares pixel buffer with parent)
        val tempCrop = Bitmap.createBitmap(
            preprocessed,
            scaledBbox.left.toInt(),
            scaledBbox.top.toInt(),
            width,
            height
        )
        
        // Make independent copy so we can recycle preprocessed safely
        val crop = tempCrop.copy(Bitmap.Config.ARGB_8888, false)
        tempCrop.recycle()
        
        return crop
    }
    
    /**
     * Generate masked bitmap for display in result sheet
     * Draws polygon overlay on original frame
     */
    private fun generateMaskedBitmap(
        originalFrame: Bitmap,
        detection: DetectionResult,
        classification: ClassificationResult
    ): Bitmap {
        val color = com.example.anemiadetector.utils.PolygonUtils.getStatusColor(classification.isAnemic)
        return com.example.anemiadetector.utils.PolygonUtils.fillPolygonAlpha(
            originalFrame,
            detection.polygon,
            color,
            alpha = 77  // ~30% opacity
        )
    }

    /**
     * Toggle torch (flashlight)
     */
    fun setTorch(enabled: Boolean) {
        _torchEnabled.value = enabled
        cameraControl?.enableTorch(enabled)
    }

    /**
     * Focus on point (tap-to-focus)
     */
    fun focusOnPoint(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val factory = SurfaceOrientedMeteringPointFactory(
            viewWidth.toFloat(),
            viewHeight.toFloat()
        )
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        
        cameraControl?.startFocusAndMetering(action)
    }

    /**
     * Reset inference state
     */
    fun resetState() {
        _inferenceState.value = InferenceState.Idle
        _resultBitmap.value = null
    }
    
    /**
     * Save examination result to gallery and database
     * 
     * @param context Application context
     * @param bitmap Masked bitmap to save
     * @param classification Classification result
     * @return true if save successful, false otherwise
     */
    suspend fun saveExamination(
        context: android.content.Context,
        bitmap: Bitmap,
        classification: ClassificationResult
    ): Boolean {
        return try {
            // Save to MediaStore (Gallery)
            val uri = com.example.anemiadetector.utils.MediaStoreUtils.saveBitmapToGallery(
                context,
                bitmap
            )
            
            if (uri == null) {
                android.util.Log.e("CameraViewModel", "Failed to save image to gallery")
                return false
            }
            
            val imagePath = com.example.anemiadetector.utils.MediaStoreUtils.getPathFromUri(uri)
            
            // Save to Room database with correct field names
            val examination = com.example.anemiadetector.data.local.entity.ExaminationEntity(
                timestamp = System.currentTimeMillis(),
                labelAnemia = classification.allScores?.get(0) ?: classification.confidence,
                labelNonAnemia = classification.allScores?.get(1) ?: (1f - classification.confidence),
                predictedLabel = if (classification.isAnemic) "ANEMIA" else "NON_ANEMIA",
                confidence = classification.confidence,
                imagePath = imagePath,
                mode = "SINGLE_CAPTURE"
            )
            
            // Save to database using repository
            examinationRepository.insert(examination)
            
            android.util.Log.d("CameraViewModel", "Examination saved successfully: $imagePath")
            true
            
        } catch (e: Exception) {
            android.util.Log.e("CameraViewModel", "Failed to save examination", e)
            false
        }
    }

    /**
     * Cleanup resources
     * Note: Don't release inferenceRepository here because it's a Singleton
     * and will be reused when screen is recreated
     */
    override fun onCleared() {
        super.onCleared()
        stopLiveInference()
        lastFrameBitmap?.recycle()
        lastPreprocessedBitmap?.recycle()
        // Don't call inferenceRepository.release() - it's a Singleton
    }
}
