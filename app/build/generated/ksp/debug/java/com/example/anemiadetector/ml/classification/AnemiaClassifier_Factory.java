package com.example.anemiadetector.ml.classification;

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
public final class AnemiaClassifier_Factory implements Factory<AnemiaClassifier> {
  private final Provider<Context> contextProvider;

  public AnemiaClassifier_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AnemiaClassifier get() {
    return newInstance(contextProvider.get());
  }

  public static AnemiaClassifier_Factory create(Provider<Context> contextProvider) {
    return new AnemiaClassifier_Factory(contextProvider);
  }

  public static AnemiaClassifier newInstance(Context context) {
    return new AnemiaClassifier(context);
  }
}
