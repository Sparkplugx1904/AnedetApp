package com.example.anemiadetector.ml.preprocessor

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object AdaptiveCLAHEProcessor {
    private const val CLAHE_CLIP_MIN = 8.0
    private const val CLAHE_CLIP_MAX = 25.0
    private val TILE_GRID = Size(8.0, 8.0)

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        val labMat = Mat()
        Imgproc.cvtColor(mat, labMat, Imgproc.COLOR_BGR2Lab)
        val channels = mutableListOf<Mat>()
        Core.split(labMat, channels)
        
        // CRITICAL FIX: Keep reference to original L channel for proper cleanup
        val lChannelOriginal = channels[0]
        
        val meanStd = MatOfDouble()
        Core.meanStdDev(lChannelOriginal, MatOfDouble(), meanStd)
        val stdL = meanStd.get(0, 0)[0]

        val clipLimit = (CLAHE_CLIP_MAX - (CLAHE_CLIP_MAX - CLAHE_CLIP_MIN) * (stdL / (0.20 * 255.0)))
            .coerceIn(CLAHE_CLIP_MIN, CLAHE_CLIP_MAX)
        val clahe = Imgproc.createCLAHE(clipLimit, TILE_GRID)
        val lEnhanced = Mat()
        clahe.apply(lChannelOriginal, lEnhanced)
        
        // Replace L channel with enhanced version
        channels[0] = lEnhanced

        Core.merge(channels, labMat)
        Imgproc.cvtColor(labMat, mat, Imgproc.COLOR_Lab2BGR)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)

        // Cleanup - release ALL Mat objects including original L channel
        mat.release()
        labMat.release()
        meanStd.release()
        lChannelOriginal.release()  // CRITICAL: Release original L channel
        channels.forEach { it.release() }  // This releases lEnhanced, a, b
        
        return result
    }
}
