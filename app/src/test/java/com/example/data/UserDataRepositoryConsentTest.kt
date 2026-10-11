package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SaberProDatabase
import com.example.domain.model.Attempt
import com.example.domain.model.AttemptAnswer
import com.example.domain.model.AttemptKind
import com.example.domain.model.QuestionReport
import com.example.domain.model.UserProfile
import com.example.domain.model.WrittenEssay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas unitarias para:
 * - Ítem 3: Consentimiento aplicado en la capa de datos (rechazo estricto si no hay consentimiento).
 * - Ítem 4: Exportación completa en JSON y eliminación total ("Eliminar mis datos") sin dejar rastros.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserDataRepositoryConsentTest {

    private lateinit var db: SaberProDatabase
    private lateinit var repository: UserDataRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SaberProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserDataRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test(expected = SecurityException::class)
    fun testSaveAttempt_withoutConsent_isRejectedInDataLayer(): Unit = runBlocking {
        val attempt = Attempt(
            kind = AttemptKind.PRACTICE,
            moduleId = "lectura_critica"
        )
        repository.saveAttempt(attempt, emptyList())
    }

    @Test(expected = SecurityException::class)
    fun testSaveEssay_withoutConsent_isRejectedInDataLayer(): Unit = runBlocking {
        val essay = WrittenEssay(title = "Test", content = "Texto")
        repository.saveWrittenEssay(essay)
    }

    @Test(expected = SecurityException::class)
    fun testReportQuestion_withoutConsent_isRejectedInDataLayer(): Unit = runBlocking {
        val report = QuestionReport(
            questionId = "LC-001",
            module = "lectura_critica",
            reason = "Error",
            details = "Detalles"
        )
        repository.reportQuestion(report)
    }

    @Test
    fun testWithConsent_allWritesSucceed_andClearDataDeletesEverything(): Unit = runBlocking {
        val profile = UserProfile(
            name = "Estudiante Real",
            email = "usuario@correo.com",
            hasConsented = true,
            consentDate = "2026-10-10T12:00:00Z",
            consentVersion = "Ley 1581 v1.0"
        )

        // 1. Registro de consentimiento inicial
        val consentRes = repository.registerInitialConsentAndProfile(profile)
        assertTrue(consentRes.isSuccess)
        assertTrue(repository.hasValidConsent())

        // 2. Ahora las escrituras deben permitirse
        val attempt = Attempt(
            kind = AttemptKind.PRACTICE,
            moduleId = "lectura_critica",
            totalQuestions = 1,
            correctAnswers = 1
        )
        val answer = AttemptAnswer(
            attemptId = attempt.id,
            questionId = "LC-001",
            selectedOption = "B",
            isCorrect = true,
            answeredAt = 1700000000000L
        )
        repository.saveAttempt(attempt, listOf(answer))

        val essay = WrittenEssay(title = "Mi Ensayo", content = "Contenido")
        repository.saveWrittenEssay(essay)

        val report = QuestionReport(
            questionId = "LC-001",
            module = "lectura_critica",
            reason = "Duda",
            details = "Texto"
        )
        repository.reportQuestion(report)

        // Verificar que los datos existen en Room
        assertEquals(1, repository.getAttempts().size)
        assertEquals(1, repository.getAttemptAnswers().size)
        assertEquals(1, repository.getAllQuestionProgress().size)
        assertEquals(1, repository.getAllReports().size)

        // 3. Exportación completa JSON (Ítem 4)
        val exportedJson = repository.exportAllDataJson()
        assertTrue(exportedJson.contains("Mi Ensayo"))
        assertTrue(exportedJson.contains("LC-001"))
        assertTrue(exportedJson.contains("Ley 1581"))

        // 4. "Eliminar mis datos" borra TODO (Ítem 4)
        repository.clearAllUserData()

        assertFalse("El consentimiento debe quedar revocado", repository.hasValidConsent())
        assertTrue("Los intentos deben ser 0", repository.getAttempts().isEmpty())
        assertTrue("Las respuestas deben ser 0", repository.getAttemptAnswers().isEmpty())
        assertTrue("El progreso Leitner debe ser 0", repository.getAllQuestionProgress().isEmpty())
        assertTrue("Los reportes deben ser 0", repository.getAllReports().isEmpty())
        assertNull("La cuenta local debe haber sido eliminada", repository.getLocalAccount())
    }

    @Test
    fun testLegacyAccountLogin_upgradesHashToOwaspIterations(): Unit = runBlocking {
        val email = "estudiante.antiguo@uniguajira.edu.co"
        val password = "ClaveAntigua123"

        // Simular una cuenta existente con el formato heredado (10.000 iteraciones y sin formato v1$)
        val legacyHash = com.example.core.crypto.PasswordHasher.hashPassword(
            password = password.toCharArray(),
            iterations = com.example.core.crypto.PasswordHasher.LEGACY_ITERATIONS
        )
        val legacyAccount = com.example.data.local.entity.LocalAccountEntity(
            email = email,
            passwordHash = legacyHash.hashHex, // Formato heredado sin v1$
            saltHex = legacyHash.saltHex
        )
        db.localAccountDao().insert(legacyAccount)

        // Verificar estado inicial en BD
        val savedBefore = repository.getLocalAccount()
        assertNotNull(savedBefore)
        assertFalse(savedBefore!!.passwordHash.startsWith("v1$"))

        // Iniciar sesión con la contraseña correcta
        val loginResult = repository.loginLocalAccount(email, password)
        assertTrue(loginResult.isSuccess)
        assertTrue(loginResult.getOrThrow())

        // Confirmar que la cuenta en BD fue re-hasheada al formato versionado con 600.000 iteraciones
        val savedAfter = repository.getLocalAccount()
        assertNotNull(savedAfter)
        assertTrue("Debe tener el prefijo de hash versionado", savedAfter!!.passwordHash.startsWith("v1$600000$"))

        // Verificar que un login subsiguiente funciona perfectamente con el nuevo hash
        val secondLogin = repository.loginLocalAccount(email, password)
        assertTrue(secondLogin.isSuccess)
    }

    @Test
    fun testPasswordVerification_doesNotRunOnMainThread(): Unit = runBlocking {
        val email = "seguridad@uniguajira.edu.co"
        val password = "PasswordSeguro2026!"

        // Crear la cuenta (se ejecuta dentro de withContext(Dispatchers.Default))
        val createResult = repository.createLocalAccount(email, password)
        assertTrue(createResult.isSuccess)

        // Resetear registro de hilo en PasswordHasher
        com.example.core.crypto.PasswordHasher.lastVerificationThread = null

        // Invocar login explícitamente desde el hilo principal (Dispatchers.Main)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            val mainThread = Thread.currentThread()
            val result = repository.loginLocalAccount(email, password)
            assertTrue(result.isSuccess)

            val verificationThread = com.example.core.crypto.PasswordHasher.lastVerificationThread
            assertNotNull("Se debe haber registrado el hilo donde ocurrió la verificación", verificationThread)
            assertNotEquals(
                "La verificación de hash NO debe ejecutarse en el hilo principal (Dispatchers.Main)",
                mainThread,
                verificationThread
            )
            assertNotEquals(
                "La verificación NO debe correr en el hilo de Looper.getMainLooper()",
                android.os.Looper.getMainLooper().thread,
                verificationThread
            )
        }
    }

    @Test
    fun testGetAllAnswers_whenInsertedOutOfOrder_returnsSortedByAnsweredAtAsc(): Unit = runBlocking {
        repository.acceptConsent("1.0")

        val attemptId = "att-orden-1"
        val attempt = Attempt(
            id = attemptId,
            kind = AttemptKind.PRACTICE,
            moduleId = "lectura_critica",
            totalQuestions = 3,
            correctAnswers = 2
        )

        // Insertar respuestas en orden cronológico desordenado:
        // ans3: t=3000ms, ans1: t=1000ms, ans2: t=2000ms
        val answers = listOf(
            AttemptAnswer(
                attemptId = attemptId,
                questionId = "Q-3",
                selectedOption = "C",
                selectedOriginalKey = "C",
                selectedOptionText = "Tercera",
                isCorrect = true,
                answeredAt = 3000L
            ),
            AttemptAnswer(
                attemptId = attemptId,
                questionId = "Q-1",
                selectedOption = "A",
                selectedOriginalKey = "A",
                selectedOptionText = "Primera",
                isCorrect = true,
                answeredAt = 1000L
            ),
            AttemptAnswer(
                attemptId = attemptId,
                questionId = "Q-2",
                selectedOption = "B",
                selectedOriginalKey = "B",
                selectedOptionText = "Segunda",
                isCorrect = false,
                answeredAt = 2000L
            )
        )

        repository.saveAttempt(attempt, answers)

        val retrievedAnswers = repository.getAttemptAnswers()
        assertEquals(3, retrievedAnswers.size)
        // Debe retornar estrictamente ordenado por answeredAt ASC
        assertEquals("Q-1", retrievedAnswers[0].questionId)
        assertEquals(1000L, retrievedAnswers[0].answeredAt)

        assertEquals("Q-2", retrievedAnswers[1].questionId)
        assertEquals(2000L, retrievedAnswers[1].answeredAt)

        assertEquals("Q-3", retrievedAnswers[2].questionId)
        assertEquals(3000L, retrievedAnswers[2].answeredAt)
    }

    @Test
    fun testExportAllDataJson_withShuffledOptions_exportsCorrectOriginalKey(): Unit = runBlocking {
        repository.acceptConsent("1.0")

        // Pregunta con opciones originales A, B, C, D
        val originalQuestion = com.example.domain.model.Question(
            id_pregunta = "LC-SHUFFLE-01",
            modulo = "lectura_critica",
            competencia_tema = "identificar_contenidos",
            enunciado = "¿Cuál es la tesis central?",
            opciones = mapOf(
                "A" to "Texto de la opción A original",
                "B" to "Texto de la opción B original",
                "C" to "Texto de la opción C original",
                "D" to "Texto de la opción D original"
            ),
            respuesta_correcta = "C",
            explicacion = "Explicación detallada"
        )

        // Barajar con semilla fija
        val shuffledQuestion = com.example.domain.rules.QuestionShuffler.shuffleQuestion(
            originalQuestion,
            sessionSeed = 12345L
        )

        // Encontrar qué letra asignada en pantalla corresponde a la opción original 'C'
        val displayedKeyForC = shuffledQuestion.clave_original_por_mostrada.entries
            .find { it.value == "C" }?.key ?: "A"
        val optionTextForC = shuffledQuestion.opciones[displayedKeyForC] ?: ""
        assertEquals("Texto de la opción C original", optionTextForC)

        val attemptId = "att-shuffle-test"
        val attempt = Attempt(
            id = attemptId,
            kind = AttemptKind.PRACTICE,
            moduleId = "lectura_critica",
            totalQuestions = 1,
            correctAnswers = 1
        )
        val answer = AttemptAnswer(
            attemptId = attemptId,
            questionId = originalQuestion.id_pregunta,
            selectedOption = displayedKeyForC, // Letra mostrada (p. ej. "A" o "B")
            selectedOriginalKey = "C", // Clave original
            selectedOptionText = optionTextForC, // Texto original elegido
            isCorrect = true,
            answeredAt = 5000L
        )
        repository.saveAttempt(attempt, listOf(answer))

        // Exportar a JSON
        val exportedJson = repository.exportAllDataJson()
        val root = org.json.JSONObject(exportedJson)
        val answersArray = root.getJSONArray("respuestas_individuales")
        assertEquals(1, answersArray.length())

        val exportedAnswer = answersArray.getJSONObject(0)
        assertEquals("LC-SHUFFLE-01", exportedAnswer.getString("pregunta_id"))
        assertEquals(displayedKeyForC, exportedAnswer.getString("opcion_elegida"))
        assertEquals("C", exportedAnswer.getString("selectedOriginalKey"))
        assertEquals("Texto de la opción C original", exportedAnswer.getString("selectedOptionText"))
        assertEquals("C", exportedAnswer.getString("clave_original_elegida"))
    }
}
