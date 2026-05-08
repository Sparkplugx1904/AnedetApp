package com.example.anemiadetector.di;

import android.content.Context;
import com.example.anemiadetector.ml.classification.AnemiaClassifier;
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
public final class AppModule_ProvideAnemiaClassifierFactory implements Factory<AnemiaClassifier> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideAnemiaClassifierFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AnemiaClassifier get() {
    return provideAnemiaClassifier(contextProvider.get());
  }

  public static AppModule_ProvideAnemiaClassifierFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideAnemiaClassifierFactory(contextProvider);
  }

  public static AnemiaClassifier provideAnemiaClassifier(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAnemiaClassifier(context));
  }
}
