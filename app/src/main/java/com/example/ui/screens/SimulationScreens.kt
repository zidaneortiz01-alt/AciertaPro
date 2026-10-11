package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttemptKind
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.MetricNoticeBanner
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.ReportQuestionDialog
import com.example.ui.components.SaberProTopBar
import com.example.ui.components.SecondaryActionButton
import com.example.ui.components.StateBadge
import com.example.ui.theme.AmbarEstimulo
import com.example.ui.theme.AzulProfundo
import com.example.ui.theme.BlancoSuperficie
import com.example.ui.theme.CarbonTexto
import com.example.ui.theme.GrisAzuladoFondo
import com.example.ui.theme.RojoError
import com.example.ui.theme.Turquesa
import com.example.ui.theme.VerdeCorrecto
import com.example.ui.viewmodel.SaberProViewModel
import com.example.ui.viewmodel.Screen

/**
 * 28.3.10. Configuración del simulacro
 */
@Composable
fun SimulationConfigScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedModuleId by remember { mutableStateOf("lectura_critica") }
    var isTimed by remember { mutableStateOf(true) }

    val currentModuleConfig = uiState.guideModules.firstOrNull { it.id == selectedModuleId }
    val defaultMinutes = currentModuleConfig?.tiempo_minutos ?: 60

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Configurar Simulacro",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) },
                syncStatus = uiState.syncStatus
            )
        },
        containerColor = GrisAzuladoFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            MetricNoticeBanner()
            Spacer(modifier = Modifier.height(14.dp))

            // Información de versión de la guía (no números harcodeados)
            Card(
                colors = CardDefaults.cardColors(containerColor = AzulProfundo.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Configuración basada en Guía ICFES",
                        fontWeight = FontWeight.Bold,
                        color = AzulProfundo
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.guideVersionInfo}\nLas cantidades de preguntas y tiempos se cargan desde la configuración versionada del sistema y deben validarse con la resolución institucional vigente.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = CarbonTexto)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ítem 9: Verificación de suficiencia del banco de preguntas
            val candidateQuestions = uiState.allQuestions.filter { it.modulo == selectedModuleId }
            val configuredQuestions = currentModuleConfig?.preguntas_simulacro ?: 35
            val isInsufficient = candidateQuestions.size < configuredQuestions

            if (isInsufficient) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmbarEstimulo.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = com.example.ui.theme.AmbarTexto,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Banco insuficiente: ${candidateQuestions.size} de $configuredQuestions preguntas disponibles [valor inicial a validar]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.AmbarTexto
                                )
                            )
                            Text(
                                text = "Se iniciará una muestra representativa con las preguntas disponibles. No constituye un simulacro completo oficial.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = CarbonTexto
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "1. Módulo del Simulacro",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    uiState.guideModules.filter { it.id != "comunicacion_escrita" }.forEach { mod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedModuleId = mod.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedModuleId == mod.id,
                                onClick = { selectedModuleId = mod.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mod.nombre,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulProfundo
                                )
                                Text(
                                    text = "Guía: ${mod.preguntas_simulacro} preg. · ${mod.tiempo_minutos} min [por confirmar]",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = CarbonTexto.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "2. Modalidad de Aplicación",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTimed = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isTimed, onClick = { isTimed = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Simulacro cronometrado oficial", fontWeight = FontWeight.Bold)
                            Text("Control estricto de tiempo ($defaultMinutes minutos)", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTimed = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isTimed, onClick = { isTimed = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Práctica libre sin límite de tiempo", fontWeight = FontWeight.Bold)
                            Text("Estudia a tu propio ritmo para reducir ansiedad", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryActionButton(
                text = if (isTimed) "Iniciar Simulacro Cronometrado" else "Iniciar Práctica Libre",
                onClick = {
                    viewModel.startSimulation(
                        moduleId = selectedModuleId,
                        isTimed = isTimed,
                        timeMinutes = defaultMinutes
                    )
                },
                testTag = "start_simulation_run_btn"
            )
        }
    }
}

/**
 * 28.3.11. Simulacro en curso (Sobrio, concentración, temporizador monótono persistente)
 */
@Composable
fun SimulationScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.activeSession
    var showConfirmDeliver by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    if (session == null || session.questions.isEmpty()) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("No hay simulacro activo.")
            }
        }
        return
    }

    val currentQ = session.questions[session.currentIndex]
    val selectedOption = session.answers[currentQ.id_pregunta]
    val isMarked = session.markedForReview.contains(currentQ.id_pregunta)

    val minutes = session.remainingSeconds / 60
    val seconds = session.remainingSeconds % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)
    val isLowTime = session.totalTimeSeconds > 0 && session.remainingSeconds < 300 // Menos de 5 min

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Simulacro · ${session.currentIndex + 1}/${session.questions.size}",
                canNavigateBack = true,
                onNavigateBack = { showConfirmDeliver = true },
                actions = {
                    if (session.totalTimeSeconds > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Temporizador",
                                tint = if (isLowTime) AmbarEstimulo else BlancoSuperficie,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timerString,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLowTime) AmbarEstimulo else BlancoSuperficie
                                ),
                                modifier = Modifier.testTag("simulation_timer_text")
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.toggleMarkForReview(currentQ.id_pregunta) }) {
                        Icon(
                            imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Marcar para revisión",
                            tint = if (isMarked) AmbarEstimulo else BlancoSuperficie
                        )
                    }

                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Reportar pregunta",
                            tint = BlancoSuperficie
                        )
                    }
                }
            )
        },
        containerColor = GrisAzuladoFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Aviso de reinicio del dispositivo si aplica (Ítem 7)
            if (session.resumeNotice != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmbarEstimulo.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = com.example.ui.theme.AmbarTexto,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = session.resumeNotice,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.AmbarTexto
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Aviso de banco insuficiente si aplica (Ítem 9)
            if (session.isBankInsufficient && session.bankNotice != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AzulProfundo.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AzulProfundo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${session.bankNotice} (Muestra parcial, no es simulacro completo)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = AzulProfundo
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Matriz rápida de navegación de preguntas
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Mapa de Preguntas (${session.answers.size}/${session.questions.size} respondidas):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AzulProfundo)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        session.questions.forEachIndexed { index, q ->
                            val isCurrent = index == session.currentIndex
                            val hasAnswer = session.answers.containsKey(q.id_pregunta)
                            val isMarkedQ = session.markedForReview.contains(q.id_pregunta)

                            val bg = when {
                                isCurrent -> Turquesa
                                isMarkedQ -> AmbarEstimulo
                                hasAnswer -> AzulProfundo.copy(alpha = 0.25f)
                                else -> GrisAzuladoFondo
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .clickable { viewModel.jumpToQuestion(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent || isMarkedQ) BlancoSuperficie else CarbonTexto
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta de la Pregunta
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pregunta ${session.currentIndex + 1} · ${currentQ.id_pregunta}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                        if (isMarked) {
                            StateBadge("Pendiente revisión", AmbarEstimulo, Icons.Default.Bookmark)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentQ.enunciado,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CarbonTexto,
                            lineHeight = 24.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opciones de respuesta
            currentQ.opciones.forEach { (key, text) ->
                val isSelected = selectedOption == key
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Turquesa else Color(0xFFD2DCE6),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.selectAnswerInSession(currentQ.id_pregunta, key) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Turquesa.copy(alpha = 0.08f) else BlancoSuperficie
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Turquesa else GrisAzuladoFondo),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BlancoSuperficie else CarbonTexto
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(color = CarbonTexto),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Controles de navegación y entrega
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.prevQuestion() },
                    enabled = session.currentIndex > 0,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Anterior")
                }

                if (session.currentIndex < session.questions.size - 1) {
                    OutlinedButton(
                        onClick = { viewModel.nextQuestion() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Siguiente")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showConfirmDeliver = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Finalizar e Entregar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryActionButton(
                text = "Entregar y Calificar Simulacro",
                onClick = { showConfirmDeliver = true },
                testTag = "deliver_simulation_btn"
            )
        }

        ConfirmationDialog(
            show = showConfirmDeliver,
            title = "Confirmar entrega del simulacro",
            message = "Has respondido ${session.answers.size} de ${session.questions.size} preguntas. ¿Estás seguro de que deseas entregar y finalizar la sesión?",
            confirmText = "Sí, entregar ahora",
            onConfirm = {
                showConfirmDeliver = false
                viewModel.finishSession()
            },
            onDismiss = { showConfirmDeliver = false }
        )

        ReportQuestionDialog(
            show = showReportDialog,
            questionId = currentQ.id_pregunta,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason, details ->
                viewModel.submitQuestionReport(
                    questionId = currentQ.id_pregunta,
                    module = currentQ.modulo,
                    reason = reason,
                    details = details
                )
            }
        )
    }
}

/**
 * 28.3.12. Resultados del intento (Métricas internas, rotuladas explícitamente)
 */
@Composable
fun AttemptResultsScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val attempt = uiState.lastAttempt
    val answers = uiState.lastAttemptAnswers

    if (attempt == null) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("No hay resultados de intento disponibles.")
            }
        }
        return
    }

    val percentage = if (attempt.totalQuestions > 0) {
        (attempt.correctAnswers * 100) / attempt.totalQuestions
    } else {
        0
    }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Resultados del Intento",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) },
                syncStatus = uiState.syncStatus
            )
        },
        containerColor = GrisAzuladoFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rótulo destacado e ineludible (Sección 3 y 28.1)
            MetricNoticeBanner()

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta de Desempeño
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Porcentaje de Aciertos Internos",
                        style = MaterialTheme.typography.titleSmall.copy(color = CarbonTexto)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (percentage >= 60) VerdeCorrecto else Turquesa
                        )
                    )
                    Text(
                        text = "${attempt.correctAnswers} de ${attempt.totalQuestions} preguntas correctas",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Modalidad", style = MaterialTheme.typography.labelSmall, color = CarbonTexto.copy(alpha = 0.7f))
                            Text(attempt.kind.label, fontWeight = FontWeight.Bold, color = AzulProfundo)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tiempo", style = MaterialTheme.typography.labelSmall, color = CarbonTexto.copy(alpha = 0.7f))
                            Text("${attempt.durationSeconds / 60}m ${attempt.durationSeconds % 60}s", fontWeight = FontWeight.Bold, color = AzulProfundo)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sincronización", style = MaterialTheme.typography.labelSmall, color = CarbonTexto.copy(alpha = 0.7f))
                            Text(attempt.syncStatus.label, fontWeight = FontWeight.Bold, color = Turquesa)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Desglose de preguntas respondidas
            Text(
                text = "Detalle de Preguntas",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            answers.forEach { ans ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (ans.isCorrect) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = if (ans.isCorrect) "Correcta" else "Incorrecta",
                                tint = if (ans.isCorrect) VerdeCorrecto else RojoError
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = ans.questionId,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulProfundo
                                )
                                Text(
                                    text = "Elegida: ${ans.selectedOption.ifEmpty { "Sin responder" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                                )
                            }
                        }

                        if (ans.markedForReview) {
                            StateBadge("Revisar", AmbarEstimulo, Icons.Default.Bookmark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryActionButton(
                text = "Volver al Panel Principal",
                onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                testTag = "results_to_dashboard_btn"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SecondaryActionButton(
                text = "Ver Plan de Estudio y Recomendaciones",
                onClick = { viewModel.navigateTo(Screen.STUDY_PLAN) },
                testTag = "results_to_study_plan_btn"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
