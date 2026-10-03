package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a fine-tuned weight delta for the on-device offline translation neural network.
 * When training is triggered locally, gradient updates adjust the alignment matrix and attention weights
 * between tokens and phrases, allowing the offline model to adapt specifically to the user's domain.
 */
@Entity(
    tableName = "neural_weight_deltas",
    indices = [Index(value = ["sourceLang", "targetLang", "tokenPattern"], unique = true)]
)
data class ModelWeightDeltaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceLang: String,
    val targetLang: String,
    val tokenPattern: String,
    val targetPattern: String,
    val weightDelta: Float,
    val activationCount: Int = 1,
    val confidenceScore: Float = 0.95f,
    val lastUpdated: Long = System.currentTimeMillis()
)
