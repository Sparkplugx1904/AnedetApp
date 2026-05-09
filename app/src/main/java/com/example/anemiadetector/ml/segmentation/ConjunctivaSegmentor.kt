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
        private const val MODEL_PATH = "models/segments/best_int8.tflite"
    }

    data class SegmentationResult(
        val polygon: List<PointF>,
        val boundingBox: RectF,
        val confidence: Float
    )

    private val interpreter: Interpreter = run {
        // Force disable GMS client - use bundled TFLite only
        System.setProperty("tflite.disable_gms_client", "true")
        
        Interpreter(
            loadModelBuffer(context, MODEL_PATH),
            Interpreter.Options().apply { 
                numThreads = 4
                // Explicitly disable NNAPI and GPU delegates
                setUseNNAPI(false)
            }
        ).also {
            for (i in 0 until it.outputTensorCount) {
                Log.d("ModelInfo", "Output $i shape: ${it.getOutputTensor(i).shape().contentToString()}")
                Log.d("ModelInfo", "Output $i dtype: ${it.getOutputTensor(i).dataType()}")
            }
        }
    }

    fun segment(preprocessedBitmap: Bitmap, originalWidth: Int, originalHeight: Int): SegmentationResult? {
        val resized = Bitmap.createScaledBitmap(preprocessedBitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputType = interpreter.getInputTensor(0).dataType()
        val input = if (inputType == DataType.UINT8 || inputType == DataType.INT8) {
            toUint8Buffer(resized)
        } else {
            toFloatBuffer(resized)
        }

        // Placeholder parser: output format model nms-embedded perlu dicek via logcat.
        // Agar aplikasi tetap jalan saat parser belum final, fungsi mengembalikan null.
        val output0 = Array(1) { FloatArray(8400 * 6) }
        try {
            interpreter.run(input, output0)
        } catch (e: Exception) {
            Log.e("Segmentor", "Inference failed", e)
        } finally {
            resized.recycle()
        }
        return null
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
