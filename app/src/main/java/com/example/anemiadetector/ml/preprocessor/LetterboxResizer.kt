package com.example.anemiadetector.ml.preprocessor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

object LetterboxResizer {
    fun resize(bitmap: Bitmap, targetSize: Int): Bitmap {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val scale = targetSize.toFloat() / maxOf(srcW, srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        val xOff = (targetSize - newW) / 2
        val yOff = (targetSize - newH) / 2
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        canvas.drawBitmap(scaled, xOff.toFloat(), yOff.toFloat(), null)
        scaled.recycle()
        return output
    }
}
