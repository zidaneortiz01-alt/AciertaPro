package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.QuestionRepository
import com.example.data.UserDataRepository
import com.example.domain.model.Attempt
import com.example.domain.model.AttemptAnswer
import com.example.domain.model.AttemptKind
import com.example.domain.model.GuideModuleConfig
import com.example.domain.model.Question
import com.example.domain.model.QuestionReport
import com.example.domain.model.Recommendation
import com.example.domain.model.SyncStatus
import com.example.domain.model.TopicMastery
import com.example.domain.model.UserProfile
import com.example.domain.model.WrittenEssay
import com.example.domain.rules.QuestionShuffler
import com.example.domain.rules.SavedSimulationState
import com.example.domain.rules.SessionRecommender
import com.example.domain.rules.SimulationClock
import com.example.domain.rules.SimulationTimerLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min
import java.util.Random

enum class Screen {
    WELCOME,
    AUTH,
    INITIAL_CONFIG,
    DASHBOARD,
    DIAGNOSTIC,
    MODULES_LIST,
    TOPICS_LIST,
    PRACTICE_QUESTION,
    FEEDBACK,
    SIMULATION_CONFIG,
    SIMULATION_IN_PROGRESS,
    ATTEMPT_RESULTS,
    PROGRESS_HISTORY,
    STUDY_PLAN,
    PROFILE_SETTINGS,
    WRITTEN_COMMUNICATION
}

data class ActiveSessionState(
    val attemptId: String = UUID.randomUUID().toString(),
    val kind: AttemptKind = AttemptKind.PRACTICE,
    val moduleId: String = "lectura_critica",
    val topicId: String? = null,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(), // questionId -> selectedOption
    val answerTimestamps: Map<String, Long> = emptyMap(), // questionId -> answeredAt
    val markedForReview: Set<String> = emptySet(),
    val totalTimeSeconds: Long = 0,
    val remainingSeconds: Long = 0,
    val targetElapsedRealtimeMs: Long = 0L,
    val sessionSeed: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isBankInsufficient: Boolean = false,
    val bankNotice: String? = null,
    val resumeNotice: String? = null
)

data class UiState(
    val currentScreen: Screen = Screen.WELCOME,
    val previousScreen: Screen? = null,
    val profile: UserProfile = UserProfile(),
    val isDarkMode: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SOLO_LOCAL, // Verídico: sin servidor, solo local (Ítem 10)
    val guideVersionInfo: String = "",
    val guideModules: List<GuideModuleConfig> = emptyList(),
    val allQuestions: List<Question> = emptyList(),
    val selectedModuleId: String = "lectura_critica",
    val selectedTopicId: String? = null,
    val activeSession: ActiveSessionState? = null,
    val lastAttempt: Attempt? = null,
    val lastAttemptAnswers: List<AttemptAnswer> = emptyList(),
    val topicMasteries: List<TopicMastery> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val attemptsHistory: List<Attempt> = emptyList(),
    val writtenEssay: WrittenEssay = WrittenEssay(),
    val hasLocalAccount: Boolean = false,
    val reportSubmittedMessage: String? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class AndroidSimulationClock(private val context: Context) : SimulationClock {
    override fun elapsedRealtimeMs(): Long = android.os.SystemClock.elapsedRealtime()
    override fun wallClockMillis(): Long = System.currentTimeMillis()
    override fun getBootCount(): Int {
        return try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.BOOT_COUNT,
                -1
            )
        } catch (_: Exception) {
            -1
        }
    }
}

class SaberProViewModel(
    application: Application,
    private val clock: SimulationClock
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, AndroidSimulationClock(application))

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SaberProViewModel(application) as T
                }
            }
    }

    private val questionRepo = QuestionRepository(application)
    private val userDataRepo = UserDataRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val questions = questionRepo.getQuestions()
            val guideModules = questionRepo.getGuideModules()
            val profile = userDataRepo.getProfile()
            val attempts = userDataRepo.getAttempts()
            val account = userDataRepo.getLocalAccount()
            val essay = userDataRepo.getWrittenEssay()

            val questionsMap = questions.associateBy { it.id_pregunta }
            val masteries = userDataRepo.getTopicMasteries(questionsMap) { clock.wallClockMillis() }
            val recommendations = SessionRecommender.generateRecommendations(
                topicsMastery = masteries,
                enabledModuleIds = profile.enabledModules,
                clock = { clock.wallClockMillis() }
            )

            // Ítem 7: Reanudación robusta del simulacro persistido
            val rawSimJson = userDataRepo.getActiveSimulationRaw()
            var resumedSession: ActiveSessionState? = null
            var expiredSimulationToFinish: ActiveSessionState? = null

            if (rawSimJson != null) {
                try {
                    val obj = JSONObject(rawSimJson)
                    val qIds = obj.getJSONArray("questionIds")
                    val qIdsList = mutableListOf<String>()
                    for (i in 0 until qIds.length()) qIdsList.add(qIds.getString(i))

                    val answersObj = obj.optJSONObject("answers") ?: JSONObject()
                    val answersMap = mutableMapOf<String, String>()
                    val it = answersObj.keys()
                    while (it.hasNext()) {
                        val k = it.next()
                        answersMap[k] = answersObj.getString(k)
                    }

                    val markedArr = obj.optJSONArray("markedForReview") ?: JSONArray()
                    val markedSet = mutableSetOf<String>()
                    for (i in 0 until markedArr.length()) markedSet.add(markedArr.getString(i))

                    val savedState = SavedSimulationState(
                        attemptId = obj.getString("attemptId"),
                        moduleId = obj.optString("moduleId", "lectura_critica"),
                        remainingSecondsAtSave = obj.optLong("remainingSecondsAtSave", 0L),
                        savedElapsedRealtimeMs = obj.optLong("savedElapsedRealtimeMs", 0L),
                        savedWallTimeMs = obj.optLong("savedWallTimeMs", 0L),
                        savedBootCount = obj.optInt("savedBootCount", 0),
                        currentIndex = obj.optInt("currentIndex", 0),
                        questionIds = qIdsList,
                        answers = answersMap,
                        markedForReview = markedSet,
                        sessionSeed = obj.optLong("sessionSeed", 0L)
                    )

                    val resumeResult = SimulationTimerLogic.calculateResumedTime(savedState, clock)
                    val sessionQuestions = qIdsList.mapNotNull { questionsMap[it] }.map {
                        QuestionShuffler.shuffleQuestion(it, savedState.sessionSeed)
                    }

                    if (sessionQuestions.isNotEmpty()) {
                        val sessionState = ActiveSessionState(
                            attemptId = savedState.attemptId,
                            kind = AttemptKind.SIMULATION,
                            moduleId = savedState.moduleId,
                            questions = sessionQuestions,
                            currentIndex = savedState.currentIndex.coerceIn(0, sessionQuestions.size - 1),
                            answers = savedState.answers,
                            markedForReview = savedState.markedForReview,
                            totalTimeSeconds = obj.optLong("totalTimeSeconds", 3600L),
                            remainingSeconds = resumeResult.remainingSeconds,
                            targetElapsedRealtimeMs = clock.elapsedRealtimeMs() + (resumeResult.remainingSeconds * 1000L),
                            sessionSeed = savedState.sessionSeed,
                            resumeNotice = resumeResult.noticeMessage
                        )

                        if (resumeResult.isExpired) {
                            expiredSimulationToFinish = sessionState
                        } else {
                            resumedSession = sessionState
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val initialScreen = when {
                resumedSession != null -> Screen.SIMULATION_IN_PROGRESS
                !profile.hasConsented -> Screen.WELCOME
                else -> Screen.DASHBOARD
            }

            _uiState.update {
                it.copy(
                    currentScreen = initialScreen,
                    profile = profile,
                    isDarkMode = profile.isDarkMode,
                    hasLocalAccount = account != null,
                    guideVersionInfo = questionRepo.guideVersionInfo,
                    guideModules = guideModules,
                    allQuestions = questions,
                    topicMasteries = masteries,
                    recommendations = recommendations,
                    attemptsHistory = attempts,
                    writtenEssay = essay,
                    activeSession = resumedSession
                )
            }

            // Si el simulacro persistido venció mientras la app estaba cerrada, finalizarlo sin perder respuestas (Ítem 7)
            if (expiredSimulationToFinish != null) {
                _uiState.update { it.copy(activeSession = expiredSimulationToFinish) }
                finishSession()
            } else if (resumedSession != null) {
                startSimulationTimer()
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update {
            it.copy(
                previousScreen = it.currentScreen,
                currentScreen = screen,
                feedbackMessage = null,
                errorMessage = null
            )
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newMode = !_uiState.value.isDarkMode
            val updated = _uiState.value.profile.copy(isDarkMode = newMode)
            if (userDataRepo.hasValidConsent()) {
                userDataRepo.updateProfile(updated)
            }
            _uiState.update { it.copy(isDarkMode = newMode, profile = updated) }
        }
    }

    // =========================================================================
    // AUTENTICACIÓN LOCAL CON HASH PBKDF2 (Ítem 2)
    // =========================================================================

    fun registerLocalAccount(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = userDataRepo.createLocalAccount(email, pass)
            result.onSuccess {
                _uiState.update { it.copy(hasLocalAccount = true, feedbackMessage = "Cuenta creada exitosamente en este dispositivo.") }
                onSuccess()
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun loginLocalAccount(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = userDataRepo.loginLocalAccount(email, pass)
            result.onSuccess {
                _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
                onSuccess()
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun deleteLocalAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            userDataRepo.deleteLocalAccountOnly()
            _uiState.update { it.copy(hasLocalAccount = false, feedbackMessage = "Cuenta local eliminada.") }
            onDeleted()
        }
    }

    // =========================================================================
    // CONSENTIMIENTO INFORMADO EN CAPA DE DATOS (Ítem 3)
    // =========================================================================

    fun grantConsentAndSaveConfig(
        program: String,
        campus: String,
        targetDate: String,
        studyDays: Int,
        enabledModules: List<String>
    ) {
        viewModelScope.launch {
            val current = _uiState.value.profile
            val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val updated = current.copy(
                academicProgram = program,
                campus = campus,
                targetDate = targetDate,
                studyDaysPerWeek = studyDays,
                enabledModules = enabledModules,
                hasConsented = true,
                consentDate = isoDate,
                consentVersion = "Ley 1581 v1.0"
            )

            val result = userDataRepo.registerInitialConsentAndProfile(updated)
            result.onSuccess {
                refreshDomainStats(updated)
                navigateTo(Screen.DASHBOARD)
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun selectModule(moduleId: String) {
        _uiState.update { it.copy(selectedModuleId = moduleId) }
        navigateTo(Screen.TOPICS_LIST)
    }

    // =========================================================================
    // DIAGNÓSTICO Y PRÁCTICA CON BARAJA DE OPCIONES (Ítem 9)
    // =========================================================================

    fun startDiagnostic(moduleId: String) {
        val seed = clock.wallClockMillis()
        val candidate = questionRepo.getQuestionsByModule(moduleId)
            .shuffled(Random(seed))
            .take(3)
            .map { QuestionShuffler.shuffleQuestion(it, seed) }

        val session = ActiveSessionState(
            attemptId = UUID.randomUUID().toString(),
            kind = AttemptKind.DIAGNOSTIC,
            moduleId = moduleId,
            questions = candidate,
            sessionSeed = seed
        )
        _uiState.update { it.copy(selectedModuleId = moduleId, activeSession = session) }
        navigateTo(Screen.PRACTICE_QUESTION)
    }

    fun startPractice(moduleId: String, topicId: String? = null) {
        val seed = clock.wallClockMillis()
        val candidate = if (topicId != null) {
            questionRepo.getQuestionsByTopic(moduleId, topicId)
        } else {
            questionRepo.getQuestionsByModule(moduleId)
        }

        val questions = if (candidate.isNotEmpty()) candidate else questionRepo.getQuestionsByModule(moduleId)
        val shuffled = questions.shuffled(Random(seed)).map { QuestionShuffler.shuffleQuestion(it, seed) }

        val session = ActiveSessionState(
            attemptId = UUID.randomUUID().toString(),
            kind = AttemptKind.PRACTICE,
            moduleId = moduleId,
            topicId = topicId,
            questions = shuffled,
            sessionSeed = seed
        )
        _uiState.update {
            it.copy(
                selectedModuleId = moduleId,
                selectedTopicId = topicId,
                activeSession = session
            )
        }
        navigateTo(Screen.PRACTICE_QUESTION)
    }

    // =========================================================================
    // SIMULACRO: BANCO INSUFICIENTE Y RELOJ ROBUSTO (Ítems 7 y 9)
    // =========================================================================

    fun startSimulation(moduleId: String, isTimed: Boolean, timeMinutes: Int) {
        val seed = clock.wallClockMillis()
        val candidateQuestions = questionRepo.getQuestionsByModule(moduleId)

        val guideModule = _uiState.value.guideModules.firstOrNull { it.id == moduleId }
        val configuredCount = guideModule?.preguntas_simulacro ?: 35
        val availableCount = candidateQuestions.size

        val isInsufficient = availableCount < configuredCount
        val targetCount = min(availableCount, configuredCount)

        val selected = candidateQuestions.shuffled(Random(seed)).take(targetCount)
        val shuffled = selected.map { QuestionShuffler.shuffleQuestion(it, seed) }

        val durationSeconds = if (isTimed) (timeMinutes * 60).toLong() else 0L
        val targetElapsed = clock.elapsedRealtimeMs() + (durationSeconds * 1000L)

        val session = ActiveSessionState(
            attemptId = UUID.randomUUID().toString(),
            kind = if (isTimed) AttemptKind.SIMULATION else AttemptKind.FREE_PRACTICE,
            moduleId = moduleId,
            questions = shuffled,
            totalTimeSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            targetElapsedRealtimeMs = targetElapsed,
            sessionSeed = seed,
            isBankInsufficient = isInsufficient,
            bankNotice = if (isInsufficient) "Banco insuficiente: $availableCount de $configuredCount preguntas disponibles [valor inicial a validar]" else null
        )

        _uiState.update {
            it.copy(
                selectedModuleId = moduleId,
                activeSession = session
            )
        }

        if (isTimed) {
            persistActiveSimulation(session)
            startSimulationTimer()
        }

        navigateTo(Screen.SIMULATION_IN_PROGRESS)
    }

    private fun persistActiveSimulation(session: ActiveSessionState) {
        val obj = JSONObject().apply {
            put("attemptId", session.attemptId)
            put("moduleId", session.moduleId)
            put("totalTimeSeconds", session.totalTimeSeconds)
            put("remainingSecondsAtSave", session.remainingSeconds)
            put("savedElapsedRealtimeMs", clock.elapsedRealtimeMs())
            put("savedWallTimeMs", clock.wallClockMillis())
            put("savedBootCount", clock.getBootCount())
            put("currentIndex", session.currentIndex)
            put("sessionSeed", session.sessionSeed)
            put("questionIds", JSONArray(session.questions.map { it.id_pregunta }))

            val ansObj = JSONObject()
            session.answers.forEach { (k, v) -> ansObj.put(k, v) }
            put("answers", ansObj)

            put("markedForReview", JSONArray(session.markedForReview))
        }
        userDataRepo.saveActiveSimulationRaw(obj.toString())
    }

    private fun startSimulationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(500L)
                val current = _uiState.value.activeSession ?: break
                if (current.kind != AttemptKind.SIMULATION) break

                val remaining = SimulationTimerLogic.calculateActiveRemaining(
                    targetElapsedRealtimeMs = current.targetElapsedRealtimeMs,
                    currentElapsedRealtimeMs = clock.elapsedRealtimeMs()
                )

                if (remaining <= 0L) {
                    _uiState.update { it.copy(activeSession = current.copy(remainingSeconds = 0L)) }
                    finishSession()
                    break
                } else {
                    val updated = current.copy(remainingSeconds = remaining)
                    _uiState.update { it.copy(activeSession = updated) }

                    // Persistir cada 5 segundos de forma determinista
                    if (remaining % 5L == 0L) {
                        persistActiveSimulation(updated)
                    }
                }
            }
        }
    }

    fun selectAnswerInSession(questionId: String, selectedOption: String) {
        val current = _uiState.value.activeSession ?: return
        val updatedAnswers = current.answers.toMutableMap()
        val updatedTimestamps = current.answerTimestamps.toMutableMap()

        updatedAnswers[questionId] = selectedOption
        updatedTimestamps[questionId] = clock.wallClockMillis() // Marca de tiempo real (Ítem 1)

        val updated = current.copy(
            answers = updatedAnswers,
            answerTimestamps = updatedTimestamps
        )
        _uiState.update { it.copy(activeSession = updated) }

        if (current.kind == AttemptKind.SIMULATION) {
            persistActiveSimulation(updated)
        }
    }

    fun toggleMarkForReview(questionId: String) {
        val current = _uiState.value.activeSession ?: return
        val marked = current.markedForReview.toMutableSet()
        if (marked.contains(questionId)) marked.remove(questionId) else marked.add(questionId)
        val updated = current.copy(markedForReview = marked)
        _uiState.update { it.copy(activeSession = updated) }
    }

    fun nextQuestion() {
        val current = _uiState.value.activeSession ?: return
        if (current.currentIndex < current.questions.size - 1) {
            _uiState.update { it.copy(activeSession = current.copy(currentIndex = current.currentIndex + 1)) }
        }
    }

    fun prevQuestion() {
        val current = _uiState.value.activeSession ?: return
        if (current.currentIndex > 0) {
            _uiState.update { it.copy(activeSession = current.copy(currentIndex = current.currentIndex - 1)) }
        }
    }

    fun jumpToQuestion(index: Int) {
        val current = _uiState.value.activeSession ?: return
        if (index in current.questions.indices) {
            _uiState.update { it.copy(activeSession = current.copy(currentIndex = index)) }
        }
    }

    fun checkAnswerForFeedback() {
        navigateTo(Screen.FEEDBACK)
    }

    fun finishSession() {
        timerJob?.cancel()
        userDataRepo.saveActiveSimulationRaw(null)

        val session = _uiState.value.activeSession ?: return
        var correctCount = 0
        val attemptAnswers = mutableListOf<AttemptAnswer>()
        val now = clock.wallClockMillis()

        for (q in session.questions) {
            val selected = session.answers[q.id_pregunta] ?: ""
            val isCorrect = selected == q.respuesta_correcta
            if (isCorrect) correctCount++

            val answeredAt = session.answerTimestamps[q.id_pregunta] ?: now
            val optionText = q.opciones[selected] ?: ""
            val originalKey = q.clave_original_por_mostrada[selected] ?: selected

            attemptAnswers.add(
                AttemptAnswer(
                    attemptId = session.attemptId,
                    questionId = q.id_pregunta,
                    selectedOption = selected,
                    selectedOriginalKey = originalKey,
                    selectedOptionText = optionText,
                    isCorrect = isCorrect,
                    timeSpentSeconds = 30L,
                    markedForReview = session.markedForReview.contains(q.id_pregunta),
                    answeredAt = answeredAt
                )
            )
        }

        val duration = if (session.totalTimeSeconds > 0) {
            (session.totalTimeSeconds - session.remainingSeconds).coerceAtLeast(1L)
        } else {
            30L * session.questions.size
        }

        val attempt = Attempt(
            id = session.attemptId,
            userId = _uiState.value.profile.id,
            kind = session.kind,
            moduleId = session.moduleId,
            startedAt = now - (duration * 1000L),
            finishedAt = now,
            totalQuestions = session.questions.size,
            correctAnswers = correctCount,
            durationSeconds = duration,
            syncStatus = SyncStatus.SOLO_LOCAL // Verídico: solo local (Ítem 10)
        )

        viewModelScope.launch {
            try {
                userDataRepo.saveAttempt(attempt, attemptAnswers)
            } catch (se: SecurityException) {
                _uiState.update { it.copy(errorMessage = se.message) }
            }

            _uiState.update {
                it.copy(
                    lastAttempt = attempt,
                    lastAttemptAnswers = attemptAnswers,
                    activeSession = null
                )
            }

            refreshDomainStats(_uiState.value.profile)
            navigateTo(Screen.ATTEMPT_RESULTS)
        }
    }

    private suspend fun refreshDomainStats(profile: UserProfile) {
        val questionsMap = _uiState.value.allQuestions.associateBy { it.id_pregunta }
        val masteries = userDataRepo.getTopicMasteries(questionsMap) { clock.wallClockMillis() }
        val recommendations = SessionRecommender.generateRecommendations(
            topicsMastery = masteries,
            enabledModuleIds = profile.enabledModules,
            clock = { clock.wallClockMillis() }
        )
        val history = userDataRepo.getAttempts()

        _uiState.update {
            it.copy(
                topicMasteries = masteries,
                recommendations = recommendations,
                attemptsHistory = history
            )
        }
    }

    // =========================================================================
    // ÍTEM 10: SINCRONIZACIÓN VERAZ (SOLO DEMO EN DEBUG)
    // =========================================================================

    fun toggleOrRetrySyncDemo() {
        viewModelScope.launch {
            _uiState.update { it.copy(syncStatus = SyncStatus.PENDIENTE) }
            delay(800L)
            _uiState.update {
                it.copy(
                    syncStatus = SyncStatus.SOLO_LOCAL,
                    feedbackMessage = "Simulación: Almacenado únicamente en este dispositivo (sin backend)."
                )
            }
        }
    }

    fun simulateSyncErrorDemo() {
        _uiState.update {
            it.copy(
                syncStatus = SyncStatus.ERROR_REINTENTO,
                feedbackMessage = "Simulación de desconexión en depuración."
            )
        }
    }

    // =========================================================================
    // COMUNICACIÓN ESCRITA Y REPORTES (Ítems 2 y 3)
    // =========================================================================

    fun saveEssay(title: String, content: String, thesis: Int, args: Int, struct: Int, lang: Int) {
        viewModelScope.launch {
            val words = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val updated = WrittenEssay(
                title = title,
                content = content,
                lastSavedAt = clock.wallClockMillis(),
                wordCount = words,
                selfScoreThesis = thesis,
                selfScoreArguments = args,
                selfScoreStructure = struct,
                selfScoreLanguage = lang
            )
            try {
                userDataRepo.saveWrittenEssay(updated)
                _uiState.update {
                    it.copy(
                        writtenEssay = updated,
                        feedbackMessage = "Redacción guardada localmente en Room."
                    )
                }
            } catch (se: SecurityException) {
                _uiState.update { it.copy(errorMessage = se.message) }
            }
        }
    }

    fun submitQuestionReport(questionId: String, module: String, reason: String, details: String) {
        viewModelScope.launch {
            val report = QuestionReport(
                questionId = questionId,
                module = module,
                reason = reason,
                details = details,
                timestamp = clock.wallClockMillis()
            )
            try {
                userDataRepo.reportQuestion(report)
                _uiState.update { it.copy(reportSubmittedMessage = "Reporte registrado para revisión académica.") }
            } catch (se: SecurityException) {
                _uiState.update { it.copy(errorMessage = se.message) }
            }
        }
    }

    fun clearReportMessage() {
        _uiState.update { it.copy(reportSubmittedMessage = null) }
    }

    suspend fun exportDataJson(): String {
        return userDataRepo.exportAllDataJson()
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            userDataRepo.clearAllUserData()
            loadInitialData()
            navigateTo(Screen.WELCOME)
        }
    }

    fun logout() {
        navigateTo(Screen.AUTH)
    }
}
