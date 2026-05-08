package com.example.anemiadetector.domain.usecase;

import com.example.anemiadetector.data.repository.ExaminationRepository;
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
public final class SaveExaminationUseCase_Factory implements Factory<SaveExaminationUseCase> {
  private final Provider<ExaminationRepository> examinationRepositoryProvider;

  public SaveExaminationUseCase_Factory(
      Provider<ExaminationRepository> examinationRepositoryProvider) {
    this.examinationRepositoryProvider = examinationRepositoryProvider;
  }

  @Override
  public SaveExaminationUseCase get() {
    return newInstance(examinationRepositoryProvider.get());
  }

  public static SaveExaminationUseCase_Factory create(
      Provider<ExaminationRepository> examinationRepositoryProvider) {
    return new SaveExaminationUseCase_Factory(examinationRepositoryProvider);
  }

  public static SaveExaminationUseCase newInstance(ExaminationRepository examinationRepository) {
    return new SaveExaminationUseCase(examinationRepository);
  }
}
