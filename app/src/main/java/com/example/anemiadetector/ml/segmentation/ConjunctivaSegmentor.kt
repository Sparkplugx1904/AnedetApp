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

        // Model FP16 NMS-embedded output shape: [1, 300, 38]
        // Format: [batch, max_detections, data]
        // data = [x1, y1, x2, y2, confidence, class_id, mask_coeffs(32)]
        val output0 = Array(1) { Array(300) { FloatArray(38) } }
        
        try {
            interpreter.run(input, output0)
            Log.d("ConjunctivaSegmentor", "Inference success, output shape: [1, 300, 38]")
            
            // Parse output untuk extract detection
            val result = parseOutput(output0, originalWidth, originalHeight)
            if (result != null) {
                Log.d("ConjunctivaSegmentor", "Detection found: confidence=${result.confidence}")
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
        originalWidth: Int,
        originalHeight: Int
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
            
            // Convert bbox to polygon (simple rectangle for now)
            // TODO: Decode mask coefficients untuk polygon yang lebih akurat
            val polygon = listOf(
                PointF(x1Denorm, y1Denorm),
                PointF(x2Denorm, y1Denorm),
                PointF(x2Denorm, y2Denorm),
                PointF(x1Denorm, y2Denorm)
            )
            
            Log.d("ConjunctivaSegmentor", "Detection: bbox=$bbox, conf=$confidence, class=$classId")
            
            return SegmentationResult(
                polygon = polygon,
                boundingBox = bbox,
                confidence = confidence
            )
        }
        
        return null  // No detection above threshold
    }

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
