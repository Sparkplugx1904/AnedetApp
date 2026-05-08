package com.example.anemiadetector.di

import android.content.Context
import com.example.anemiadetector.data.repository.InferenceRepository
import com.example.anemiadetector.data.repository.InferenceRepositoryImpl
import com.example.anemiadetector.ml.classification.AnemiaClassifier
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideConjunctivaSegmentor(
        @ApplicationContext context: Context
    ): ConjunctivaSegmentor {
        return ConjunctivaSegmentor(context)
    }

    @Provides
    @Singleton
    fun provideAnemiaClassifier(
        @ApplicationContext context: Context
    ): AnemiaClassifier {
        return AnemiaClassifier(context)
    }

    @Provides
    @Singleton
    fun provideInferenceRepository(
        impl: InferenceRepositoryImpl
    ): InferenceRepository {
        return impl
    }
}
