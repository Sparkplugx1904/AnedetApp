package com.example.anemiadetector.domain.usecase

import com.example.anemiadetector.data.local.entity.ExaminationEntity
import com.example.anemiadetector.data.repository.ExaminationRepository
import javax.inject.Inject

class SaveExaminationUseCase @Inject constructor(
    private val examinationRepository: ExaminationRepository
) {
    suspend operator fun invoke(examination: ExaminationEntity): Long {
        return examinationRepository.insert(examination)
    }
}
