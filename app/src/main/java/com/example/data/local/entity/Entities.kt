package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "local_account")
data class LocalAccountEntity(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val saltHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val academicProgram: String,
    val campus: String,
    val enabledModulesJson: String,
    val targetDate: String,
    val studyDaysPerWeek: Int,
    val sessionMinutes: Int,
    val hasConsented: Boolean,
    val consentDate: String,
    val consentVersion: String,
    val isDarkMode: Boolean
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String, // UUID generado en cliente para idempotencia
    val userId: String,
    val kind: String,
    val moduleId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val durationSeconds: Long,
    val syncStatus: String
)

@Entity(
    tableName = "attempt_answers",
    primaryKeys = ["attemptId", "questionId"],
    indices = [Index(value = ["attemptId"]), Index(value = ["questionId"])]
)
data class AttemptAnswerEntity(
    val attemptId: String,
    val questionId: String,
    val selectedOption: String,
    val selectedOriginalKey: String = selectedOption,
    val selectedOptionText: String = "",
    val isCorrect: Boolean,
    val timeSpentSeconds: Long,
    val markedForReview: Boolean,
    val answeredAt: Long // Marca de tiempo real de cada respuesta (Ítem 1)
)

@Entity(tableName = "question_progress")
data class QuestionProgressEntity(
    @PrimaryKey val questionId: String,
    val box: Int, // 1..4
    val lastAnsweredAt: Long,
    val dueAt: Long // Momento exacto en que vence el repaso espaciado
)

@Entity(tableName = "written_essays")
data class WrittenEssayEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val lastSavedAt: Long,
    val wordCount: Int,
    val selfScoreThesis: Int,
    val selfScoreArguments: Int,
    val selfScoreStructure: Int,
    val selfScoreLanguage: Int
)

@Entity(tableName = "question_reports")
data class QuestionReportEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val module: String,
    val reason: String,
    val details: String,
    val timestamp: Long
)
