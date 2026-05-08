package com.example.anemiadetector.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * Utility functions for camera operations
 */
object CameraUtils {

    /**
     * Convert ImageProxy to Bitmap
     * Handles rotation and format conversion
     */
    fun ImageProxy.toBitmap(): Bitmap {
        val bitmap = this.toBitmap()
        
        // Handle rotation
        val rotationDegrees = this.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            rotateBitmap(bitmap, rotationDegrees.toFloat())
        } else {
            bitmap
        }
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * Check if enough time has passed for next inference
     * Used for FPS throttling
     */
    fun shouldProcessFrame(
        lastInferenceMs: Long,
        currentMs: Long,
        intervalMs: Long
    ): Boolean {
        return (currentMs - lastInferenceMs) >= intervalMs
    }

    /**
     * Calculate FPS from frame timestamps
     */
    fun calculateFps(frameTimestamps: List<Long>): Float {
        if (frameTimestamps.size < 2) return 0f
        
        val timeSpan = frameTimestamps.last() - frameTimestamps.first()
        return if (timeSpan > 0) {
            (frameTimestamps.size - 1) * 1000f / timeSpan
        } else {
            0f
        }
    }
}
