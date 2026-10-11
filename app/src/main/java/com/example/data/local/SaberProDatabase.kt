package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AttemptAnswerDao
import com.example.data.local.dao.AttemptDao
import com.example.data.local.dao.LocalAccountDao
import com.example.data.local.dao.QuestionProgressDao
import com.example.data.local.dao.QuestionReportDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WrittenEssayDao
import com.example.data.local.entity.AttemptAnswerEntity
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.LocalAccountEntity
import com.example.data.local.entity.QuestionProgressEntity
import com.example.data.local.entity.QuestionReportEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WrittenEssayEntity

@Database(
    entities = [
        LocalAccountEntity::class,
        UserProfileEntity::class,
        AttemptEntity::class,
        AttemptAnswerEntity::class,
        QuestionProgressEntity::class,
        WrittenEssayEntity::class,
        QuestionReportEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SaberProDatabase : RoomDatabase() {
    abstract fun localAccountDao(): LocalAccountDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun attemptDao(): AttemptDao
    abstract fun attemptAnswerDao(): AttemptAnswerDao
    abstract fun questionProgressDao(): QuestionProgressDao
    abstract fun writtenEssayDao(): WrittenEssayDao
    abstract fun questionReportDao(): QuestionReportDao

    companion object {
        @Volatile
        private var INSTANCE: SaberProDatabase? = null

        fun getInstance(context: Context): SaberProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SaberProDatabase::class.java,
                    "saber_pro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
