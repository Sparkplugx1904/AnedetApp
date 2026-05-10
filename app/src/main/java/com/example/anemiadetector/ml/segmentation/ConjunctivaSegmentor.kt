package com.example.anemiadetector.ml.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.example.anemiadetector.ml.preprocessor.LetterboxResizer
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
    
    /**
     * Proto mask format enum
     */
    private enum class ProtoMaskFormat {
        CHANNELS_FIRST,  // [1, 32, H, W]
        CHANNELS_LAST    // [1, H, W, 32]
    }
    
    /**
     * Proto masks data with format information
     */
    private data class ProtoMasksData(
        val buffer: Any,
        val format: ProtoMaskFormat,
        val height: Int,
        val width: Int,
        val channels: Int
    )

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
        // CRITICAL: Use letterbox resize, NOT stretch
        // This maintains aspect ratio and adds black padding
        val resized = LetterboxResizer.resize(preprocessedBitmap, INPUT_SIZE)
        // FP16 model selalu pakai float buffer (input FLOAT32)
        val input = toFloatBuffer(resized)

        // Model FP16 NMS-embedded output
        // Output 0: [1, 300, 38] - detections with mask coefficients
        // Output 1 (if exists): proto masks for mask reconstruction
        val output0 = Array(1) { Array(300) { FloatArray(38) } }
        
        // Check if model has proto masks output (for proper segmentation)
        val hasProtoMasks = interpreter.outputTensorCount > 1
        val protoMasksData = if (hasProtoMasks) {
            // Proto masks typically [1, 32, mask_h, mask_w] or [1, mask_h, mask_w, 32]
            val protoShape = interpreter.getOutputTensor(1).shape()
            Log.d("ConjunctivaSegmentor", "Proto masks shape: ${protoShape.contentToString()}")
            
            // Allocate buffer based on actual shape
            // Common shapes: [1, 32, 160, 160] or [1, 160, 160, 32]
            when {
                protoShape.size == 4 && protoShape[1] == 32 -> {
                    // [1, 32, H, W]
                    val buffer = Array(1) { Array(32) { Array(protoShape[2]) { FloatArray(protoShape[3]) } } }
                    ProtoMasksData(buffer, ProtoMaskFormat.CHANNELS_FIRST, protoShape[2], protoShape[3], 32)
                }
                protoShape.size == 4 && protoShape[3] == 32 -> {
                    // [1, H, W, 32]
                    val buffer = Array(1) { Array(protoShape[1]) { Array(protoShape[2]) { FloatArray(32) } } }
                    ProtoMasksData(buffer, ProtoMaskFormat.CHANNELS_LAST, protoShape[1], protoShape[2], 32)
                }
                else -> null
            }
        } else {
            null
        }
        
        try {
            if (protoMasksData != null) {
                // Run inference with both outputs
                val outputs = mapOf(
                    0 to output0,
                    1 to protoMasksData.buffer
                )
                interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)
                Log.d("ConjunctivaSegmentor", "Inference success with proto masks")
            } else {
                // Run inference with single output
                interpreter.run(input, output0)
                Log.d("ConjunctivaSegmentor", "Inference success, output shape: [1, 300, 38]")
            }
            
            // Parse output untuk extract detection
            val result = parseOutput(output0, protoMasksData, originalWidth, originalHeight, resized)
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
     * 
     * FIXED: Apply inverse letterbox transform to coordinates
     */
    private fun parseOutput(
        output: Array<Array<FloatArray>>,
        protoMasksData: ProtoMasksData?,
        originalWidth: Int,
        originalHeight: Int,
        resizedBitmap: Bitmap
    ): SegmentationResult? {
        val detections = output[0]  // [300, 38]
        
        // Calculate letterbox parameters for inverse transform
        val letterboxParams = calculateLetterboxParams(originalWidth, originalHeight, INPUT_SIZE)
        
        Log.d("ConjunctivaSegmentor", "Letterbox params: scale=${letterboxParams.scale}, xOff=${letterboxParams.xOffset}, yOff=${letterboxParams.yOffset}")
        
        // Find best detection above threshold
        for (i in 0 until 300) {
            val detection = detections[i]
            
            // Extract data (normalized coordinates [0,1])
            val x1 = detection[0]
            val y1 = detection[1]
            val x2 = detection[2]
            val y2 = detection[3]
            val confidence = detection[4]
            val classId = detection[5].toInt()
            
            // Log first few detections for debugging
            if (i < 5) {
                Log.d("SegDebug", "Detection $i: conf=$confidence, x1=$x1, y1=$y1, x2=$x2, y2=$y2, class=$classId")
            }
            
            // Filter by confidence threshold
            if (confidence < CONF_THRESHOLD) {
                // Detections are sorted by confidence, so we can break early
                Log.d("ConjunctivaSegmentor", "Stopped at detection $i, confidence $confidence < $CONF_THRESHOLD")
                break
            }
            
            // Filter by class (0 = conjunctiva)
            if (classId != 0) continue
            
            // FIXED: Apply inverse letterbox transform to get correct frame coordinates
            val topLeft = modelToFrame(x1, y1, letterboxParams, INPUT_SIZE, originalWidth, originalHeight)
            val bottomRight = modelToFrame(x2, y2, letterboxParams, INPUT_SIZE, originalWidth, originalHeight)
            
            val x1Denorm = topLeft.x
            val y1Denorm = topLeft.y
            val x2Denorm = bottomRight.x
            val y2Denorm = bottomRight.y
            
            // Create bounding box
            val bbox = RectF(x1Denorm, y1Denorm, x2Denorm, y2Denorm)
            
            Log.d("ConjunctivaSegmentor", "Bbox after inverse letterbox: $bbox")
            
            // Extract mask coefficients (32 values from index 6-37)
            val maskCoeffs = FloatArray(32) { idx -> detection[6 + idx] }
            
            // Decode polygon from mask coefficients if proto masks available
            val polygon = if (protoMasksData != null) {
                try {
                    decodeMaskToPolygon(
                        maskCoeffs,
                        protoMasksData,
                        bbox,
                        letterboxParams,
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
     * FIXED: Handle both [1,32,H,W] and [1,H,W,32] formats correctly
     * FIXED: Apply inverse letterbox transform to contour coordinates
     */
    private fun decodeMaskToPolygon(
        maskCoeffs: FloatArray,
        protoMasksData: ProtoMasksData,
        bbox: RectF,
        letterboxParams: LetterboxParams,
        originalWidth: Int,
        originalHeight: Int
    ): List<PointF> {
        val protoHeight = protoMasksData.height
        val protoWidth = protoMasksData.width
        val protoChannels = protoMasksData.channels
        
        Log.d("ConjunctivaSegmentor", "Proto masks: ${protoChannels}x${protoHeight}x${protoWidth}, format=${protoMasksData.format}")
        
        // Extract proto data with correct indexing based on format
        val protoData = when (protoMasksData.format) {
            ProtoMaskFormat.CHANNELS_FIRST -> {
                // Format: [1, 32, H, W] → access as arr[0][c][y][x]
                val arr = protoMasksData.buffer as Array<Array<Array<FloatArray>>>
                Array(protoChannels) { c ->
                    Array(protoHeight) { y ->
                        FloatArray(protoWidth) { x ->
                            arr[0][c][y][x]
                        }
                    }
                }
            }
            ProtoMaskFormat.CHANNELS_LAST -> {
                // Format: [1, H, W, 32] → access as arr[0][y][x][c]
                val arr = protoMasksData.buffer as Array<Array<Array<FloatArray>>>
                Array(protoChannels) { c ->
                    Array(protoHeight) { y ->
                        FloatArray(protoWidth) { x ->
                            arr[0][y][x][c]  // ← CRITICAL FIX: correct indexing for CHANNELS_LAST
                        }
                    }
                }
            }
        }
        
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
        // bbox is in original frame coordinates, need to convert to proto space
        // Step 1: Frame → normalized [0,1]
        // Step 2: Normalized → INPUT_SIZE space (with letterbox)
        // Step 3: INPUT_SIZE → proto space
        val scaleX = protoWidth.toFloat() / INPUT_SIZE
        val scaleY = protoHeight.toFloat() / INPUT_SIZE
        
        // Convert bbox corners from frame to model normalized coordinates
        val bboxNormX1 = (bbox.left * letterboxParams.scale + letterboxParams.xOffset) / INPUT_SIZE
        val bboxNormY1 = (bbox.top * letterboxParams.scale + letterboxParams.yOffset) / INPUT_SIZE
        val bboxNormX2 = (bbox.right * letterboxParams.scale + letterboxParams.xOffset) / INPUT_SIZE
        val bboxNormY2 = (bbox.bottom * letterboxParams.scale + letterboxParams.yOffset) / INPUT_SIZE
        
        // Scale to proto space
        val bboxInProto = RectF(
            (bboxNormX1 * INPUT_SIZE * scaleX).coerceIn(0f, protoWidth.toFloat()),
            (bboxNormY1 * INPUT_SIZE * scaleY).coerceIn(0f, protoHeight.toFloat()),
            (bboxNormX2 * INPUT_SIZE * scaleX).coerceIn(0f, protoWidth.toFloat()),
            (bboxNormY2 * INPUT_SIZE * scaleY).coerceIn(0f, protoHeight.toFloat())
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
        
        // FIXED: Scale contour points back to original frame coordinates
        // using inverse letterbox transform
        val scaledContour = contourPoints.map { pt ->
            // pt is in proto mask coordinates (e.g., 0-160)
            // Convert to normalized model coordinates [0,1]
            val xNorm = pt.x / protoWidth
            val yNorm = pt.y / protoHeight
            // Apply inverse letterbox to get original frame coordinates
            modelToFrame(xNorm, yNorm, letterboxParams, INPUT_SIZE, originalWidth, originalHeight)
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
     * Helper data class for tuple return
     */
    private data class LetterboxParams(
        val scale: Float,
        val xOffset: Float,
        val yOffset: Float,
        val scaledWidth: Int,
        val scaledHeight: Int
    )
    
    /**
     * Calculate letterbox parameters for inverse transform
     * CRITICAL: Must match LetterboxResizer behavior exactly
     */
    private fun calculateLetterboxParams(
        originalWidth: Int,
        originalHeight: Int,
        targetSize: Int
    ): LetterboxParams {
        val scale = targetSize.toFloat() / maxOf(originalWidth, originalHeight)
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        val xOffset = (targetSize - scaledWidth) / 2f
        val yOffset = (targetSize - scaledHeight) / 2f
        
        return LetterboxParams(scale, xOffset, yOffset, scaledWidth, scaledHeight)
    }
    
    /**
     * Convert model normalized coordinates [0,1] to original frame coordinates
     * with inverse letterbox transform
     * 
     * FIXED: Account for letterbox padding offset
     */
    private fun modelToFrame(
        xNorm: Float,
        yNorm: Float,
        params: LetterboxParams,
        targetSize: Int,
        originalWidth: Int,
        originalHeight: Int
    ): PointF {
        // 1. Model norm [0,1] → target size space (e.g., 320x320) including padding
        val xTarget = xNorm * targetSize
        val yTarget = yNorm * targetSize
        
        // 2. Target space → original frame (inverse letterbox: subtract offset, divide by scale)
        val xOrig = (xTarget - params.xOffset) / params.scale
        val yOrig = (yTarget - params.yOffset) / params.scale
        
        // 3. Clamp to valid range
        return PointF(
            xOrig.coerceIn(0f, originalWidth.toFloat()),
            yOrig.coerceIn(0f, originalHeight.toFloat())
        )
    }
    
    /**
     * Extract contour from binary mask using OpenCV findContours
     * FIXED: Use proper contour tracing instead of raster scan
     */
    private fun extractContourFromMask(
        mask: Array<BooleanArray>,
        bbox: RectF,
        width: Int,
        height: Int
    ): List<PointF> {
        // Convert boolean mask to OpenCV Mat
        val x1 = bbox.left.toInt().coerceIn(0, width - 1)
        val y1 = bbox.top.toInt().coerceIn(0, height - 1)
        val x2 = bbox.right.toInt().coerceIn(0, width - 1)
        val y2 = bbox.bottom.toInt().coerceIn(0, height - 1)
        
        val roiWidth = (x2 - x1).coerceAtLeast(1)
        val roiHeight = (y2 - y1).coerceAtLeast(1)
        
        // Create binary Mat from mask ROI
        val binaryMat = org.opencv.core.Mat(roiHeight, roiWidth, org.opencv.core.CvType.CV_8UC1)
        for (y in 0 until roiHeight) {
            for (x in 0 until roiWidth) {
                val maskY = (y1 + y).coerceIn(0, height - 1)
                val maskX = (x1 + x).coerceIn(0, width - 1)
                val value = if (mask[maskY][maskX]) 255.0 else 0.0
                binaryMat.put(y, x, value)
            }
        }
        
        // Find contours using OpenCV
        val contours = mutableListOf<org.opencv.core.MatOfPoint>()
        val hierarchy = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.findContours(
            binaryMat,
            contours,
            hierarchy,
            org.opencv.imgproc.Imgproc.RETR_EXTERNAL,
            org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Find largest contour
        if (contours.isEmpty()) {
            Log.w("ConjunctivaSegmentor", "No contours found in mask")
            binaryMat.release()
            hierarchy.release()
            return emptyList()
        }
        
        val largestContour = contours.maxByOrNull { org.opencv.imgproc.Imgproc.contourArea(it) }
        
        if (largestContour == null || largestContour.empty()) {
            Log.w("ConjunctivaSegmentor", "Largest contour is empty")
            binaryMat.release()
            hierarchy.release()
            contours.forEach { it.release() }
            return emptyList()
        }
        
        // Convert MatOfPoint to List<PointF> with offset
        val contourPoints = mutableListOf<PointF>()
        val points = largestContour.toArray()
        for (point in points) {
            contourPoints.add(PointF(
                (point.x + x1).toFloat(),
                (point.y + y1).toFloat()
            ))
        }
        
        // Cleanup
        binaryMat.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        Log.d("ConjunctivaSegmentor", "Extracted contour with ${contourPoints.size} points")
        
        return contourPoints
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
