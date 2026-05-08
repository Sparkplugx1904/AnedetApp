package com.example.anemiadetector.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "examinations")
data class ExaminationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val labelAnemia: Float,
    val labelNonAnemia: Float,
    val predictedLabel: String,
    val confidence: Float,
    val imagePath: String,
    val mode: String
)
