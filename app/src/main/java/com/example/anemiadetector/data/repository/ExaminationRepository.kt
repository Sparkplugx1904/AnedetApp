package com.example.anemiadetector.data.repository

import com.example.anemiadetector.data.local.dao.ExaminationDao
import com.example.anemiadetector.data.local.entity.ExaminationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExaminationRepository @Inject constructor(
    private val examinationDao: ExaminationDao
) {
    fun getAll(): Flow<List<ExaminationEntity>> = examinationDao.getAllExaminations()
    fun getByLabel(label: String): Flow<List<ExaminationEntity>> = examinationDao.getByLabel(label)
    suspend fun insert(exam: ExaminationEntity): Long = examinationDao.insert(exam)
    suspend fun delete(exam: ExaminationEntity) = examinationDao.delete(exam)
}
