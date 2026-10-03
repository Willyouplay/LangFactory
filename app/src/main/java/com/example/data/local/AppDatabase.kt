package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.TranslatorDao
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.ModelWeightDeltaEntity
import com.example.data.local.entity.TrainingLogEntity
import com.example.data.local.entity.TranslationHistoryEntity

@Database(
    entities = [
        DictionaryEntryEntity::class,
        TranslationHistoryEntity::class,
        ModelWeightDeltaEntity::class,
        TrainingLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun translatorDao(): TranslatorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speaksy_offline_translator.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
