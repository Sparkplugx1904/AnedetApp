package com.example.anemiadetector.domain.usecase

import com.example.anemiadetector.data.local.entity.ExaminationEntity
import com.example.anemiadetector.data.repository.ExaminationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val examinationRepository: ExaminationRepository
) {
    operator fun invoke(): Flow<List<ExaminationEntity>> = examinationRepository.getAll()
}
