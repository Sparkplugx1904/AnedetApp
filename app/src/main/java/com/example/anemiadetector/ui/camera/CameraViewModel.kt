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
    private val inferenceRepository: InferenceRepository
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
        
        // Store frame for capture
        lastFrameBitmap?.recycle()
        lastFrameBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)

        viewModelScope.launch {
            try {
                // Preprocess
                val preprocessed = inferenceRepository.preprocess(bitmap)
                lastPreprocessedBitmap?.recycle()
                lastPreprocessedBitmap = preprocessed.copy(Bitmap.Config.ARGB_8888, false)

                // Segment
                val detection = inferenceRepository.segment(
                    preprocessed,
                    FRAME_WIDTH,
                    FRAME_HEIGHT
                )

                if (detection != null) {
                    _inferenceState.value = InferenceState.Success(detection, null)
                } else {
                    _inferenceState.value = InferenceState.NoDetection()
                }
            } catch (e: Exception) {
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

                _inferenceState.value = InferenceState.Success(detection, classification)

                // Cleanup
                preprocessed.recycle()
                crop.recycle()

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
     */
    private fun cropConjunctiva(
        preprocessed: Bitmap,
        bbox: RectF,
        polygon: List<android.graphics.PointF>
    ): Bitmap {
        // Scale bbox from frame coordinates to preprocessed bitmap coordinates (224x224)
        val scaleX = 224f / FRAME_WIDTH
        val scaleY = 224f / FRAME_HEIGHT

        val scaledBbox = RectF(
            bbox.left * scaleX,
            bbox.top * scaleY,
            bbox.right * scaleX,
            bbox.bottom * scaleY
        )

        // Ensure bbox is within bounds
        scaledBbox.left = scaledBbox.left.coerceIn(0f, 224f)
        scaledBbox.top = scaledBbox.top.coerceIn(0f, 224f)
        scaledBbox.right = scaledBbox.right.coerceIn(0f, 224f)
        scaledBbox.bottom = scaledBbox.bottom.coerceIn(0f, 224f)

        val width = (scaledBbox.right - scaledBbox.left).toInt().coerceAtLeast(1)
        val height = (scaledBbox.bottom - scaledBbox.top).toInt().coerceAtLeast(1)

        return Bitmap.createBitmap(
            preprocessed,
            scaledBbox.left.toInt(),
            scaledBbox.top.toInt(),
            width,
            height
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
    }

    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        stopLiveInference()
        lastFrameBitmap?.recycle()
        lastPreprocessedBitmap?.recycle()
        inferenceRepository.release()
    }
}
