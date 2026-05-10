package com.example.anemiadetector.data.repository

import android.graphics.Bitmap
import com.example.anemiadetector.data.model.ClassificationResult
import com.example.anemiadetector.data.model.DetectionResult

/**
 * Repository interface for inference operations
 * 
 * FIXED: Separate preprocessing for segmentation and classification
 */
interface InferenceRepository {
    /**
     * Run full preprocessing pipeline for CLASSIFICATION
     * @param bitmap Input bitmap from camera
     * @return Preprocessed bitmap (224x224 letterboxed, CLAHE enhanced)
     */
    suspend fun preprocess(bitmap: Bitmap): Bitmap
    
    /**
     * Run preprocessing pipeline for SEGMENTATION (no letterbox)
     * @param bitmap Input bitmap from camera
     * @return Preprocessed bitmap (original size, CLAHE enhanced, no letterbox)
     */
    suspend fun preprocessForSegmentation(bitmap: Bitmap): Bitmap

    /**
     * Run conjunctiva segmentation
     * @param preprocessedBitmap Preprocessed bitmap (from preprocessForSegmentation)
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
