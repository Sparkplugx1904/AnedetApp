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
public final class GetHistoryUseCase_Factory implements Factory<GetHistoryUseCase> {
  private final Provider<ExaminationRepository> examinationRepositoryProvider;

  public GetHistoryUseCase_Factory(Provider<ExaminationRepository> examinationRepositoryProvider) {
    this.examinationRepositoryProvider = examinationRepositoryProvider;
  }

  @Override
  public GetHistoryUseCase get() {
    return newInstance(examinationRepositoryProvider.get());
  }

  public static GetHistoryUseCase_Factory create(
      Provider<ExaminationRepository> examinationRepositoryProvider) {
    return new GetHistoryUseCase_Factory(examinationRepositoryProvider);
  }

  public static GetHistoryUseCase newInstance(ExaminationRepository examinationRepository) {
    return new GetHistoryUseCase(examinationRepository);
  }
}
