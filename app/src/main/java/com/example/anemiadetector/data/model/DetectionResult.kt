package com.example.anemiadetector.data.model

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF

/**
 * Result from conjunctiva segmentation
 * @param polygon 6-15 points in original frame coordinates
 * @param boundingBox Bounding box in original frame coordinates
 * @param maskBitmap Binary mask with alpha fill for overlay
 * @param confidence Detection confidence score
 * @param frameWidth Original frame width
 * @param frameHeight Original frame height
 */
data class DetectionResult(
    val polygon: List<PointF>,
    val boundingBox: RectF,
    val maskBitmap: Bitmap,
    val confidence: Float,
    val frameWidth: Int,
    val frameHeight: Int
)
