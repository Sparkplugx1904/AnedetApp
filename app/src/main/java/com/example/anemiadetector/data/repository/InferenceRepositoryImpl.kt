package com.example.anemiadetector.data.repository

import android.graphics.Bitmap
import com.example.anemiadetector.data.model.ClassificationResult
import com.example.anemiadetector.data.model.DetectionResult
import com.example.anemiadetector.domain.usecase.RunPreprocessingUseCase
import com.example.anemiadetector.ml.classification.AnemiaClassifier
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor
import com.example.anemiadetector.utils.PolygonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of InferenceRepository
 * Thread-safe inference operations using Mutex
 * 
 * FIXED: Separate preprocessing paths for segmentation and classification
 * - Segmentation: WB → Gamma → Bilateral → CLAHE (NO letterbox, segmentor resize to 320)
 * - Classification: WB → Gamma → Letterbox 224 → Bilateral → CLAHE
 */
@Singleton
class InferenceRepositoryImpl @Inject constructor(
    private val preprocessingUseCase: RunPreprocessingUseCase,
    private val segmentationPreprocessingUseCase: com.example.anemiadetector.domain.usecase.RunSegmentationPreprocessingUseCase,
    private val segmentor: ConjunctivaSegmentor,
    private val classifier: AnemiaClassifier
) : InferenceRepository {

    // TFLite Interpreter is NOT thread-safe, use Mutex for sequential access
    private val segmentationMutex = Mutex()
    private val classificationMutex = Mutex()

    override suspend fun preprocess(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // For classification: full preprocessing with letterbox to 224
        preprocessingUseCase.execute(bitmap)
    }
    
    override suspend fun preprocessForSegmentation(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // For segmentation: NO letterbox, segmentor will resize to 320 internally
        segmentationPreprocessingUseCase.execute(bitmap)
    }

    override suspend fun segment(
        preprocessedBitmap: Bitmap,
        originalWidth: Int,
        originalHeight: Int
    ): DetectionResult? = withContext(Dispatchers.Default) {
        segmentationMutex.withLock {
            // CRITICAL: preprocessedBitmap here should be from segmentation preprocessing
            // (no letterbox), not classification preprocessing (letterbox 224)
            val result = segmentor.segment(preprocessedBitmap, originalWidth, originalHeight)
            result?.let {
                // Convert ConjunctivaSegmentor.SegmentationResult to DetectionResult
                // NOTE: maskBitmap is not used in UI (overlay drawn directly on Canvas)
                // If needed in future, pass originalFrame here instead of creating empty bitmap
                DetectionResult(
                    polygon = it.polygon,
                    boundingBox = it.boundingBox,
                    maskBitmap = null,  // Not used, set to null to save memory
                    confidence = it.confidence
                )
            }
        }
    }

    override suspend fun classify(conjunctivaCrop: Bitmap): ClassificationResult =
        withContext(Dispatchers.Default) {
            classificationMutex.withLock {
                val result = classifier.classify(conjunctivaCrop)
                // Convert AnemiaClassifier.ClassificationResult to data.model.ClassificationResult
                ClassificationResult(
                    label = result.label,
                    confidence = result.confidence,
                    allScores = result.allScores,
                    isAnemic = result.isAnemic
                )
            }
        }

    override fun release() {
        segmentor.close()
        classifier.close()
    }
}
