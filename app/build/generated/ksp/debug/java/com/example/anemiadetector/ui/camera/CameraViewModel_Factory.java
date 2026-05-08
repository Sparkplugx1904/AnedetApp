package com.example.anemiadetector.ui.camera;

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

  public CameraViewModel_Factory(Provider<InferenceRepository> inferenceRepositoryProvider) {
    this.inferenceRepositoryProvider = inferenceRepositoryProvider;
  }

  @Override
  public CameraViewModel get() {
    return newInstance(inferenceRepositoryProvider.get());
  }

  public static CameraViewModel_Factory create(
      Provider<InferenceRepository> inferenceRepositoryProvider) {
    return new CameraViewModel_Factory(inferenceRepositoryProvider);
  }

  public static CameraViewModel newInstance(InferenceRepository inferenceRepository) {
    return new CameraViewModel(inferenceRepository);
  }
}
