package com.example.anemiadetector.ml.segmentation;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ConjunctivaSegmentor_Factory implements Factory<ConjunctivaSegmentor> {
  private final Provider<Context> contextProvider;

  public ConjunctivaSegmentor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ConjunctivaSegmentor get() {
    return newInstance(contextProvider.get());
  }

  public static ConjunctivaSegmentor_Factory create(Provider<Context> contextProvider) {
    return new ConjunctivaSegmentor_Factory(contextProvider);
  }

  public static ConjunctivaSegmentor newInstance(Context context) {
    return new ConjunctivaSegmentor(context);
  }
}
