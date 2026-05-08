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
public final class RunClassificationUseCase_Factory implements Factory<RunClassificationUseCase> {
  private final Provider<InferenceRepository> repositoryProvider;

  public RunClassificationUseCase_Factory(Provider<InferenceRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RunClassificationUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RunClassificationUseCase_Factory create(
      Provider<InferenceRepository> repositoryProvider) {
    return new RunClassificationUseCase_Factory(repositoryProvider);
  }

  public static RunClassificationUseCase newInstance(InferenceRepository repository) {
    return new RunClassificationUseCase(repository);
  }
}
