package com.example.anemiadetector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.anemiadetector.data.local.dao.ExaminationDao
import com.example.anemiadetector.data.local.entity.ExaminationEntity

@Database(entities = [ExaminationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examinationDao(): ExaminationDao
}
