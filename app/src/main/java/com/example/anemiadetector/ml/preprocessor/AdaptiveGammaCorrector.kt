package com.example.anemiadetector.ml.preprocessor

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

object AdaptiveGammaCorrector {
    private const val GAMMA_MIN = 0.5f
    private const val GAMMA_MAX = 1.2f

    fun apply(bitmap: Bitmap): Bitmap {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2BGR)

        val lab = Mat()
        Imgproc.cvtColor(rgba, lab, Imgproc.COLOR_BGR2Lab)
        val channels = mutableListOf<Mat>()
        org.opencv.core.Core.split(lab, channels)
        val meanL = org.opencv.core.Core.mean(channels[0]).`val`[0] / 255.0

        val gamma = (GAMMA_MIN + (GAMMA_MAX - GAMMA_MIN) * (meanL / 0.9))
            .coerceIn(GAMMA_MIN.toDouble(), GAMMA_MAX.toDouble())
        val lut = Mat(1, 256, CvType.CV_8UC1)
        for (i in 0..255) {
            val value = ((i / 255.0).pow(gamma.toDouble()) * 255.0).coerceIn(0.0, 255.0)
            lut.put(0, i, value)
        }

        val corrected = Mat()
        org.opencv.core.Core.LUT(rgba, lut, corrected)
        Imgproc.cvtColor(corrected, corrected, Imgproc.COLOR_BGR2RGBA)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(corrected, result)

        rgba.release()
        lab.release()
        lut.release()
        corrected.release()
        channels.forEach { it.release() }
        return result
    }
}
