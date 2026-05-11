package com.example.anemiadetector.ml.classification

import android.content.Context
import android.graphics.Bitmap
import com.example.anemiadetector.utils.loadModelBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnemiaClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {

    companion object {
        private const val MODEL_PATH = "models/classify/best_float32.tflite"
        private val CLASS_NAMES = mapOf(0 to "Anemia", 1 to "Non-Anemia")
    }

    private val interpreter: Interpreter
    val inputSize: Int

    data class ClassificationResult(
        val label: String,
        val confidence: Float,
        val allScores: FloatArray,
        val isAnemic: Boolean
    )

    init {
        // Force disable GMS client - use bundled TFLite only
        System.setProperty("tflite.disable_gms_client", "true")
        
        // Model FLOAT32 classification bisa pakai GPU delegate untuk performa lebih baik
        // Berbeda dengan segmentation INT8 yang harus CPU-only
        val options = Interpreter.Options().apply { 
            setNumThreads(4)
            // FLOAT32 model compatible dengan NNAPI/GPU delegate
            // Tapi untuk konsistensi dan stabilitas, kita pakai CPU-only dulu
            setUseNNAPI(false)
        }
        interpreter = Interpreter(loadModelBuffer(context, MODEL_PATH), options)

        val inputShape = interpreter.getInputTensor(0).shape()
        // Input shape usually [1, H, W, 3] or [1, 3, H, W]
        inputSize = if (inputShape[1] == 3) {
            inputShape[2] // NCHW
        } else {
            inputShape[1] // NHWC
        }
        Log.d("AnemiaClassifier", "Model input shape: ${inputShape.contentToString()}, selected inputSize: $inputSize")
    }

    fun classify(conjunctivaCrop: Bitmap): ClassificationResult {
        val resized = Bitmap.createScaledBitmap(conjunctivaCrop, inputSize, inputSize, true)
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        val output = Array(1) { FloatArray(2) }
        interpreter.run(inputBuffer, output)
        val scores = output[0]
        val idx = scores.indices.maxByOrNull { scores[it] } ?: 0
        val label = CLASS_NAMES[idx] ?: "Unknown"
        resized.recycle()
        return ClassificationResult(label, scores[idx], scores, idx == 0)
    }

    override fun close() = interpreter.close()
}
