package com.example.domain

import com.example.core.crypto.PasswordHasher
import com.example.domain.model.Question
import com.example.domain.model.TopicMastery
import com.example.domain.rules.LeitnerBoxManager
import com.example.domain.rules.MasteryCalculator
import com.example.domain.rules.QuestionShuffler
import com.example.domain.rules.SavedSimulationState
import com.example.domain.rules.SessionRecommender
import com.example.domain.rules.SimulationClock
import com.example.domain.rules.SimulationTimerLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de la lógica pura de dominio (Secciones 23 y requerimientos de corrección).
 */
class DomainLogicTest {

    // =========================================================================
    // ÍTEM 8: MOTOR DE MAESTRÍA (EMA) CON PONDERACIÓN EFECTIVA DE DIFICULTAD
    // =========================================================================

    @Test
    fun testMasteryCalculator_emptyHistoryReturnsZero() {
        val mastery = MasteryCalculator.calculateTopicMastery(emptyList())
        assertEquals(0, mastery)
    }

    @Test
    fun testMasteryCalculator_syntheticAllCorrectIncreasesEma() {
        val syntheticHistory = listOf(
            Pair(true, 2),
            Pair(true, 2),
            Pair(true, 2),
            Pair(true, 2),
            Pair(true, 2)
        )
        val score = MasteryCalculator.calculateTopicMastery(syntheticHistory)
        assertTrue("El puntaje acumulado con aciertos continuos debe ser >= 75%", score >= 75)
    }

    @Test
    fun testMasteryCalculator_correctInDifficulty3RaisesMoreThanInDifficulty2() {
        // Partiendo del punto neutral (50.0), un acierto en dificultad 3 debe subir más que en 2
        val scoreDiff3 = MasteryCalculator.calculateTopicMastery(listOf(Pair(true, 3)))
        val scoreDiff2 = MasteryCalculator.calculateTopicMastery(listOf(Pair(true, 2)))
        assertTrue("Acierto en dificultad 3 ($scoreDiff3) debe subir más que en 2 ($scoreDiff2)", scoreDiff3 > scoreDiff2)
    }

    @Test
    fun testMasteryCalculator_errorInDifficulty3DropsMoreThanInDifficulty1() {
        // Partiendo del punto neutral (50.0), un error en dificultad 3 debe bajar más que en 1 (score menor)
        val scoreErrorDiff3 = MasteryCalculator.calculateTopicMastery(listOf(Pair(false, 3)))
        val scoreErrorDiff1 = MasteryCalculator.calculateTopicMastery(listOf(Pair(false, 1)))
        assertTrue("Error en dificultad 3 ($scoreErrorDiff3) debe bajar más que en 1 ($scoreErrorDiff1)", scoreErrorDiff3 < scoreErrorDiff1)
    }

    // =========================================================================
    // ÍTEM 1: REPASO ESPACIADO LEITNER POR PREGUNTA Y RELOJ INYECTADO
    // =========================================================================

    @Test
    fun testLeitner_questionProgressTransitionsAndResetOnFailure() {
        val t0 = 1000000000L
        val qId = "RC-001"

        // 1. Acierto inicial: Caja 1 -> Caja 2
        val prog1 = LeitnerBoxManager.calculateNextProgress(qId, currentBox = 1, isCorrect = true, answeredAt = t0)
        assertEquals(2, prog1.box)
        val intervalBox2 = LeitnerBoxManager.BOX_INTERVALS_MS[2]!!
        assertEquals(t0 + intervalBox2, prog1.dueAt)

        // 2. Segundo acierto consecutivo: Caja 2 -> Caja 3
        val prog2 = LeitnerBoxManager.calculateNextProgress(qId, currentBox = 2, isCorrect = true, answeredAt = t0 + 100)
        assertEquals(3, prog2.box)

        // 3. Tercer acierto consecutivo: Caja 3 -> Caja 4 (máximo)
        val prog3 = LeitnerBoxManager.calculateNextProgress(qId, currentBox = 3, isCorrect = true, answeredAt = t0 + 200)
        assertEquals(4, prog3.box)

        // 4. Pregunta fallada: VUELVE A CAJA 1 (Ítem 1)
        val progFailed = LeitnerBoxManager.calculateNextProgress(qId, currentBox = 4, isCorrect = false, answeredAt = t0 + 300)
        assertEquals(1, progFailed.box)
        val intervalBox1 = LeitnerBoxManager.BOX_INTERVALS_MS[1]!!
        assertEquals(t0 + 300 + intervalBox1, progFailed.dueAt)
    }

    @Test
    fun testLeitner_overdueCalculationWithInjectedClock() {
        val fakeClockNow = 2000000000000L
        val oneDayMs = 1L * 24 * 60 * 60 * 1000

        // dueAt futuro: no vencida
        val futureDue = fakeClockNow + 3600000L
        assertFalse(LeitnerBoxManager.isOverdue(futureDue, fakeClockNow))

        // dueAt pasado: vencida
        val pastDue = fakeClockNow - 1000L
        assertTrue(LeitnerBoxManager.isOverdue(pastDue, fakeClockNow))
    }

    @Test
    fun testSessionRecommender_topicWithOverdueQuestionsAppearsWithReason() {
        val fakeClock = { 1700000000000L }

        val syntheticMasteries = listOf(
            TopicMastery(
                topicId = "geometria_medidas",
                moduleId = "razonamiento_cuantitativo",
                totalAttempts = 6,
                correctCount = 5,
                masteryPercentage = 80,
                overdueCount = 3 // 3 preguntas vencidas en Leitner
            ),
            TopicMastery(
                topicId = "porcentajes",
                moduleId = "razonamiento_cuantitativo",
                totalAttempts = 5,
                correctCount = 1,
                masteryPercentage = 20,
                overdueCount = 0
            )
        )

        val recommendations = SessionRecommender.generateRecommendations(
            topicsMastery = syntheticMasteries,
            enabledModuleIds = listOf("razonamiento_cuantitativo"),
            clock = fakeClock
        )

        assertTrue(recommendations.isNotEmpty())
        val overdueRec = recommendations.firstOrNull { it.topicId == "geometria_medidas" }
        assertTrue("El tema con preguntas vencidas debe ser recomendado", overdueRec != null)
        assertTrue("La razón debe explicar que tiene preguntas vencidas en Leitner",
            overdueRec!!.reason.contains("tienes 3 pregunta(s) vencida(s) para revisión periódica"))
    }

    // =========================================================================
    // ÍTEM 7: SIMULACRO CON RELOJ ROBUSTO (ELAPSEDREALTIME, REINICIO Y EXPIRACIÓN)
    // =========================================================================

    class FakeSimulationClock(
        var elapsed: Long,
        var wall: Long,
        var boot: Int
    ) : SimulationClock {
        override fun elapsedRealtimeMs(): Long = elapsed
        override fun wallClockMillis(): Long = wall
        override fun getBootCount(): Int = boot
    }

    @Test
    fun testSimulationTimer_deviceWallTimeChangeDoesNotAffectRemainingTime() {
        val fakeClock = FakeSimulationClock(elapsed = 100000L, wall = 1700000000000L, boot = 1)
        val saved = SavedSimulationState(
            attemptId = "sim-1",
            moduleId = "lectura_critica",
            remainingSecondsAtSave = 3600L,
            savedElapsedRealtimeMs = 100000L,
            savedWallTimeMs = 1700000000000L,
            savedBootCount = 1,
            currentIndex = 0,
            questionIds = listOf("LC-001"),
            answers = emptyMap(),
            markedForReview = emptySet()
        )

        // El usuario adelanta el reloj de pared 5 horas, pero elapsedRealtime solo avanzó 60 segundos (60,000 ms)
        fakeClock.wall += 5L * 3600 * 1000 // +5 horas en reloj del sistema
        fakeClock.elapsed += 60000L       // +60 segundos reales

        val result = SimulationTimerLogic.calculateResumedTime(saved, fakeClock)

        assertFalse("No debe detectar reinicio", result.wasRebootDetected)
        assertFalse("No debe expirar", result.isExpired)
        assertEquals("El tiempo restante debe descontar solo los 60s reales medidos por elapsedRealtime",
            3540L, result.remainingSeconds)
    }

    @Test
    fun testSimulationTimer_resumingAfterDeviceRebootUsesWallClockDifference() {
        val fakeClock = FakeSimulationClock(elapsed = 5000L, wall = 1700000000000L + 120000L, boot = 2) // Boot count cambió a 2
        val saved = SavedSimulationState(
            attemptId = "sim-2",
            moduleId = "lectura_critica",
            remainingSecondsAtSave = 600L,
            savedElapsedRealtimeMs = 999999L,
            savedWallTimeMs = 1700000000000L,
            savedBootCount = 1, // Boot count anterior era 1
            currentIndex = 0,
            questionIds = listOf("LC-001"),
            answers = emptyMap(),
            markedForReview = emptySet()
        )

        val result = SimulationTimerLogic.calculateResumedTime(saved, fakeClock)

        assertTrue("Debe detectar reinicio al cambiar el bootCount", result.wasRebootDetected)
        // Pasaron 120 segundos en el reloj de pared (120,000 ms)
        assertEquals(480L, result.remainingSeconds)
        assertTrue(result.noticeMessage!!.contains("tras reinicio"))
    }

    @Test
    fun testSimulationTimer_expirationMarksAttemptAsExpired() {
        val fakeClock = FakeSimulationClock(elapsed = 2000000L, wall = 1700005000000L, boot = 1)
        val saved = SavedSimulationState(
            attemptId = "sim-3",
            moduleId = "lectura_critica",
            remainingSecondsAtSave = 30L,
            savedElapsedRealtimeMs = 100000L,
            savedWallTimeMs = 1700000000000L,
            savedBootCount = 1,
            currentIndex = 0,
            questionIds = listOf("LC-001"),
            answers = mapOf("LC-001" to "A"),
            markedForReview = emptySet()
        )

        val result = SimulationTimerLogic.calculateResumedTime(saved, fakeClock)
        assertTrue("El simulacro debe marcarse como vencido", result.isExpired)
        assertEquals(0L, result.remainingSeconds)
    }

    @Test
    fun testSimulationTimer_withoutBootCount_afterReboot_usesWallClockDifferenceNotElapsed() {
        // Estado guardado antes del reinicio
        val saved = SavedSimulationState(
            attemptId = "sim-no-boot",
            moduleId = "lectura_critica",
            remainingSecondsAtSave = 1000L,
            savedElapsedRealtimeMs = 40_000L,
            savedWallTimeMs = 1_000_000L,
            savedBootCount = -1, // Falla de lectura o sin soporte de BOOT_COUNT
            currentIndex = 0,
            questionIds = listOf("LC-001"),
            answers = emptyMap(),
            markedForReview = emptySet()
        )

        // Reloj simulado tras reinicio del dispositivo:
        // elapsedRealtime es 90_000 ms (mayor que los 40_000 ms guardados, p. ej. el usuario abrió la app 90s tras encender).
        // Si erróneamente se usara elapsedRealtime: (90_000 - 40_000) / 1000 = 50s descontados (950s restantes).
        // Reloj de pared avanzó 100 segundos (1_100_000 ms).
        // Al tener bootCount = -1, calculateResumedTime debe usar SIEMPRE la rama de reloj de pared acotado:
        // (1_100_000 - 1_000_000) / 1000 = 100s descontados (900s restantes).
        val fakeClock = object : SimulationClock {
            override fun elapsedRealtimeMs(): Long = 90_000L
            override fun wallClockMillis(): Long = 1_100_000L
            override fun getBootCount(): Int = -1 // lectura falla o devuelve -1
        }

        val result = SimulationTimerLogic.calculateResumedTime(saved, fakeClock)

        assertTrue("Debe detectar reinicio o fallback al no tener bootCount válido", result.wasRebootDetected)
        assertEquals("Debe descontar 100s de reloj de pared y no 50s de elapsedRealtime", 900L, result.remainingSeconds)
        assertTrue(result.noticeMessage!!.contains("tras reinicio"))
    }

    // =========================================================================
    // ÍTEM 2: VALIDACIÓN Y HASH PBKDF2 DE CONTRASEÑA
    // =========================================================================

    @Test
    fun testPasswordHasher_pbkdf2ValidationAndVerification() {
        assertTrue(PasswordHasher.isValidEmail("estudiante@uniguajira.edu.co"))
        assertFalse(PasswordHasher.isValidEmail("invalido-sin-arroba"))
        assertTrue(PasswordHasher.isValidPassword("ClaveSegura123"))
        assertFalse(PasswordHasher.isValidPassword("corta"))

        val rawPass = "MiPasswordSecreto123".toCharArray()
        // Probar hash con formato versionado v1$<iterations>$<saltHex>$<hashHex>
        val hashed = PasswordHasher.hashPassword(rawPass, iterations = PasswordHasher.DEFAULT_ITERATIONS)
        assertEquals(PasswordHasher.DEFAULT_ITERATIONS, hashed.iterations)
        val storedString = hashed.toStoredString()
        assertTrue("El hash versionado debe iniciar con v1$", storedString.startsWith("v1$600000$"))

        val parsed = PasswordHasher.parseStoredPassword(storedString)
        assertFalse("No debe ser legacy", parsed.isLegacy)
        assertEquals(PasswordHasher.DEFAULT_ITERATIONS, parsed.iterations)
        assertTrue(PasswordHasher.verifyPassword(rawPass, parsed.saltHex, parsed.hashHex, parsed.iterations))
        assertFalse(PasswordHasher.verifyPassword("PasswordEquivocado".toCharArray(), parsed.saltHex, parsed.hashHex, parsed.iterations))

        // Compatibilidad hacia atrás: hash legado con 10.000 iteraciones sin prefijo v1$
        val legacyHashed = PasswordHasher.hashPassword(rawPass, iterations = PasswordHasher.LEGACY_ITERATIONS)
        val parsedLegacy = PasswordHasher.parseStoredPassword(legacyHashed.hashHex, legacyHashed.saltHex)
        assertTrue("Debe marcarse como legacy para rehashing", parsedLegacy.isLegacy)
        assertEquals(10000, parsedLegacy.iterations)
        assertTrue(PasswordHasher.verifyPassword(rawPass, parsedLegacy.saltHex, parsedLegacy.hashHex, parsedLegacy.iterations))
    }

    // =========================================================================
    // ÍTEM 9: BARAJA DE OPCIONES SIN SESGO HACIA LA OPCIÓN A
    // =========================================================================

    @Test
    fun testQuestionShuffler_preservesCorrectAnswerTextWhileChangingKey() {
        val original = Question(
            id_pregunta = "TEST-01",
            modulo = "lectura_critica",
            competencia_tema = "tesis",
            enunciado = "¿Cuál es la respuesta?",
            opciones = mapOf(
                "A" to "Texto Respuesta Correcta",
                "B" to "Distractor 1",
                "C" to "Distractor 2",
                "D" to "Distractor 3"
            ),
            respuesta_correcta = "A",
            explicacion = "Explicación"
        )

        // Con una semilla fija que desplace la posición
        val shuffled = QuestionShuffler.shuffleQuestion(original, sessionSeed = 1234567L)

        // El texto que ahora corresponde a la respuesta_correcta debe ser idéntico al original
        val textOfNewCorrectKey = shuffled.opciones[shuffled.respuesta_correcta]
        assertEquals("Texto Respuesta Correcta", textOfNewCorrectKey)
    }
}
