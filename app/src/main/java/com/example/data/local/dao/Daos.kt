package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AttemptAnswerEntity
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.LocalAccountEntity
import com.example.data.local.entity.QuestionProgressEntity
import com.example.data.local.entity.QuestionReportEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WrittenEssayEntity

@Dao
interface LocalAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: LocalAccountEntity)

    @Query("SELECT * FROM local_account WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): LocalAccountEntity?

    @Query("SELECT * FROM local_account LIMIT 1")
    suspend fun getFirstAccount(): LocalAccountEntity?

    @Query("DELETE FROM local_account")
    suspend fun deleteAll()
}

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}

@Dao
interface AttemptDao {
    // Inserción idempotente: conflicto ignorado si el UUID del cliente ya existe
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts ORDER BY startedAt DESC")
    suspend fun getAllAttempts(): List<AttemptEntity>

    @Query("DELETE FROM attempts")
    suspend fun deleteAll()
}

@Dao
interface AttemptAnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<AttemptAnswerEntity>)

    @Query("SELECT * FROM attempt_answers ORDER BY answeredAt ASC")
    suspend fun getAllAnswers(): List<AttemptAnswerEntity>

    @Query("SELECT * FROM attempt_answers WHERE attemptId = :attemptId ORDER BY answeredAt ASC")
    suspend fun getAnswersForAttempt(attemptId: String): List<AttemptAnswerEntity>

    @Query("DELETE FROM attempt_answers")
    suspend fun deleteAll()
}

@Dao
interface QuestionProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: QuestionProgressEntity)

    @Query("SELECT * FROM question_progress WHERE questionId = :questionId LIMIT 1")
    suspend fun getProgress(questionId: String): QuestionProgressEntity?

    @Query("SELECT * FROM question_progress")
    suspend fun getAllProgress(): List<QuestionProgressEntity>

    @Query("DELETE FROM question_progress")
    suspend fun deleteAll()
}

@Dao
interface WrittenEssayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(essay: WrittenEssayEntity)

    @Query("SELECT * FROM written_essays ORDER BY lastSavedAt DESC LIMIT 1")
    suspend fun getEssay(): WrittenEssayEntity?

    @Query("DELETE FROM written_essays")
    suspend fun deleteAll()
}

@Dao
interface QuestionReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: QuestionReportEntity)

    @Query("SELECT * FROM question_reports ORDER BY timestamp DESC")
    suspend fun getAllReports(): List<QuestionReportEntity>

    @Query("DELETE FROM question_reports")
    suspend fun deleteAll()
}
