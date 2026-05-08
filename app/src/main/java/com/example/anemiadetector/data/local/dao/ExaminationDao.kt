package com.example.anemiadetector.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.anemiadetector.data.local.entity.ExaminationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExaminationDao {
    @Query("SELECT * FROM examinations ORDER BY timestamp DESC")
    fun getAllExaminations(): Flow<List<ExaminationEntity>>

    @Query("SELECT * FROM examinations WHERE predictedLabel = :label ORDER BY timestamp DESC")
    fun getByLabel(label: String): Flow<List<ExaminationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(examination: ExaminationEntity): Long

    @Delete
    suspend fun delete(examination: ExaminationEntity)

    @Query("DELETE FROM examinations")
    suspend fun deleteAll()
}
