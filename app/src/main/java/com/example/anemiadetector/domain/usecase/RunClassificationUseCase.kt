package com.example.anemiadetector.domain.usecase

import android.graphics.Bitmap
import com.example.anemiadetector.data.model.ClassificationResult
import com.example.anemiadetector.data.repository.InferenceRepository
import javax.inject.Inject

/**
 * Use case for running anemia classification
 */
class RunClassificationUseCase @Inject constructor(
    private val repository: InferenceRepository
) {
    suspend fun execute(conjunctivaCrop: Bitmap): ClassificationResult {
        return repository.classify(conjunctivaCrop)
    }
}
