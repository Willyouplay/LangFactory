package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.ModelWeightDeltaEntity
import com.example.data.local.entity.TrainingLogEntity
import com.example.data.local.entity.TranslationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslatorDao {

    // --- User Custom Dictionary ---
    @Query("SELECT * FROM user_dictionary ORDER BY priorityWeight DESC, createdAt DESC")
    fun getAllDictionaryEntries(): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT * FROM user_dictionary WHERE sourceLang = :sourceLang AND targetLang = :targetLang ORDER BY priorityWeight DESC")
    suspend fun getDictionaryForPair(sourceLang: String, targetLang: String): List<DictionaryEntryEntity>

    @Query("SELECT * FROM user_dictionary ORDER BY priorityWeight DESC")
    suspend fun getAllDictionaryEntriesList(): List<DictionaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryEntry(entry: DictionaryEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryEntries(entries: List<DictionaryEntryEntity>): List<Long>

    @Update
    suspend fun updateDictionaryEntry(entry: DictionaryEntryEntity)

    @Delete
    suspend fun deleteDictionaryEntry(entry: DictionaryEntryEntity)

    @Query("DELETE FROM user_dictionary WHERE id = :id")
    suspend fun deleteDictionaryEntryById(id: Long)

    @Query("DELETE FROM user_dictionary")
    suspend fun clearAllDictionary()

    // --- Translation History ---
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TranslationHistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistory(): Flow<List<TranslationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: TranslationHistoryEntity): Long

    @Query("UPDATE translation_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM translation_history WHERE timestamp >= :sinceTimestamp")
    suspend fun clearHistorySince(sinceTimestamp: Long)

    @Query("DELETE FROM translation_history WHERE timestamp < :beforeTimestamp")
    suspend fun clearHistoryOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearHistory()

    // --- Neural Weight Deltas (Locally Learned Weights) ---
    @Query("SELECT * FROM neural_weight_deltas WHERE sourceLang = :sourceLang AND targetLang = :targetLang")
    suspend fun getWeightDeltasForPair(sourceLang: String, targetLang: String): List<ModelWeightDeltaEntity>

    @Query("SELECT * FROM neural_weight_deltas")
    fun getAllWeightDeltasFlow(): Flow<List<ModelWeightDeltaEntity>>

    @Query("SELECT * FROM neural_weight_deltas")
    suspend fun getAllWeightDeltasList(): List<ModelWeightDeltaEntity>

    @Query("SELECT COUNT(*) FROM neural_weight_deltas")
    fun getWeightDeltasCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM neural_weight_deltas")
    suspend fun getWeightDeltasCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightDelta(delta: ModelWeightDeltaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightDeltas(deltas: List<ModelWeightDeltaEntity>)

    @Query("DELETE FROM neural_weight_deltas")
    suspend fun clearAllWeightDeltas()

    // --- Training Logs ---
    @Query("SELECT * FROM training_logs ORDER BY timestamp DESC")
    fun getAllTrainingLogs(): Flow<List<TrainingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingLog(log: TrainingLogEntity): Long

    @Query("DELETE FROM training_logs")
    suspend fun clearTrainingLogs()

    // --- Complete Factory Reset Transaction ---
    @Transaction
    suspend fun executeCompleteFactoryReset() {
        clearAllDictionary()
        clearAllWeightDeltas()
        clearTrainingLogs()
        clearHistory()
    }
}
