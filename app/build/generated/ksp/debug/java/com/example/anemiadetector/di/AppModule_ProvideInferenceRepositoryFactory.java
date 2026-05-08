package com.example.anemiadetector.di;

import com.example.anemiadetector.data.repository.InferenceRepository;
import com.example.anemiadetector.data.repository.InferenceRepositoryImpl;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideInferenceRepositoryFactory implements Factory<InferenceRepository> {
  private final Provider<InferenceRepositoryImpl> implProvider;

  public AppModule_ProvideInferenceRepositoryFactory(
      Provider<InferenceRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public InferenceRepository get() {
    return provideInferenceRepository(implProvider.get());
  }

  public static AppModule_ProvideInferenceRepositoryFactory create(
      Provider<InferenceRepositoryImpl> implProvider) {
    return new AppModule_ProvideInferenceRepositoryFactory(implProvider);
  }

  public static InferenceRepository provideInferenceRepository(InferenceRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideInferenceRepository(impl));
  }
}
