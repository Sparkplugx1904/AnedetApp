package com.example.anemiadetector.ml.preprocessor

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object GrayWorldWhiteBalance {
    private const val WB_STRENGTH = 0.8f
    private const val SCALE_MIN = 0.5f
    private const val SCALE_MAX = 1.8f

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)

        val floatMat = Mat()
        mat.convertTo(floatMat, CvType.CV_32FC3)
        val channels = mutableListOf<Mat>()
        Core.split(floatMat, channels)

        val meanB = Core.mean(channels[0]).`val`[0]
        val meanG = Core.mean(channels[1]).`val`[0]
        val meanR = Core.mean(channels[2]).`val`[0]
        val meanGray = (meanB + meanG + meanR) / 3.0

        if (meanGray < 1e-6) {
            mat.release()
            floatMat.release()
            channels.forEach { it.release() }
            return bitmap
        }

        fun computeScale(meanCh: Double): Float {
            val rawScale = (meanGray / (meanCh + 1e-6)).toFloat()
            val clipped = rawScale.coerceIn(SCALE_MIN, SCALE_MAX)
            return 1.0f + (clipped - 1.0f) * WB_STRENGTH
        }

        Core.multiply(channels[0], Scalar(computeScale(meanB).toDouble()), channels[0])
        Core.multiply(channels[1], Scalar(computeScale(meanG).toDouble()), channels[1])
        Core.multiply(channels[2], Scalar(computeScale(meanR).toDouble()), channels[2])

        Core.merge(channels, floatMat)
        Core.min(floatMat, Scalar(255.0, 255.0, 255.0), floatMat)
        Core.max(floatMat, Scalar(0.0, 0.0, 0.0), floatMat)

        floatMat.convertTo(mat, CvType.CV_8UC3)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGBA)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, result)

        mat.release()
        floatMat.release()
        channels.forEach { it.release() }
        return result
    }
}
