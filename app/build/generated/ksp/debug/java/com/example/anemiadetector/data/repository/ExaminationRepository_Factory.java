package com.example.anemiadetector.data.repository;

import com.example.anemiadetector.data.local.dao.ExaminationDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ExaminationRepository_Factory implements Factory<ExaminationRepository> {
  private final Provider<ExaminationDao> examinationDaoProvider;

  public ExaminationRepository_Factory(Provider<ExaminationDao> examinationDaoProvider) {
    this.examinationDaoProvider = examinationDaoProvider;
  }

  @Override
  public ExaminationRepository get() {
    return newInstance(examinationDaoProvider.get());
  }

  public static ExaminationRepository_Factory create(
      Provider<ExaminationDao> examinationDaoProvider) {
    return new ExaminationRepository_Factory(examinationDaoProvider);
  }

  public static ExaminationRepository newInstance(ExaminationDao examinationDao) {
    return new ExaminationRepository(examinationDao);
  }
}
