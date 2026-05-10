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
public final class RunSegmentationPreprocessingUseCase_Factory implements Factory<RunSegmentationPreprocessingUseCase> {
  @Override
  public RunSegmentationPreprocessingUseCase get() {
    return newInstance();
  }

  public static RunSegmentationPreprocessingUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RunSegmentationPreprocessingUseCase newInstance() {
    return new RunSegmentationPreprocessingUseCase();
  }

  private static final class InstanceHolder {
    private static final RunSegmentationPreprocessingUseCase_Factory INSTANCE = new RunSegmentationPreprocessingUseCase_Factory();
  }
}
