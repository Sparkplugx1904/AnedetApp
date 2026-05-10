package com.example.anemiadetector.ml.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.example.anemiadetector.utils.loadModelBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConjunctivaSegmentor @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {

    companion object {
        const val INPUT_SIZE = 320
        const val CONF_THRESHOLD = 0.35f
        private const val MODEL_PATH = "models/segments/best_float16.tflite"
    }

    data class SegmentationResult(
        val polygon: List<PointF>,
        val boundingBox: RectF,
        val confidence: Float
    )

    private val interpreter: Interpreter = run {
        try {
            Log.d("ConjunctivaSegmentor", "Starting TFLite initialization...")
            
            // Force disable GMS client - use bundled TFLite only
            System.setProperty("tflite.disable_gms_client", "true")
            Log.d("ConjunctivaSegmentor", "GMS client disabled")
            
            // Load model buffer
            val modelBuffer = loadModelBuffer(context, MODEL_PATH)
            Log.d("ConjunctivaSegmentor", "Model buffer loaded: ${modelBuffer.capacity()} bytes")
            
            // Create interpreter options - CPU ONLY untuk FP16 model
            val options = Interpreter.Options().apply { 
                setNumThreads(4)
                // Disable delegates untuk konsistensi inference
                // FP16 model: weights FP16, input/output FLOAT32
                setUseNNAPI(false)
                setUseXNNPACK(false)
                // Pure CPU inference only - no delegates
            }
            Log.d("ConjunctivaSegmentor", "Interpreter options created (CPU-only, FP16 model)")
            
            // Create interpreter
            val interp = Interpreter(modelBuffer, options)
            Log.d("ConjunctivaSegmentor", "Interpreter created successfully")
            
            // Log model info
            for (i in 0 until interp.outputTensorCount) {
                Log.d("ModelInfo", "Output $i shape: ${interp.getOutputTensor(i).shape().contentToString()}")
                Log.d("ModelInfo", "Output $i dtype: ${interp.getOutputTensor(i).dataType()}")
            }
            
            interp
        } catch (e: Exception) {
            Log.e("ConjunctivaSegmentor", "Failed to initialize TFLite interpreter", e)
            Log.e("ConjunctivaSegmentor", "Exception type: ${e.javaClass.name}")
            Log.e("ConjunctivaSegmentor", "Exception message: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to load segmentation model: ${e.message}", e)
        }
    }

    fun segment(preprocessedBitmap: Bitmap, originalWidth: Int, originalHeight: Int): SegmentationResult? {
        val resized = Bitmap.createScaledBitmap(preprocessedBitmap, INPUT_SIZE, INPUT_SIZE, true)
        // FP16 model selalu pakai float buffer (input FLOAT32)
        val input = toFloatBuffer(resized)

        // Model FP16 NMS-embedded output
        // Output 0: [1, 300, 38] - detections with mask coefficients
        // Output 1 (if exists): proto masks for mask reconstruction
        val output0 = Array(1) { Array(300) { FloatArray(38) } }
        
        // Check if model has proto masks output (for proper segmentation)
        val hasProtoMasks = interpreter.outputTensorCount > 1
        val protoMasks = if (hasProtoMasks) {
            // Proto masks typically [1, 32, mask_h, mask_w] or [1, mask_h, mask_w, 32]
            val protoShape = interpreter.getOutputTensor(1).shape()
            Log.d("ConjunctivaSegmentor", "Proto masks shape: ${protoShape.contentToString()}")
            
            // Allocate buffer based on actual shape
            // Common shapes: [1, 32, 160, 160] or [1, 160, 160, 32]
            when {
                protoShape.size == 4 && protoShape[1] == 32 -> {
                    // [1, 32, H, W]
                    Array(1) { Array(32) { Array(protoShape[2]) { FloatArray(protoShape[3]) } } }
                }
                protoShape.size == 4 && protoShape[3] == 32 -> {
                    // [1, H, W, 32]
                    Array(1) { Array(protoShape[1]) { Array(protoShape[2]) { FloatArray(32) } } }
                }
                else -> null
            }
        } else {
            null
        }
        
        try {
            if (protoMasks != null) {
                // Run inference with both outputs
                val outputs = mapOf(
                    0 to output0,
                    1 to protoMasks
                )
                interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)
                Log.d("ConjunctivaSegmentor", "Inference success with proto masks")
            } else {
                // Run inference with single output
                interpreter.run(input, output0)
                Log.d("ConjunctivaSegmentor", "Inference success, output shape: [1, 300, 38]")
            }
            
            // Parse output untuk extract detection
            val result = parseOutput(output0, protoMasks, originalWidth, originalHeight, resized)
            if (result != null) {
                Log.d("ConjunctivaSegmentor", "Detection found: confidence=${result.confidence}, polygon points=${result.polygon.size}")
            } else {
                Log.d("ConjunctivaSegmentor", "No detection above threshold")
            }
            
            resized.recycle()
            return result
        } catch (e: Exception) {
            Log.e("Segmentor", "Inference failed", e)
            resized.recycle()
            return null
        }
    }
    
    /**
     * Parse model output [1, 300, 38] untuk extract detection
     * Output format: [x1, y1, x2, y2, confidence, class_id, mask_coeffs(32)]
     */
    private fun parseOutput(
        output: Array<Array<FloatArray>>,
        protoMasks: Any?,
        originalWidth: Int,
        originalHeight: Int,
        resizedBitmap: Bitmap
    ): SegmentationResult? {
        val detections = output[0]  // [300, 38]
        
        // Find best detection above threshold
        for (i in 0 until 300) {
            val detection = detections[i]
            
            // Extract data
            val x1 = detection[0]
            val y1 = detection[1]
            val x2 = detection[2]
            val y2 = detection[3]
            val confidence = detection[4]
            val classId = detection[5].toInt()
            
            // Filter by confidence threshold
            if (confidence < CONF_THRESHOLD) {
                // Detections are sorted by confidence, so we can break early
                break
            }
            
            // Filter by class (0 = conjunctiva)
            if (classId != 0) continue
            
            // Denormalize coordinates from [0-1] to original frame size
            val x1Denorm = x1 * originalWidth
            val y1Denorm = y1 * originalHeight
            val x2Denorm = x2 * originalWidth
            val y2Denorm = y2 * originalHeight
            
            // Create bounding box
            val bbox = RectF(x1Denorm, y1Denorm, x2Denorm, y2Denorm)
            
            // Extract mask coefficients (32 values from index 6-37)
            val maskCoeffs = FloatArray(32) { idx -> detection[6 + idx] }
            
            // Decode polygon from mask coefficients if proto masks available
            val polygon = if (protoMasks != null) {
                try {
                    decodeMaskToPolygon(
                        maskCoeffs,
                        protoMasks,
                        bbox,
                        originalWidth,
                        originalHeight
                    )
                } catch (e: Exception) {
                    Log.w("ConjunctivaSegmentor", "Mask decoding failed, using bbox: ${e.message}")
                    // Fallback to rectangle
                    createRectanglePolygon(x1Denorm, y1Denorm, x2Denorm, y2Denorm)
                }
            } else {
                // No proto masks - use rectangle as fallback
                Log.d("ConjunctivaSegmentor", "No proto masks available, using bbox rectangle")
                createRectanglePolygon(x1Denorm, y1Denorm, x2Denorm, y2Denorm)
            }
            
            Log.d("ConjunctivaSegmentor", "Detection: bbox=$bbox, conf=$confidence, class=$classId, polygon_pts=${polygon.size}")
            
            return SegmentationResult(
                polygon = polygon,
                boundingBox = bbox,
                confidence = confidence
            )
        }
        
        return null  // No detection above threshold
    }
    
    /**
     * Create simple rectangle polygon from bbox coordinates
     */
    private fun createRectanglePolygon(x1: Float, y1: Float, x2: Float, y2: Float): List<PointF> {
        return listOf(
            PointF(x1, y1),
            PointF(x2, y1),
            PointF(x2, y2),
            PointF(x1, y2)
        )
    }
    
    /**
     * Decode mask coefficients to polygon using proto masks
     * Implements: mask = sigmoid(proto @ coeffs.T)
     */
    private fun decodeMaskToPolygon(
        maskCoeffs: FloatArray,
        protoMasks: Any,
        bbox: RectF,
        originalWidth: Int,
        originalHeight: Int
    ): List<PointF> {
        // Determine proto mask shape and extract data
        val (protoHeight, protoWidth, protoChannels, protoData) = when (protoMasks) {
            is Array<*> -> {
                val arr = protoMasks as Array<Array<Array<FloatArray>>>
                // Shape: [1, 32, H, W]
                val h = arr[0][0].size
                val w = arr[0][0][0].size
                val channels = 32
                val data = Array(channels) { c ->
                    Array(h) { y ->
                        FloatArray(w) { x ->
                            arr[0][c][y][x]
                        }
                    }
                }
                Tuple4(h, w, channels, data)
            }
            else -> {
                Log.w("ConjunctivaSegmentor", "Unknown proto mask format")
                throw IllegalArgumentException("Unsupported proto mask format")
            }
        }
        
        Log.d("ConjunctivaSegmentor", "Proto masks: ${protoChannels}x${protoHeight}x${protoWidth}")
        
        // Matrix multiplication: proto @ coeffs.T
        // Result shape: [H, W]
        val maskRaw = Array(protoHeight) { y ->
            FloatArray(protoWidth) { x ->
                var sum = 0f
                for (c in 0 until protoChannels) {
                    sum += protoData[c][y][x] * maskCoeffs[c]
                }
                sum
            }
        }
        
        // Apply sigmoid activation
        val maskSigmoid = Array(protoHeight) { y ->
            FloatArray(protoWidth) { x ->
                sigmoid(maskRaw[y][x])
            }
        }
        
        // Threshold to binary mask (threshold = 0.5)
        val binaryMask = Array(protoHeight) { y ->
            BooleanArray(protoWidth) { x ->
                maskSigmoid[y][x] > 0.5f
            }
        }
        
        // Scale bbox to proto mask coordinates
        val scaleX = protoWidth.toFloat() / INPUT_SIZE
        val scaleY = protoHeight.toFloat() / INPUT_SIZE
        
        val bboxInProto = RectF(
            (bbox.left / originalWidth * INPUT_SIZE * scaleX).coerceIn(0f, protoWidth.toFloat()),
            (bbox.top / originalHeight * INPUT_SIZE * scaleY).coerceIn(0f, protoHeight.toFloat()),
            (bbox.right / originalWidth * INPUT_SIZE * scaleX).coerceIn(0f, protoWidth.toFloat()),
            (bbox.bottom / originalHeight * INPUT_SIZE * scaleY).coerceIn(0f, protoHeight.toFloat())
        )
        
        // Extract contour from binary mask within bbox
        val contourPoints = extractContourFromMask(
            binaryMask,
            bboxInProto,
            protoWidth,
            protoHeight
        )
        
        if (contourPoints.isEmpty()) {
            Log.w("ConjunctivaSegmentor", "No contour found in mask, using bbox")
            return createRectanglePolygon(bbox.left, bbox.top, bbox.right, bbox.bottom)
        }
        
        // Scale contour points back to original frame coordinates
        val scaledContour = contourPoints.map { pt ->
            PointF(
                pt.x / scaleX / INPUT_SIZE * originalWidth,
                pt.y / scaleY / INPUT_SIZE * originalHeight
            )
        }
        
        // Apply adaptive polygon reduction (6-15 points)
        val adaptivePolygon = com.example.anemiadetector.utils.PolygonUtils.getAdaptivePolygon(
            scaledContour,
            minPts = 6,
            maxPts = 15
        )
        
        Log.d("ConjunctivaSegmentor", "Contour: ${contourPoints.size} pts → Adaptive: ${adaptivePolygon.size} pts")
        
        return adaptivePolygon
    }
    
    /**
     * Sigmoid activation function
     */
    private fun sigmoid(x: Float): Float {
        return 1f / (1f + kotlin.math.exp(-x))
    }
    
    /**
     * Extract contour from binary mask using simple edge tracing
     */
    private fun extractContourFromMask(
        mask: Array<BooleanArray>,
        bbox: RectF,
        width: Int,
        height: Int
    ): List<PointF> {
        val contour = mutableListOf<PointF>()
        
        val x1 = bbox.left.toInt().coerceIn(0, width - 1)
        val y1 = bbox.top.toInt().coerceIn(0, height - 1)
        val x2 = bbox.right.toInt().coerceIn(0, width - 1)
        val y2 = bbox.bottom.toInt().coerceIn(0, height - 1)
        
        // Scan bbox region for edge pixels (simple contour extraction)
        for (y in y1..y2) {
            for (x in x1..x2) {
                if (mask[y][x]) {
                    // Check if this is an edge pixel (has at least one non-mask neighbor)
                    var isEdge = false
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val ny = (y + dy).coerceIn(0, height - 1)
                            val nx = (x + dx).coerceIn(0, width - 1)
                            if (!mask[ny][nx]) {
                                isEdge = true
                                break
                            }
                        }
                        if (isEdge) break
                    }
                    
                    if (isEdge) {
                        contour.add(PointF(x.toFloat(), y.toFloat()))
                    }
                }
            }
        }
        
        return contour
    }
    
    /**
     * Helper data class for tuple return
     */
    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun toFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            buffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        buffer.rewind()
        return buffer
    }

    private fun toUint8Buffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte())
            buffer.put(((pixel shr 8) and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }
        buffer.rewind()
        return buffer
    }

    override fun close() = interpreter.close()
}
