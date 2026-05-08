package com.example.anemiadetector.ml.preprocessor

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object BilateralFilterProcessor {
    private const val KERNEL_DIAMETER = 9
    private const val SIGMA_COLOR = 25.5
    private const val SIGMA_SPACE = 1.5

    fun apply(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        val filtered = Mat()
        Imgproc.bilateralFilter(mat, filtered, KERNEL_DIAMETER, SIGMA_COLOR, SIGMA_SPACE)
        Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_BGR2RGBA)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(filtered, result)
        mat.release()
        filtered.release()
        return result
    }
}
