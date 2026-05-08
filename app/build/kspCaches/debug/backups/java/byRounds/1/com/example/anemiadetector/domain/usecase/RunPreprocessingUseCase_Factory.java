package com.example.anemiadetector.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class RunPreprocessingUseCase_Factory implements Factory<RunPreprocessingUseCase> {
  @Override
  public RunPreprocessingUseCase get() {
    return newInstance();
  }

  public static RunPreprocessingUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RunPreprocessingUseCase newInstance() {
    return new RunPreprocessingUseCase();
  }

  private static final class InstanceHolder {
    private static final RunPreprocessingUseCase_Factory INSTANCE = new RunPreprocessingUseCase_Factory();
  }
}
