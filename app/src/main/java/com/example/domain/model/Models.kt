package com.example.domain.model

import java.util.UUID

enum class AttemptKind(val label: String) {
    DIAGNOSTIC("Diagnóstico"),
    PRACTICE("Práctica guiada"),
    SIMULATION("Simulacro cronometrado"),
    FREE_PRACTICE("Práctica libre")
}

enum class SyncStatus(val label: String) {
    SOLO_LOCAL("Solo local"),
    PENDIENTE("Pendiente de sincronizar (demo)"),
    ERROR_REINTENTO("Error en sincronización (demo)")
}

data class FuenteVersion(
    val origen: String = "original",
    val guia: String = "2026-2 (por confirmar)",
    val fecha: String = "2026-10"
)

data class Question(
    val id_pregunta: String,
    val modulo: String,
    val competencia_tema: String,
    val enunciado: String,
    val tipo_pregunta: String = "seleccion_multiple_unica",
    val opciones: Map<String, String>,
    val respuesta_correcta: String,
    val explicacion: String,
    val dificultad: Int = 2,
    val fuente_version: FuenteVersion = FuenteVersion(),
    val estado_revision: String = "borrador",
    val clave_original_por_mostrada: Map<String, String> = emptyMap(),
    val mediaUrlsJson: String = "[]",
    val mediaType: String = "image",
    val altTextAccessibility: String? = null
)

data class Attempt(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "local",
    val kind: AttemptKind,
    val moduleId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val durationSeconds: Long = 0,
    val syncStatus: SyncStatus = SyncStatus.SOLO_LOCAL
)

data class AttemptAnswer(
    val attemptId: String,
    val questionId: String,
    val selectedOption: String, // Letra mostrada
    val selectedOriginalKey: String = selectedOption, // Clave original elegida
    val selectedOptionText: String = "", // Texto de la opción original elegida
    val isCorrect: Boolean,
    val timeSpentSeconds: Long = 0,
    val markedForReview: Boolean = false,
    val answeredAt: Long = System.currentTimeMillis() // Marca de tiempo real por respuesta (Ítem 1)
)

data class QuestionProgress(
    val questionId: String,
    val box: Int = 1, // 1..4
    val lastAnsweredAt: Long = 0L,
    val dueAt: Long = 0L // Momento exacto de vencimiento Leitner
)

enum class UserRole(val label: String) {
    STUDENT("Estudiante"),
    TEACHER("Docente"),
    SUPERUSER("Superusuario")
}

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val isGoogleAccount: Boolean = false,
    val photoUrl: String? = null,
    val academicProgram: String = "Ingeniería de Sistemas",
    val campus: String = "Sede Maicao",
    val enabledModules: List<String> = listOf(
        "lectura_critica",
        "razonamiento_cuantitativo",
        "competencias_ciudadanas",
        "ingles",
        "comunicacion_escrita",
        "ingenieria_sistemas"
    ),
    val targetDate: String = "Noviembre 2026",
    val studyDaysPerWeek: Int = 4,
    val sessionMinutes: Int = 30,
    val hasConsented: Boolean = false,
    val consentDate: String = "",
    val consentVersion: String = "Ley 1581 v1.0",
    val isDarkMode: Boolean = false,
    val isAdjustedBooklet: Boolean = false,
    val includesEnglishTest: Boolean = true,
    val disabilityType: String? = null
)

data class GuideModuleConfig(
    val id: String,
    val nombre: String,
    val tipo: String, // "oficial_generico" | "refuerzo_carrera"
    val preguntas_simulacro: Int,
    val tiempo_minutos: Int,
    val descripcion: String
)

data class QuestionReport(
    val id: String = UUID.randomUUID().toString(),
    val questionId: String,
    val module: String,
    val reason: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Recommendation(
    val moduleId: String,
    val topicId: String,
    val title: String,
    val reason: String,
    val priorityScore: Double,
    val estimatedMinutes: Int = 15
)

data class TopicMastery(
    val topicId: String,
    val moduleId: String,
    val totalAttempts: Int,
    val correctCount: Int,
    val masteryPercentage: Int, // 0..100
    val averageLeitnerBox: Double = 1.0,
    val overdueCount: Int = 0
)

data class WrittenEssay(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Ensayo Argumentativo: Impacto de la Inteligencia Artificial [tema de práctica de ejemplo]",
    val content: String = "",
    val lastSavedAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val selfScoreThesis: Int = 0,      // 0..4
    val selfScoreArguments: Int = 0,   // 0..4
    val selfScoreStructure: Int = 0,   // 0..4
    val selfScoreLanguage: Int = 0     // 0..4
)
