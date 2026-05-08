package com.example.anemiadetector.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

fun generateMaskedBitmap(
    originalFrame: Bitmap,
    polygon: List<PointF>,
    isAnemic: Boolean?,
    originalFrameWidth: Int,
    originalFrameHeight: Int
): Bitmap {
    val result = originalFrame.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val color = when (isAnemic) {
        true -> Color.parseColor("#FFFF3B30")
        false -> Color.parseColor("#FF34C759")
        null -> Color.parseColor("#FF007AFF")
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        alpha = if (isAnemic == null) 64 else 77
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        alpha = 255
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    val path = Path()
    polygon.forEachIndexed { index, pt ->
        if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    path.close()
    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
    return result
}
