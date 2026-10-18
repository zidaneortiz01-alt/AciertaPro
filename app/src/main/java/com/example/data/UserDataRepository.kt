package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.core.crypto.PasswordHasher
import com.example.data.local.SaberProDatabase
import com.example.data.local.entity.AttemptAnswerEntity
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.LocalAccountEntity
import com.example.data.local.entity.QuestionProgressEntity
import com.example.data.local.entity.QuestionReportEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WrittenEssayEntity
import com.example.domain.model.Attempt
import com.example.domain.model.AttemptAnswer
import com.example.domain.model.AttemptKind
import com.example.domain.model.Question
import com.example.domain.model.QuestionProgress
import com.example.domain.model.QuestionReport
import com.example.domain.model.SyncStatus
import com.example.domain.model.TopicMastery
import com.example.domain.model.UserProfile
import com.example.domain.model.WrittenEssay
import com.example.domain.rules.LeitnerBoxManager
import com.example.domain.rules.MasteryCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class UserDataRepository(
    private val context: Context,
    private val database: SaberProDatabase = SaberProDatabase.getInstance(context)
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("saber_pro_simulation_cache", Context.MODE_PRIVATE)

    private val KEY_ACTIVE_SIMULATION = "active_simulation_state"

    // =========================================================================
    // ÍTEM 2: CUENTA LOCAL CON HASH PBKDF2 Y SAL (Sin servidor)
    // =========================================================================

    suspend fun createLocalAccount(email: String, password: String): Result<Unit> = withContext(Dispatchers.Default) {
        val trimmedEmail = email.trim()
        if (!PasswordHasher.isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("El correo ingresado no tiene un formato válido."))
        }
        if (!PasswordHasher.isValidPassword(password)) {
            return@withContext Result.failure(IllegalArgumentException("La contraseña debe contener al menos 8 caracteres."))
        }

        val existing = database.localAccountDao().getByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("Ya existe una cuenta con este correo en este dispositivo."))
        }

        val hashResult = PasswordHasher.hashPassword(password.toCharArray())
        val account = LocalAccountEntity(
            email = trimmedEmail,
            passwordHash = hashResult.toStoredString(),
            saltHex = hashResult.saltHex,
            createdAt = System.currentTimeMillis()
        )
        database.localAccountDao().insert(account)
        Result.success(Unit)
    }

    private var currentUserEmail: String? = null

    fun setCurrentUserEmail(email: String?) {
        currentUserEmail = email
    }

    fun getCurrentUserEmail(): String? = currentUserEmail

    fun determineRole(email: String?): com.example.domain.model.UserRole {
        val clean = email?.trim()?.lowercase() ?: return com.example.domain.model.UserRole.STUDENT
        return if (clean == "proyectoing2026@gmail.com" || clean == "docente.saberpro@institucion.edu.co") {
            com.example.domain.model.UserRole.SUPERUSER
        } else {
            com.example.domain.model.UserRole.STUDENT
        }
    }

    suspend fun loginWithGoogle(email: String, displayName: String): Result<com.example.domain.model.UserProfile> = withContext(Dispatchers.Default) {
        val trimmedEmail = email.trim()
        currentUserEmail = trimmedEmail
        val role = determineRole(trimmedEmail)

        var entity = database.userProfileDao().getProfileByEmail(trimmedEmail)
        if (entity == null) {
            val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            entity = UserProfileEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = displayName.ifBlank { trimmedEmail.split("@")[0] },
                email = trimmedEmail,
                academicProgram = "Ingeniería de Sistemas",
                campus = "Sede Maicao",
                enabledModulesJson = org.json.JSONArray(listOf("lectura_critica", "razonamiento_cuantitativo", "competencias_ciudadanas", "ingles", "comunicacion_escrita", "ingenieria_sistemas")).toString(),
                targetDate = "Noviembre 2026",
                studyDaysPerWeek = 4,
                sessionMinutes = 30,
                hasConsented = true,
                consentDate = isoDate,
                consentVersion = "Google Auth v1.0",
                isDarkMode = false
            )
            database.userProfileDao().insertOrUpdate(entity)
        }

        val profile = getProfile()
        Result.success(profile)
    }

    suspend fun logout() {
        currentUserEmail = null
    }

    suspend fun loginLocalAccount(email: String, password: String): Result<Boolean> = withContext(Dispatchers.Default) {
        val trimmedEmail = email.trim()
        val account = database.localAccountDao().getByEmail(trimmedEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No existe ninguna cuenta registrada con este correo en este dispositivo."))

        val parsed = PasswordHasher.parseStoredPassword(account.passwordHash, account.saltHex)
        val matches = PasswordHasher.verifyPassword(
            password = password.toCharArray(),
            saltHex = parsed.saltHex,
            expectedHashHex = parsed.hashHex,
            iterations = parsed.iterations
        )
        if (matches) {
            currentUserEmail = trimmedEmail
            // Re-hashear automáticamente si el hash proviene de iteraciones heredadas o formato anterior
            if (parsed.isLegacy || parsed.iterations < PasswordHasher.DEFAULT_ITERATIONS) {
                val upgradedHash = PasswordHasher.hashPassword(password.toCharArray())
                val updatedAccount = account.copy(
                    passwordHash = upgradedHash.toStoredString(),
                    saltHex = upgradedHash.saltHex
                )
                database.localAccountDao().insert(updatedAccount)
            }
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException("Contraseña incorrecta para la cuenta local."))
        }
    }

    suspend fun getLocalAccount(): LocalAccountEntity? {
        return database.localAccountDao().getFirstAccount()
    }

    suspend fun deleteLocalAccountOnly() {
        database.localAccountDao().deleteAll()
        currentUserEmail = null
    }

    // =========================================================================
    // ÍTEM 3: CONSENTIMIENTO APLICADO EN LA CAPA DE DATOS (RECHAZO ESTRICTO)
    // =========================================================================

    suspend fun hasValidConsent(): Boolean {
        val profile = database.userProfileDao().getProfile() ?: return false
        return profile.hasConsented && profile.consentDate.isNotBlank() && profile.consentVersion.isNotBlank()
    }

    private suspend fun checkConsentOrThrow() {
        if (!hasValidConsent()) {
            throw SecurityException(
                "Escritura rechazada en capa de datos: No se ha registrado un consentimiento informado previo conforme a la Ley 1581 de 2012."
            )
        }
    }

    suspend fun registerInitialConsentAndProfile(profile: UserProfile): Result<Unit> {
        if (!profile.hasConsented || profile.consentDate.isBlank()) {
            return Result.failure(
                SecurityException("No se puede registrar el perfil sin consentimiento informado explícito.")
            )
        }

        val entity = UserProfileEntity(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            academicProgram = profile.academicProgram,
            campus = profile.campus,
            enabledModulesJson = JSONArray(profile.enabledModules).toString(),
            targetDate = profile.targetDate,
            studyDaysPerWeek = profile.studyDaysPerWeek,
            sessionMinutes = profile.sessionMinutes,
            hasConsented = true,
            consentDate = profile.consentDate,
            consentVersion = profile.consentVersion,
            isDarkMode = profile.isDarkMode
        )
        database.userProfileDao().insertOrUpdate(entity)
        return Result.success(Unit)
    }

    suspend fun acceptConsent(version: String = "1.0"): Result<Unit> {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
        val profile = UserProfile(
            name = "Usuario Local",
            email = "usuario@local.app",
            hasConsented = true,
            consentDate = now,
            consentVersion = version
        )
        return registerInitialConsentAndProfile(profile)
    }

    suspend fun updateProfile(profile: UserProfile) {
        checkConsentOrThrow()
        val entity = UserProfileEntity(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            academicProgram = profile.academicProgram,
            campus = profile.campus,
            enabledModulesJson = JSONArray(profile.enabledModules).toString(),
            targetDate = profile.targetDate,
            studyDaysPerWeek = profile.studyDaysPerWeek,
            sessionMinutes = profile.sessionMinutes,
            hasConsented = profile.hasConsented,
            consentDate = profile.consentDate,
            consentVersion = profile.consentVersion,
            isDarkMode = profile.isDarkMode
        )
        database.userProfileDao().insertOrUpdate(entity)
    }

    suspend fun getProfile(): UserProfile {
        val email = currentUserEmail
        val entity = if (!email.isNullOrBlank()) {
            database.userProfileDao().getProfileByEmail(email) ?: database.userProfileDao().getProfile()
        } else {
            database.userProfileDao().getProfile()
        } ?: return UserProfile(email = email ?: "", role = determineRole(email))

        val role = determineRole(entity.email)
        val modulesList = mutableListOf<String>()
        try {
            val arr = JSONArray(entity.enabledModulesJson)
            for (i in 0 until arr.length()) {
                modulesList.add(arr.getString(i))
            }
        } catch (_: Exception) {
            modulesList.addAll(
                listOf("lectura_critica", "razonamiento_cuantitativo", "competencias_ciudadanas", "ingles", "comunicacion_escrita", "ingenieria_sistemas")
            )
        }

        return UserProfile(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            role = role,
            isGoogleAccount = entity.consentVersion.contains("Google"),
            academicProgram = entity.academicProgram,
            campus = entity.campus,
            enabledModules = modulesList,
            targetDate = entity.targetDate,
            studyDaysPerWeek = entity.studyDaysPerWeek,
            sessionMinutes = entity.sessionMinutes,
            hasConsented = entity.hasConsented,
            consentDate = entity.consentDate,
            consentVersion = entity.consentVersion,
            isDarkMode = entity.isDarkMode
        )
    }

    // =========================================================================
    // ÍTEM 1 & 5: INTENTOS, RESPUESTAS Y QUESTION_PROGRESS EN ROOM
    // =========================================================================

    suspend fun saveAttempt(attempt: Attempt, answers: List<AttemptAnswer>) {
        checkConsentOrThrow()

        // Inserción incremental e idempotente de intento (UUID generado en cliente, conflicto ignorado)
        val attemptEntity = AttemptEntity(
            id = attempt.id,
            userId = attempt.userId,
            kind = attempt.kind.name,
            moduleId = attempt.moduleId,
            startedAt = attempt.startedAt,
            finishedAt = attempt.finishedAt,
            totalQuestions = attempt.totalQuestions,
            correctAnswers = attempt.correctAnswers,
            durationSeconds = attempt.durationSeconds,
            syncStatus = attempt.syncStatus.name
        )
        database.attemptDao().insertAttempt(attemptEntity)

        // Inserción de respuestas individuales con timestamp real de respuesta
        val answerEntities = answers.map { ans ->
            AttemptAnswerEntity(
                attemptId = ans.attemptId,
                questionId = ans.questionId,
                selectedOption = ans.selectedOption,
                selectedOriginalKey = ans.selectedOriginalKey,
                selectedOptionText = ans.selectedOptionText,
                isCorrect = ans.isCorrect,
                timeSpentSeconds = ans.timeSpentSeconds,
                markedForReview = ans.markedForReview,
                answeredAt = ans.answeredAt
            )
        }
        database.attemptAnswerDao().insertAnswers(answerEntities)

        // Ítem 1: Actualizar progreso Leitner POR PREGUNTA (question_progress)
        for (ans in answers) {
            val existing = database.questionProgressDao().getProgress(ans.questionId)
            val currentBox = existing?.box ?: 1
            val updated = LeitnerBoxManager.calculateNextProgress(
                questionId = ans.questionId,
                currentBox = currentBox,
                isCorrect = ans.isCorrect,
                answeredAt = ans.answeredAt
            )
            database.questionProgressDao().insertOrUpdate(
                QuestionProgressEntity(
                    questionId = updated.questionId,
                    box = updated.box,
                    lastAnsweredAt = updated.lastAnsweredAt,
                    dueAt = updated.dueAt
                )
            )
        }
    }

    suspend fun getAttempts(): List<Attempt> {
        return database.attemptDao().getAllAttempts().map { e ->
            Attempt(
                id = e.id,
                userId = e.userId,
                kind = try { AttemptKind.valueOf(e.kind) } catch (_: Exception) { AttemptKind.PRACTICE },
                moduleId = e.moduleId,
                startedAt = e.startedAt,
                finishedAt = e.finishedAt,
                totalQuestions = e.totalQuestions,
                correctAnswers = e.correctAnswers,
                durationSeconds = e.durationSeconds,
                syncStatus = try { SyncStatus.valueOf(e.syncStatus) } catch (_: Exception) { SyncStatus.SOLO_LOCAL }
            )
        }
    }

    suspend fun getAttemptAnswers(): List<AttemptAnswer> {
        return database.attemptAnswerDao().getAllAnswers().map { e ->
            AttemptAnswer(
                attemptId = e.attemptId,
                questionId = e.questionId,
                selectedOption = e.selectedOption,
                selectedOriginalKey = e.selectedOriginalKey,
                selectedOptionText = e.selectedOptionText,
                isCorrect = e.isCorrect,
                timeSpentSeconds = e.timeSpentSeconds,
                markedForReview = e.markedForReview,
                answeredAt = e.answeredAt
            )
        }
    }

    suspend fun getAllQuestionProgress(): List<QuestionProgress> {
        return database.questionProgressDao().getAllProgress().map { e ->
            QuestionProgress(
                questionId = e.questionId,
                box = e.box,
                lastAnsweredAt = e.lastAnsweredAt,
                dueAt = e.dueAt
            )
        }
    }

    suspend fun getTopicMasteries(
        questionsMap: Map<String, Question>,
        clock: () -> Long = { System.currentTimeMillis() }
    ): List<TopicMastery> {
        val answers = database.attemptAnswerDao().getAllAnswers()
        val allProgress = database.questionProgressDao().getAllProgress().associateBy { it.questionId }
        val currentTime = clock()

        // Agrupar respuestas por tema
        val groupedByTopic = mutableMapOf<String, MutableList<Pair<AttemptAnswerEntity, Question>>>()
        for (ans in answers) {
            val q = questionsMap[ans.questionId] ?: continue
            val key = "${q.modulo}::${q.competencia_tema}"
            groupedByTopic.getOrPut(key) { mutableListOf() }.add(Pair(ans, q))
        }

        // Agrupar preguntas del catálogo por tema para calcular vencidas
        val catalogByTopic = mutableMapOf<String, MutableList<Question>>()
        for (q in questionsMap.values) {
            val key = "${q.modulo}::${q.competencia_tema}"
            catalogByTopic.getOrPut(key) { mutableListOf() }.add(q)
        }

        val result = mutableListOf<TopicMastery>()

        for ((key, topicQuestions) in catalogByTopic) {
            val parts = key.split("::")
            val moduleId = parts[0]
            val topicId = parts[1]

            val answersForTopic = groupedByTopic[key] ?: emptyList()
            val history = answersForTopic.map { Pair(it.first.isCorrect, it.second.dificultad) }
            val mastery = MasteryCalculator.calculateTopicMastery(history)

            // Contar preguntas vencidas en este tema según question_progress
            var overdueCount = 0
            var totalBoxes = 0
            var answeredCount = 0

            for (q in topicQuestions) {
                val prog = allProgress[q.id_pregunta]
                if (prog != null) {
                    answeredCount++
                    totalBoxes += prog.box
                    if (LeitnerBoxManager.isOverdue(prog.dueAt, currentTime)) {
                        overdueCount++
                    }
                }
            }

            val avgBox = if (answeredCount > 0) totalBoxes.toDouble() / answeredCount else 1.0

            result.add(
                TopicMastery(
                    topicId = topicId,
                    moduleId = moduleId,
                    totalAttempts = answersForTopic.size,
                    correctCount = answersForTopic.count { it.first.isCorrect },
                    masteryPercentage = mastery,
                    averageLeitnerBox = avgBox,
                    overdueCount = overdueCount
                )
            )
        }

        return result
    }

    // =========================================================================
    // COMUNICACIÓN ESCRITA Y REPORTES (CON CHEQUEO DE CONSENTIMIENTO)
    // =========================================================================

    suspend fun saveWrittenEssay(essay: WrittenEssay) {
        checkConsentOrThrow()
        val entity = WrittenEssayEntity(
            id = essay.id,
            title = essay.title,
            content = essay.content,
            lastSavedAt = essay.lastSavedAt,
            wordCount = essay.wordCount,
            selfScoreThesis = essay.selfScoreThesis,
            selfScoreArguments = essay.selfScoreArguments,
            selfScoreStructure = essay.selfScoreStructure,
            selfScoreLanguage = essay.selfScoreLanguage
        )
        database.writtenEssayDao().insertOrUpdate(entity)
    }

    suspend fun getWrittenEssay(): WrittenEssay {
        val entity = database.writtenEssayDao().getEssay()
            ?: return WrittenEssay(
                title = "Ensayo Argumentativo: Impacto de la Inteligencia Artificial [tema de práctica de ejemplo]",
                content = ""
            )
        return WrittenEssay(
            id = entity.id,
            title = entity.title,
            content = entity.content,
            lastSavedAt = entity.lastSavedAt,
            wordCount = entity.wordCount,
            selfScoreThesis = entity.selfScoreThesis,
            selfScoreArguments = entity.selfScoreArguments,
            selfScoreStructure = entity.selfScoreStructure,
            selfScoreLanguage = entity.selfScoreLanguage
        )
    }

    suspend fun reportQuestion(report: QuestionReport) {
        checkConsentOrThrow()
        database.questionReportDao().insert(
            QuestionReportEntity(
                id = report.id,
                questionId = report.questionId,
                module = report.module,
                reason = report.reason,
                details = report.details,
                timestamp = report.timestamp
            )
        )
    }

    suspend fun getAllReports(): List<QuestionReport> {
        return database.questionReportDao().getAllReports().map { e ->
            QuestionReport(
                id = e.id,
                questionId = e.questionId,
                module = e.module,
                reason = e.reason,
                details = e.details,
                timestamp = e.timestamp
            )
        }
    }

    // =========================================================================
    // PERSISTENCIA DE SIMULACRO ACTIVO
    // =========================================================================

    fun saveActiveSimulationRaw(json: String?) {
        if (json == null) {
            prefs.edit().remove(KEY_ACTIVE_SIMULATION).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_SIMULATION, json).apply()
        }
    }

    fun getActiveSimulationRaw(): String? {
        return prefs.getString(KEY_ACTIVE_SIMULATION, null)
    }

    // =========================================================================
    // ÍTEM 4: EXPORTACIÓN Y ELIMINACIÓN COMPLETAS (RF-11)
    // =========================================================================

    suspend fun exportAllDataJson(): String {
        val root = JSONObject()
        root.put("export_version", "2.0")
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        root.put("exported_at_iso", isoFormat.format(Date()))
        root.put(
            "aviso_legal",
            "Exportación completa de datos personales y de aprendizaje almacenados localmente de conformidad con la Ley 1581 de 2012."
        )

        // Cuenta local (metadatos sin secretos)
        val acc = getLocalAccount()
        if (acc != null) {
            root.put("cuenta_local", JSONObject().apply {
                put("correo", acc.email)
                put("fecha_creacion_ms", acc.createdAt)
                put("almacenamiento", "local_dispositivo")
            })
        }

        // Perfil completo
        val p = getProfile()
        root.put("perfil_completo", JSONObject().apply {
            put("id", p.id)
            put("nombre", p.name)
            put("correo", p.email)
            put("programa_academico", p.academicProgram)
            put("sede", p.campus)
            put("fecha_objetivo", p.targetDate)
            put("dias_estudio_semanales", p.studyDaysPerWeek)
            put("minutos_por_sesion", p.sessionMinutes)
            put("modo_oscuro", p.isDarkMode)
            put("modulos_habilitados", JSONArray(p.enabledModules))
            put("consentimiento_aceptado", p.hasConsented)
            put("consentimiento_fecha", p.consentDate)
            put("consentimiento_version", p.consentVersion)
        })

        // Intentos
        val attemptsArr = JSONArray()
        for (att in getAttempts()) {
            attemptsArr.put(JSONObject().apply {
                put("id", att.id)
                put("usuario_id", att.userId)
                put("modalidad", att.kind.name)
                put("modulo", att.moduleId)
                put("fecha_inicio_ms", att.startedAt)
                put("fecha_fin_ms", att.finishedAt)
                put("total_preguntas", att.totalQuestions)
                put("aciertos_internos", att.correctAnswers)
                put("duracion_segundos", att.durationSeconds)
                put("estado_sincronizacion", att.syncStatus.name)
            })
        }
        root.put("intentos", attemptsArr)

        // Respuestas individuales con answeredAt, markedForReview, selectedOriginalKey y selectedOptionText
        val answersArr = JSONArray()
        for (ans in getAttemptAnswers()) {
            answersArr.put(JSONObject().apply {
                put("intento_id", ans.attemptId)
                put("pregunta_id", ans.questionId)
                put("opcion_elegida", ans.selectedOption)
                put("selectedOption", ans.selectedOption)
                put("selectedOriginalKey", ans.selectedOriginalKey)
                put("selectedOptionText", ans.selectedOptionText)
                put("clave_original_elegida", ans.selectedOriginalKey)
                put("texto_opcion_elegida", ans.selectedOptionText)
                put("es_correcta", ans.isCorrect)
                put("tiempo_segundos", ans.timeSpentSeconds)
                put("marcada_para_revision", ans.markedForReview)
                put("respondida_en_ms", ans.answeredAt)
            })
        }
        root.put("respuestas_individuales", answersArr)

        // Progreso por pregunta Leitner
        val leitnerArr = JSONArray()
        for (lp in getAllQuestionProgress()) {
            leitnerArr.put(JSONObject().apply {
                put("pregunta_id", lp.questionId)
                put("caja_leitner", lp.box)
                put("ultima_respuesta_ms", lp.lastAnsweredAt)
                put("vence_en_ms", lp.dueAt)
            })
        }
        root.put("progreso_preguntas_leitner", leitnerArr)

        // Comunicación Escrita
        val essay = getWrittenEssay()
        root.put("comunicacion_escrita", JSONObject().apply {
            put("id", essay.id)
            put("titulo", essay.title)
            put("contenido", essay.content)
            put("conteo_palabras", essay.wordCount)
            put("ultima_modificacion_ms", essay.lastSavedAt)
            put("autoevaluacion_rubrica", JSONObject().apply {
                put("tesis", essay.selfScoreThesis)
                put("argumentos", essay.selfScoreArguments)
                put("estructura", essay.selfScoreStructure)
                put("lenguaje", essay.selfScoreLanguage)
            })
        })

        // Reportes de preguntas
        val reportsArr = JSONArray()
        for (rep in getAllReports()) {
            reportsArr.put(JSONObject().apply {
                put("id", rep.id)
                put("pregunta_id", rep.questionId)
                put("modulo", rep.module)
                put("motivo", rep.reason)
                put("detalle", rep.details)
                put("fecha_ms", rep.timestamp)
            })
        }
        root.put("reportes_preguntas", reportsArr)

        return root.toString(2)
    }

    suspend fun clearAllUserData() {
        // 1. Borrar todas las tablas de Room
        database.localAccountDao().deleteAll()
        database.userProfileDao().deleteAll()
        database.attemptDao().deleteAll()
        database.attemptAnswerDao().deleteAll()
        database.questionProgressDao().deleteAll()
        database.writtenEssayDao().deleteAll()
        database.questionReportDao().deleteAll()

        // 2. Borrar simulación activa y preferencias
        saveActiveSimulationRaw(null)
        prefs.edit().clear().apply()
    }
}
