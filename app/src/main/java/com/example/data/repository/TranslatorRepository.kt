package com.example.data.repository

import com.example.data.local.dao.TranslatorDao
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.ModelWeightDeltaEntity
import com.example.data.local.entity.TrainingLogEntity
import com.example.data.local.entity.TranslationHistoryEntity
import com.example.engine.OfflineNeuralEngine
import com.example.engine.TrainingProgress
import com.example.engine.TrainingResult
import com.example.engine.TranslationResult
import com.example.model.SupportedLanguage
import kotlinx.coroutines.flow.Flow

class TranslatorRepository(
    private val translatorDao: TranslatorDao,
    private val neuralEngine: OfflineNeuralEngine
) {

    // --- Translation Operations ---
    suspend fun translateOffline(
        inputText: String,
        sourceLang: SupportedLanguage,
        targetLang: SupportedLanguage
    ): TranslationResult {
        val result = neuralEngine.translate(inputText, sourceLang, targetLang)
        if (inputText.isNotBlank() && result.translatedText.isNotBlank()) {
            translatorDao.insertHistory(
                TranslationHistoryEntity(
                    sourceLang = sourceLang.code,
                    targetLang = targetLang.code,
                    sourceText = inputText,
                    translatedText = result.translatedText,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return result
    }

    // --- On-Device Neural Training ---
    suspend fun trainOfflineNeuralModel(
        epochs: Int,
        learningRate: Float,
        onProgress: (TrainingProgress) -> Unit
    ): TrainingResult {
        return neuralEngine.trainLocalModel(epochs, learningRate, onProgress)
    }

    suspend fun factoryReset(): Boolean {
        return neuralEngine.factoryReset()
    }

    // --- User Custom Dictionary ---
    fun getAllDictionaryEntries(): Flow<List<DictionaryEntryEntity>> =
        translatorDao.getAllDictionaryEntries()

    suspend fun insertDictionaryEntry(entry: DictionaryEntryEntity): Long =
        translatorDao.insertDictionaryEntry(entry)

    suspend fun deleteDictionaryEntry(entry: DictionaryEntryEntity) =
        translatorDao.deleteDictionaryEntry(entry)

    suspend fun deleteDictionaryEntryById(id: Long) =
        translatorDao.deleteDictionaryEntryById(id)

    // --- Translation History ---
    fun getTranslationHistory(): Flow<List<TranslationHistoryEntity>> =
        translatorDao.getAllHistory()

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        translatorDao.updateFavorite(id, isFavorite)

    suspend fun deleteHistoryById(id: Long) =
        translatorDao.deleteHistoryById(id)

    suspend fun clearHistorySince(sinceTimestamp: Long) =
        translatorDao.clearHistorySince(sinceTimestamp)

    suspend fun clearHistoryOlderThan(beforeTimestamp: Long) =
        translatorDao.clearHistoryOlderThan(beforeTimestamp)

    suspend fun insertHistoryItem(item: TranslationHistoryEntity) =
        translatorDao.insertHistory(item)

    suspend fun clearHistory() =
        translatorDao.clearHistory()

    // --- Weight Deltas & Training Logs ---
    fun getWeightDeltasCountFlow(): Flow<Int> =
        translatorDao.getWeightDeltasCountFlow()

    fun getAllWeightDeltas(): Flow<List<ModelWeightDeltaEntity>> =
        translatorDao.getAllWeightDeltasFlow()

    fun getTrainingLogs(): Flow<List<TrainingLogEntity>> =
        translatorDao.getAllTrainingLogs()

    suspend fun getInitialDeltasCount(): Int =
        translatorDao.getWeightDeltasCount()
}
