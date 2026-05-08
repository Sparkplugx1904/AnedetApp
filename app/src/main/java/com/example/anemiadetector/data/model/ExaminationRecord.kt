package com.example.anemiadetector.data.model

import android.graphics.Bitmap

/**
 * Domain model for examination record (for UI layer)
 */
data class ExaminationRecord(
    val id: Long,
    val timestamp: Long,
    val labelAnemia: Float,
    val labelNonAnemia: Float,
    val predictedLabel: String,
    val confidence: Float,
    val imagePath: String,
    val mode: String,
    val thumbnail: Bitmap? = null
)
