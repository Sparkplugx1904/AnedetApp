package com.example.anemiadetector.data.model

/**
 * Sealed class representing the state of inference pipeline
 */
sealed class InferenceState {
    data object Idle : InferenceState()
    data object Processing : InferenceState()
    data class Success(
        val detectionResult: DetectionResult,
        val classificationResult: ClassificationResult?
    ) : InferenceState()
    data class NoDetection(val message: String = "Konjungtiva tidak terdeteksi") : InferenceState()
    data class Error(val message: String, val throwable: Throwable? = null) : InferenceState()
}
