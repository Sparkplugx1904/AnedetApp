package com.example.anemiadetector.ui.camera;

import com.example.anemiadetector.data.repository.ExaminationRepository;
import com.example.anemiadetector.data.repository.InferenceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class CameraViewModel_Factory implements Factory<CameraViewModel> {
  private final Provider<InferenceRepository> inferenceRepositoryProvider;

  private final Provider<ExaminationRepository> examinationRepositoryProvider;

  public CameraViewModel_Factory(Provider<InferenceRepository> inferenceRepositoryProvider,
      Provider<ExaminationRepository> examinationRepositoryProvider) {
    this.inferenceRepositoryProvider = inferenceRepositoryProvider;
    this.examinationRepositoryProvider = examinationRepositoryProvider;
  }

  @Override
  public CameraViewModel get() {
    return newInstance(inferenceRepositoryProvider.get(), examinationRepositoryProvider.get());
  }

  public static CameraViewModel_Factory create(
      Provider<InferenceRepository> inferenceRepositoryProvider,
      Provider<ExaminationRepository> examinationRepositoryProvider) {
    return new CameraViewModel_Factory(inferenceRepositoryProvider, examinationRepositoryProvider);
  }

  public static CameraViewModel newInstance(InferenceRepository inferenceRepository,
      ExaminationRepository examinationRepository) {
    return new CameraViewModel(inferenceRepository, examinationRepository);
  }
}
