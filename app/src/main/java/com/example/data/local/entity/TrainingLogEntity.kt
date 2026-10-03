package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_logs")
data class TrainingLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val epochsCompleted: Int,
    val initialLoss: Float,
    val finalLoss: Float,
    val samplesTrained: Int,
    val updatedDeltasCount: Int,
    val modelVersion: String,
    val notes: String = ""
)
