package com.example.anemiadetector.domain.usecase

import android.graphics.Bitmap
import com.example.anemiadetector.ml.preprocessor.AdaptiveCLAHEProcessor
import com.example.anemiadetector.ml.preprocessor.AdaptiveGammaCorrector
import com.example.anemiadetector.ml.preprocessor.BilateralFilterProcessor
import com.example.anemiadetector.ml.preprocessor.GrayWorldWhiteBalance
import com.example.anemiadetector.ml.preprocessor.LetterboxResizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunPreprocessingUseCase @Inject constructor() {
    fun run(input: Bitmap): Bitmap {
        val wb = GrayWorldWhiteBalance.apply(input)
        val gamma = AdaptiveGammaCorrector.apply(wb)
        if (wb != input && !wb.isRecycled) wb.recycle()
        val letterbox = LetterboxResizer.resize(gamma, 224)
        if (!gamma.isRecycled) gamma.recycle()
        val bilateral = BilateralFilterProcessor.apply(letterbox)
        if (!letterbox.isRecycled) letterbox.recycle()
        val clahe = AdaptiveCLAHEProcessor.apply(bilateral)
        if (!bilateral.isRecycled) bilateral.recycle()
        return clahe
    }
}
