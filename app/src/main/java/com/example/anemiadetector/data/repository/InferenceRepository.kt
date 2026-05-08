package com.example.anemiadetector.data.repository

import android.graphics.Bitmap
import com.example.anemiadetector.data.model.ClassificationResult
import com.example.anemiadetector.data.model.DetectionResult

/**
 * Repository interface for inference operations
 */
interface InferenceRepository {
    /**
     * Run full preprocessing pipeline on input bitmap
     * @param bitmap Input bitmap from camera
     * @return Preprocessed bitmap (224x224, CLAHE enhanced)
     */
    suspend fun preprocess(bitmap: Bitmap): Bitmap

    /**
     * Run conjunctiva segmentation
     * @param preprocessedBitmap Preprocessed bitmap (224x224)
     * @param originalWidth Original frame width
     * @param originalHeight Original frame height
     * @return DetectionResult or null if no detection
     */
    suspend fun segment(
        preprocessedBitmap: Bitmap,
        originalWidth: Int,
        originalHeight: Int
    ): DetectionResult?

    /**
     * Run anemia classification on cropped conjunctiva
     * @param conjunctivaCrop Cropped conjunctiva region
     * @return ClassificationResult
     */
    suspend fun classify(conjunctivaCrop: Bitmap): ClassificationResult

    /**
     * Release resources
     */
    fun release()
}
