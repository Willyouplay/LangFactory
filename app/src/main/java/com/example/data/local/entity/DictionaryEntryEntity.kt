package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_dictionary",
    indices = [Index(value = ["sourceLang", "targetLang", "sourceWord"])]
)
data class DictionaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceLang: String,
    val targetLang: String,
    val sourceWord: String,
    val targetWord: String,
    val category: String = "Общий",
    val priorityWeight: Float = 2.0f,
    val createdAt: Long = System.currentTimeMillis()
)
