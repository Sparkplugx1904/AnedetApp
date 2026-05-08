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
 */
@Singleton
class InferenceRepositoryImpl @Inject constructor(
    private val preprocessingUseCase: RunPreprocessingUseCase,
    private val segmentor: ConjunctivaSegmentor,
    private val classifier: AnemiaClassifier
) : InferenceRepository {

    // TFLite Interpreter is NOT thread-safe, use Mutex for sequential access
    private val segmentationMutex = Mutex()
    private val classificationMutex = Mutex()

    override suspend fun preprocess(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        preprocessingUseCase.execute(bitmap)
    }

    override suspend fun segment(
        preprocessedBitmap: Bitmap,
        originalWidth: Int,
        originalHeight: Int
    ): DetectionResult? = withContext(Dispatchers.Default) {
        segmentationMutex.withLock {
            val result = segmentor.segment(preprocessedBitmap, originalWidth, originalHeight)
            result?.let {
                // Convert ConjunctivaSegmentor.SegmentationResult to DetectionResult
                // Generate mask bitmap with alpha fill
                val maskBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
                val color = PolygonUtils.getStatusColor(null) // Blue for segmentation only
                val maskedBitmap = PolygonUtils.fillPolygonAlpha(maskBitmap, it.polygon, color, 77)
                
                DetectionResult(
                    polygon = it.polygon,
                    boundingBox = it.boundingBox,
                    maskBitmap = maskedBitmap,
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
