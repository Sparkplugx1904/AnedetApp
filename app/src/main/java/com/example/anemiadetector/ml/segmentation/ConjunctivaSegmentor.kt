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
        val resized = Bitmap.createScaledBitmap(preprocessedBitmap, INPUT_SIZE, INPUT_SIZE, true)
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
     */
    private fun parseOutput(
        output: Array<Array<FloatArray>>,
        protoMasksData: ProtoMasksData?,
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
                // JANGAN break, karena output model mungkin tidak terurut berdasarkan confidence
                continue
            }
            
            // Filter by class (0 = conjunctiva)
            if (classId != 0) continue
            
            // Robust denormalization: detect if coordinates are [0,1] or [0,320]
            val x1Denorm: Float
            val y1Denorm: Float
            val x2Denorm: Float
            val y2Denorm: Float

            if (x1 <= 1.0f && y1 <= 1.0f && x2 <= 1.1f && y2 <= 1.1f && (x1 != 0f || x2 != 0f)) {
                // Coordinates are likely normalized [0, 1]
                x1Denorm = x1 * originalWidth
                y1Denorm = y1 * originalHeight
                x2Denorm = x2 * originalWidth
                y2Denorm = y2 * originalHeight
                Log.d("ConjunctivaSegmentor", "Detected normalized coordinates: [$x1, $y1, $x2, $y2]")
            } else {
                // Coordinates are likely in pixel space [0, 320]
                x1Denorm = (x1 / INPUT_SIZE) * originalWidth
                y1Denorm = (y1 / INPUT_SIZE) * originalHeight
                x2Denorm = (x2 / INPUT_SIZE) * originalWidth
                y2Denorm = (y2 / INPUT_SIZE) * originalHeight
                Log.d("ConjunctivaSegmentor", "Detected pixel-space coordinates: [$x1, $y1, $x2, $y2]")
            }
            
            // Create bounding box
            val bbox = RectF(x1Denorm, y1Denorm, x2Denorm, y2Denorm)
            
            // Extract mask coefficients (32 values from index 6-37)
            val maskCoeffs = FloatArray(32) { idx -> detection[6 + idx] }
            
            // Decode polygon from mask coefficients if proto masks available
            val polygon = if (protoMasksData != null) {
                try {
                    decodeMaskToPolygon(
                        maskCoeffs,
                        protoMasksData,
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
     * FIXED: Handle both [1,32,H,W] and [1,H,W,32] formats correctly
     */
    private fun decodeMaskToPolygon(
        maskCoeffs: FloatArray,
        protoMasksData: ProtoMasksData,
        bbox: RectF,
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
