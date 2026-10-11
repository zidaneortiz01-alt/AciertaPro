package com.example.domain.rules

import com.example.domain.model.QuestionProgress
import com.example.domain.model.Recommendation
import com.example.domain.model.TopicMastery
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Reglas de Dominio Puras (Sección 23 del documento).
 * Funciones sin efectos secundarios ni dependencias de plataforma.
 */
object MasteryCalculator {
    private const val BASE_ALPHA = 0.35

    /**
     * Calcula la maestría de un tema utilizando Media Móvil Exponencial (EMA)
     * ponderada por la dificultad de la pregunta (1: 0.85, 2: 1.0, 3: 1.25 [valor inicial a validar]).
     * Aplica alfa efectivo ponderado tanto a aciertos como a errores:
     * alfa_ef = min(1.0, 0.35 * peso_dificultad)
     * meta = 100 en acierto, 0 en error.
     * @param history Lista de pares (acierto, dificultad) en orden cronológico
     * @return Porcentaje de maestría estimado entre 0 y 100
     */
    fun calculateTopicMastery(history: List<Pair<Boolean, Int>>): Int {
        if (history.isEmpty()) return 0

        var currentEma = 50.0 // Punto neutral de partida

        for ((isCorrect, difficulty) in history) {
            // Ponderación por dificultad:
            // Dificultad 1: 0.85, Dificultad 2: 1.0, Dificultad 3: 1.25 [valor inicial a validar]
            val difficultyWeight = when (difficulty) {
                1 -> 0.85
                3 -> 1.25
                else -> 1.0
            }

            val alphaEf = min(1.0, BASE_ALPHA * difficultyWeight)
            val targetScore = if (isCorrect) 100.0 else 0.0

            currentEma = (alphaEf * targetScore) + ((1.0 - alphaEf) * currentEma)
        }

        return currentEma.roundToInt().coerceIn(0, 100)
    }

    /**
     * Retorna el nivel cualitativo de dominio según el porcentaje interno de la app.
     * Siempre rotulado como métrica interna de aprendizaje.
     */
    fun getMasteryLevel(percentage: Int): String {
        return when {
            percentage >= 75 -> "Nivel Consolidado [métrica interna]"
            percentage >= 50 -> "Nivel En Desarrollo [métrica interna]"
            percentage >= 25 -> "Nivel Básico [métrica interna]"
            else -> "Por Iniciar [métrica interna]"
        }
    }
}

object LeitnerBoxManager {
    // Intervalos en milisegundos para cada caja (1, 2, 4 y 7 días [valor inicial a validar])
    val BOX_INTERVALS_MS = mapOf(
        1 to 1L * 24 * 60 * 60 * 1000, // 1 día [valor inicial a validar]
        2 to 2L * 24 * 60 * 60 * 1000, // 2 días [valor inicial a validar]
        3 to 4L * 24 * 60 * 60 * 1000, // 4 días [valor inicial a validar]
        4 to 7L * 24 * 60 * 60 * 1000  // 7 días [valor inicial a validar]
    )

    /**
     * Determina el siguiente estado de progreso de una PREGUNTA individual (Ítem 1).
     * Acierto: avanza a la siguiente caja (máx 4).
     * Error: vuelve inmediatamente a la Caja 1 para refuerzo intensivo.
     * dueAt = answeredAt + intervalo_de_la_caja.
     */
    fun calculateNextProgress(
        questionId: String,
        currentBox: Int,
        isCorrect: Boolean,
        answeredAt: Long
    ): QuestionProgress {
        val nextBox = if (isCorrect) {
            min(currentBox + 1, 4)
        } else {
            1
        }
        val interval = BOX_INTERVALS_MS[nextBox] ?: (1L * 24 * 60 * 60 * 1000)
        val dueAt = answeredAt + interval

        return QuestionProgress(
            questionId = questionId,
            box = nextBox,
            lastAnsweredAt = answeredAt,
            dueAt = dueAt
        )
    }

    /**
     * Evalúa si una pregunta está vencida para repaso espaciado.
     */
    fun isOverdue(dueAt: Long, currentTime: Long): Boolean {
        if (dueAt <= 0L) return true
        return currentTime >= dueAt
    }
}

object SessionRecommender {
    /**
     * Genera sugerencias de estudio explicables (Sección 23 y RF-07).
     * Cada recomendación incluye una razón transparente en texto.
     * El reloj se inyecta para facilitar pruebas deterministas (Ítem 1).
     */
    fun generateRecommendations(
        topicsMastery: List<TopicMastery>,
        enabledModuleIds: List<String>,
        clock: () -> Long = { System.currentTimeMillis() }
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        val filteredMastery = topicsMastery.filter { it.moduleId in enabledModuleIds }

        // Regla 1: Repasos espaciados con preguntas vencidas por tema (método Leitner por pregunta, Ítem 1)
        val topicsWithOverdue = filteredMastery.filter { it.overdueCount > 0 }
            .sortedByDescending { it.overdueCount }

        for (item in topicsWithOverdue.take(2)) {
            recommendations.add(
                Recommendation(
                    moduleId = item.moduleId,
                    topicId = item.topicId,
                    title = "Repaso Leitner: ${formatTopicName(item.topicId)}",
                    reason = "Repaso espaciado Leitner: tienes ${item.overdueCount} pregunta(s) vencida(s) para revisión periódica en este tema [valor inicial a validar].",
                    priorityScore = 95.0 + item.overdueCount,
                    estimatedMinutes = 10
                )
            )
        }

        // Regla 2: Temas con baja maestría (< 50%) o errores acumulados
        val weakTopics = filteredMastery.filter { it.totalAttempts > 0 && it.masteryPercentage < 50 }
            .filter { rec -> recommendations.none { it.topicId == rec.topicId } }
            .sortedBy { it.masteryPercentage }

        for (item in weakTopics.take(2)) {
            val failedCount = item.totalAttempts - item.correctCount
            recommendations.add(
                Recommendation(
                    moduleId = item.moduleId,
                    topicId = item.topicId,
                    title = "Reforzar: ${formatTopicName(item.topicId)}",
                    reason = "Recomendado porque tu precisión interna es del ${item.masteryPercentage}% (${failedCount} desaciertos registrados en prácticas).",
                    priorityScore = 80.0 - item.masteryPercentage,
                    estimatedMinutes = 15
                )
            )
        }

        // Regla 3: Temas sin explorar
        val unattemptedTopics = filteredMastery.filter { it.totalAttempts == 0 }
            .filter { rec -> recommendations.none { it.topicId == rec.topicId } }

        for (item in unattemptedTopics.take(1)) {
            recommendations.add(
                Recommendation(
                    moduleId = item.moduleId,
                    topicId = item.topicId,
                    title = "Diagnóstico: ${formatTopicName(item.topicId)}",
                    reason = "Aún no has practicado este tema; completa una sesión breve para calibrar tu ruta de aprendizaje.",
                    priorityScore = 60.0,
                    estimatedMinutes = 12
                )
            )
        }

        return recommendations.sortedByDescending { it.priorityScore }
    }

    private fun formatTopicName(topicId: String): String {
        return topicId.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}

/**
 * Lógica pura del temporizador del simulacro resistente a manipulaciones y reinicios (Ítem 7).
 */
interface SimulationClock {
    fun elapsedRealtimeMs(): Long
    fun wallClockMillis(): Long
    fun getBootCount(): Int
}

data class SavedSimulationState(
    val attemptId: String,
    val moduleId: String,
    val remainingSecondsAtSave: Long,
    val savedElapsedRealtimeMs: Long,
    val savedWallTimeMs: Long,
    val savedBootCount: Int,
    val currentIndex: Int,
    val questionIds: List<String>,
    val answers: Map<String, String>,
    val markedForReview: Set<String>,
    val sessionSeed: Long = 0L
)

data class ResumedSimulationResult(
    val remainingSeconds: Long,
    val wasRebootDetected: Boolean,
    val isExpired: Boolean,
    val noticeMessage: String? = null
)

object SimulationTimerLogic {
    /**
     * Calcula el tiempo restante real al reanudar una sesión persistida.
     * - Si el bootCount coincide y elapsedRealtime es coherente, usa elapsedRealtime (inmune a cambios de reloj del sistema).
     * - Si el bootCount cambió o hubo reinicio, descuenta mediante la diferencia del reloj de pared acotada a [0, remainingSeconds].
     */
    fun calculateResumedTime(
        saved: SavedSimulationState,
        clock: SimulationClock
    ): ResumedSimulationResult {
        val currentBoot = clock.getBootCount()
        val currentElapsed = clock.elapsedRealtimeMs()
        val currentWall = clock.wallClockMillis()

        // Si falla la lectura o no hay BOOT_COUNT (alguno de los dos valores es -1),
        // se usa SIEMPRE la rama de reloj de pared acotado.
        val canUseElapsed = currentBoot != -1 &&
                saved.savedBootCount != -1 &&
                currentBoot == saved.savedBootCount &&
                currentElapsed >= saved.savedElapsedRealtimeMs

        return if (canUseElapsed) {
            val elapsedDiffSeconds = (currentElapsed - saved.savedElapsedRealtimeMs) / 1000L
            val remaining = (saved.remainingSecondsAtSave - elapsedDiffSeconds).coerceAtLeast(0L)
            ResumedSimulationResult(
                remainingSeconds = remaining,
                wasRebootDetected = false,
                isExpired = remaining <= 0L,
                noticeMessage = null
            )
        } else {
            // Se detectó reinicio del dispositivo o alguno de los boot counts es -1
            val wallDiffSeconds = ((currentWall - saved.savedWallTimeMs) / 1000L).coerceAtLeast(0L)
            val remaining = (saved.remainingSecondsAtSave - wallDiffSeconds).coerceIn(0L, saved.remainingSecondsAtSave)
            ResumedSimulationResult(
                remainingSeconds = remaining,
                wasRebootDetected = true,
                isExpired = remaining <= 0L,
                noticeMessage = "Tiempo restante estimado tras reinicio del dispositivo"
            )
        }
    }

    /**
     * Durante la ejecución activa, calcula el tiempo restante respecto al instante fin (targetElapsed).
     */
    fun calculateActiveRemaining(
        targetElapsedRealtimeMs: Long,
        currentElapsedRealtimeMs: Long
    ): Long {
        return ((targetElapsedRealtimeMs - currentElapsedRealtimeMs) / 1000L).coerceAtLeast(0L)
    }
}
