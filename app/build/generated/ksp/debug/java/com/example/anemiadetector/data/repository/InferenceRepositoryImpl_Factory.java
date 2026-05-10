package com.example.anemiadetector.data.repository;

import com.example.anemiadetector.domain.usecase.RunPreprocessingUseCase;
import com.example.anemiadetector.domain.usecase.RunSegmentationPreprocessingUseCase;
import com.example.anemiadetector.ml.classification.AnemiaClassifier;
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class InferenceRepositoryImpl_Factory implements Factory<InferenceRepositoryImpl> {
  private final Provider<RunPreprocessingUseCase> preprocessingUseCaseProvider;

  private final Provider<RunSegmentationPreprocessingUseCase> segmentationPreprocessingUseCaseProvider;

  private final Provider<ConjunctivaSegmentor> segmentorProvider;

  private final Provider<AnemiaClassifier> classifierProvider;

  public InferenceRepositoryImpl_Factory(
      Provider<RunPreprocessingUseCase> preprocessingUseCaseProvider,
      Provider<RunSegmentationPreprocessingUseCase> segmentationPreprocessingUseCaseProvider,
      Provider<ConjunctivaSegmentor> segmentorProvider,
      Provider<AnemiaClassifier> classifierProvider) {
    this.preprocessingUseCaseProvider = preprocessingUseCaseProvider;
    this.segmentationPreprocessingUseCaseProvider = segmentationPreprocessingUseCaseProvider;
    this.segmentorProvider = segmentorProvider;
    this.classifierProvider = classifierProvider;
  }

  @Override
  public InferenceRepositoryImpl get() {
    return newInstance(preprocessingUseCaseProvider.get(), segmentationPreprocessingUseCaseProvider.get(), segmentorProvider.get(), classifierProvider.get());
  }

  public static InferenceRepositoryImpl_Factory create(
      Provider<RunPreprocessingUseCase> preprocessingUseCaseProvider,
      Provider<RunSegmentationPreprocessingUseCase> segmentationPreprocessingUseCaseProvider,
      Provider<ConjunctivaSegmentor> segmentorProvider,
      Provider<AnemiaClassifier> classifierProvider) {
    return new InferenceRepositoryImpl_Factory(preprocessingUseCaseProvider, segmentationPreprocessingUseCaseProvider, segmentorProvider, classifierProvider);
  }

  public static InferenceRepositoryImpl newInstance(RunPreprocessingUseCase preprocessingUseCase,
      RunSegmentationPreprocessingUseCase segmentationPreprocessingUseCase,
      ConjunctivaSegmentor segmentor, AnemiaClassifier classifier) {
    return new InferenceRepositoryImpl(preprocessingUseCase, segmentationPreprocessingUseCase, segmentor, classifier);
  }
}
