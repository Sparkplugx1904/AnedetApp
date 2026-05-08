package com.example.anemiadetector.di

import android.content.Context
import androidx.room.Room
import com.example.anemiadetector.data.local.AppDatabase
import com.example.anemiadetector.data.local.dao.ExaminationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "anemia_detector.db").build()
    }

    @Provides
    fun provideExaminationDao(database: AppDatabase): ExaminationDao = database.examinationDao()
}
