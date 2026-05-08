package com.example.anemiadetector.di;

import com.example.anemiadetector.data.local.AppDatabase;
import com.example.anemiadetector.data.local.dao.ExaminationDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideExaminationDaoFactory implements Factory<ExaminationDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideExaminationDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ExaminationDao get() {
    return provideExaminationDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideExaminationDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideExaminationDaoFactory(databaseProvider);
  }

  public static ExaminationDao provideExaminationDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideExaminationDao(database));
  }
}
