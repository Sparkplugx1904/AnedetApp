package com.example.anemiadetector.di;

import android.content.Context;
import com.example.anemiadetector.ml.segmentation.ConjunctivaSegmentor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideConjunctivaSegmentorFactory implements Factory<ConjunctivaSegmentor> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideConjunctivaSegmentorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ConjunctivaSegmentor get() {
    return provideConjunctivaSegmentor(contextProvider.get());
  }

  public static AppModule_ProvideConjunctivaSegmentorFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideConjunctivaSegmentorFactory(contextProvider);
  }

  public static ConjunctivaSegmentor provideConjunctivaSegmentor(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideConjunctivaSegmentor(context));
  }
}
