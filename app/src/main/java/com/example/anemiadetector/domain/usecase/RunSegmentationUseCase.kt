package com.example.anemiadetector.domain.usecase

import android.graphics.Bitmap
import com.example.anemiadetector.data.model.DetectionResult
import com.example.anemiadetector.data.repository.InferenceRepository
import javax.inject.Inject

/**
 * Use case for running conjunctiva segmentation
 */
class RunSegmentationUseCase @Inject constructor(
    private val repository: InferenceRepository
) {
    suspend fun execute(
        preprocessedBitmap: Bitmap,
        originalWidth: Int,
        originalHeight: Int
    ): DetectionResult? {
        return repository.segment(preprocessedBitmap, originalWidth, originalHeight)
    }
}
