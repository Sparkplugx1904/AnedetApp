package com.example.anemiadetector.domain.usecase

import android.graphics.Bitmap
import com.example.anemiadetector.ml.preprocessor.AdaptiveCLAHEProcessor
import com.example.anemiadetector.ml.preprocessor.AdaptiveGammaCorrector
import com.example.anemiadetector.ml.preprocessor.BilateralFilterProcessor
import com.example.anemiadetector.ml.preprocessor.GrayWorldWhiteBalance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preprocessing for segmentation model
 * 
 * CRITICAL: NO letterbox resize!
 * Segmentation model (320x320) needs high-quality input.
 * Segmentor will handle resize internally from original frame to 320x320.
 * 
 * Pipeline: WB → Gamma → Bilateral → CLAHE (NO letterbox)
 */
@Singleton
class RunSegmentationPreprocessingUseCase @Inject constructor() {
    fun execute(input: Bitmap): Bitmap {
        // White balance
        val wb = GrayWorldWhiteBalance.apply(input)
        
        // Gamma correction
        val gamma = AdaptiveGammaCorrector.apply(wb)
        if (wb != input && !wb.isRecycled) wb.recycle()
        
        // Bilateral filter
        val bilateral = BilateralFilterProcessor.apply(gamma)
        if (!gamma.isRecycled) gamma.recycle()
        
        // CLAHE
        val clahe = AdaptiveCLAHEProcessor.apply(bilateral)
        if (!bilateral.isRecycled) bilateral.recycle()
        
        return clahe
    }
}
