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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
 * 28.3.8. Pregunta de práctica
 */
@Composable
fun PracticeQuestionScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.activeSession
    val snackbarHostState = remember { SnackbarHostState() }

    var showReportDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.reportSubmittedMessage) {
        uiState.reportSubmittedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearReportMessage()
        }
    }

    if (session == null || session.questions.isEmpty()) {
        Scaffold(
            topBar = {
                SaberProTopBar(
                    title = "Práctica",
                    canNavigateBack = true,
                    onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay preguntas activas en la sesión actual.")
            }
        }
        return
    }

    val currentQuestion = session.questions[session.currentIndex]
    val selectedOption = session.answers[currentQuestion.id_pregunta]
    val isMarked = session.markedForReview.contains(currentQuestion.id_pregunta)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SaberProTopBar(
                title = "${session.kind.label} (${session.currentIndex + 1}/${session.questions.size})",
                canNavigateBack = true,
                onNavigateBack = { showExitDialog = true },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleMarkForReview(currentQuestion.id_pregunta) },
                        modifier = Modifier.testTag("toggle_mark_review_btn")
                    ) {
                        Icon(
                            imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isMarked) "Marcada para revisión" else "Marcar para revisión",
                            tint = if (isMarked) AmbarEstimulo else BlancoSuperficie
                        )
                    }
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.testTag("report_question_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Reportar posible error en pregunta",
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
            // Progreso de preguntas de la sesión
            LinearProgressIndicator(
                progress = { (session.currentIndex + 1).toFloat() / session.questions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Turquesa,
                trackColor = AzulProfundo.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Rótulo obligatorio de origen de pregunta de ejemplo y tema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StateBadge(
                    label = "Pregunta de práctica de ejemplo",
                    color = AzulProfundo,
                    icon = Icons.Default.Info
                )

                if (isMarked) {
                    StateBadge(
                        label = "Pendiente",
                        color = AmbarEstimulo,
                        icon = Icons.Default.Bookmark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tarjeta del Enunciado (Superficie blanca, texto carbón según 29.2)
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Pregunta ${session.currentIndex + 1} de ${session.questions.size} · ${currentQuestion.id_pregunta}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentQuestion.enunciado,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CarbonTexto,
                            lineHeight = 24.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opciones de respuesta
            Text(
                text = "Selecciona una opción:",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            currentQuestion.opciones.forEach { (key, text) ->
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
                        .clickable {
                            viewModel.selectAnswerInSession(currentQuestion.id_pregunta, key)
                        }
                        .testTag("option_${key}"),
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
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CarbonTexto,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Acciones de navegación y verificación
            if (session.kind == AttemptKind.PRACTICE) {
                PrimaryActionButton(
                    text = "Verificar y ver explicación",
                    onClick = { viewModel.checkAnswerForFeedback() },
                    enabled = selectedOption != null,
                    testTag = "verify_answer_btn"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                        onClick = { viewModel.finishSession() },
                        enabled = selectedOption != null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Finalizar sesión")
                    }
                }
            }
        }

        ReportQuestionDialog(
            show = showReportDialog,
            questionId = currentQuestion.id_pregunta,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason, details ->
                viewModel.submitQuestionReport(
                    questionId = currentQuestion.id_pregunta,
                    module = currentQuestion.modulo,
                    reason = reason,
                    details = details
                )
            }
        )

        ConfirmationDialog(
            show = showExitDialog,
            title = "Salir de la práctica",
            message = "¿Deseas pausar esta sesión? Tu progreso actual se guardará en el dispositivo.",
            confirmText = "Sí, guardar y salir",
            onConfirm = {
                showExitDialog = false
                viewModel.navigateTo(Screen.DASHBOARD)
            },
            onDismiss = { showExitDialog = false }
        )
    }
}

/**
 * 28.3.9. Retroalimentación (Sección 29.3 - 29.5: Icono + Texto, nunca solo color)
 */
@Composable
fun FeedbackScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.activeSession
    val currentQuestion = session?.questions?.getOrNull(session.currentIndex)

    if (session == null || currentQuestion == null) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("No hay sesión activa.")
            }
        }
        return
    }

    val selectedOption = session.answers[currentQuestion.id_pregunta]
    val isCorrect = selectedOption == currentQuestion.respuesta_correcta
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Retroalimentación Explicada",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.PRACTICE_QUESTION) }
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
            // Banner de resultado con Texto e Icono (Regla 29.5: no depender sólo del color)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) VerdeCorrecto.copy(alpha = 0.12f) else RojoError.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = if (isCorrect) "Correcta" else "Incorrecta",
                        tint = if (isCorrect) VerdeCorrecto else RojoError,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isCorrect) "Respuesta Correcta" else "Respuesta Incorrecta",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) VerdeCorrecto else RojoError
                            )
                        )
                        Text(
                            text = if (isCorrect)
                                "Has seleccionado la clave esperada: Opción ${currentQuestion.respuesta_correcta}"
                            else
                                "Tu elección: Opción ${selectedOption ?: "Ninguna"} · Clave esperada: Opción ${currentQuestion.respuesta_correcta}",
                            style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explicación paso a paso revisada y razonamiento
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmbarEstimulo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Justificación y Análisis Pedagógico",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentQuestion.explicacion,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CarbonTexto,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resumen de opciones
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Opciones evaluadas:",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    currentQuestion.opciones.forEach { (k, v) ->
                        val isTarget = k == currentQuestion.respuesta_correcta
                        val wasSelected = k == selectedOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opción $k: ",
                                fontWeight = FontWeight.Bold,
                                color = if (isTarget) VerdeCorrecto else if (wasSelected) RojoError else CarbonTexto
                            )
                            Text(
                                text = v,
                                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Acciones principales
            if (session.currentIndex < session.questions.size - 1) {
                PrimaryActionButton(
                    text = "Continuar a la siguiente pregunta",
                    onClick = {
                        viewModel.nextQuestion()
                        viewModel.navigateTo(Screen.PRACTICE_QUESTION)
                    },
                    testTag = "feedback_next_btn"
                )
            } else {
                PrimaryActionButton(
                    text = "Finalizar y ver resultados de la sesión",
                    onClick = { viewModel.finishSession() },
                    testTag = "feedback_finish_btn"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SecondaryActionButton(
                text = "Reportar posible error en esta explicación",
                onClick = { showReportDialog = true },
                testTag = "feedback_report_btn"
            )
        }

        ReportQuestionDialog(
            show = showReportDialog,
            questionId = currentQuestion.id_pregunta,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason, details ->
                viewModel.submitQuestionReport(
                    questionId = currentQuestion.id_pregunta,
                    module = currentQuestion.modulo,
                    reason = reason,
                    details = details
                )
            }
        )
    }
}
