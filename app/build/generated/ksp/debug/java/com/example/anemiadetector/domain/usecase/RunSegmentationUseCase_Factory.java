package com.example.anemiadetector.domain.usecase;

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
public final class RunSegmentationUseCase_Factory implements Factory<RunSegmentationUseCase> {
  private final Provider<InferenceRepository> repositoryProvider;

  public RunSegmentationUseCase_Factory(Provider<InferenceRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RunSegmentationUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RunSegmentationUseCase_Factory create(
      Provider<InferenceRepository> repositoryProvider) {
    return new RunSegmentationUseCase_Factory(repositoryProvider);
  }

  public static RunSegmentationUseCase newInstance(InferenceRepository repository) {
    return new RunSegmentationUseCase(repository);
  }
}
