package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.model.GuideModuleConfig
import com.example.domain.rules.MasteryCalculator
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.MetricNoticeBanner
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SaberProTopBar
import com.example.ui.components.StateBadge
import com.example.ui.theme.AmbarEstimulo
import com.example.ui.theme.AzulProfundo
import com.example.ui.theme.BlancoSuperficie
import com.example.ui.theme.CarbonTexto
import com.example.ui.theme.GrisAzuladoFondo
import com.example.ui.theme.Turquesa
import com.example.ui.theme.VerdeCorrecto
import com.example.ui.theme.VioletaExploracion
import com.example.ui.viewmodel.SaberProViewModel
import com.example.ui.viewmodel.Screen

/**
 * 28.3.5. Diagnóstico inicial
 */
@Composable
fun DiagnosticScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedModule by remember { mutableStateOf("lectura_critica") }
    var showExitConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Diagnóstico Inicial",
                canNavigateBack = true,
                onNavigateBack = { showExitConfirm = true },
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
        ) {
            MetricNoticeBanner()
            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = AzulProfundo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Propósito de la Evaluación Diagnóstica",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta prueba breve (3 preguntas de práctica de ejemplo por módulo) permite identificar tus fortalezas y aspectos a reforzar. El resultado es estrictamente orientativo y propio de la aplicación; no predice ni garantiza un puntaje en el examen oficial.",
                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Duración estimada: 5 a 8 minutos\n• Retroalimentación explicativa al finalizar\n• Puedes repetir el diagnóstico cuando lo desees",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = AzulProfundo
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selecciona el Módulo a Diagnosticar:",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.guideModules) { mod ->
                    val isSelected = selectedModule == mod.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedModule = mod.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Turquesa.copy(alpha = 0.12f) else BlancoSuperficie
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mod.nombre,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Turquesa else AzulProfundo
                                    )
                                )
                                Text(
                                    text = mod.descripcion,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = CarbonTexto.copy(alpha = 0.7f)
                                    ),
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seleccionado",
                                    tint = Turquesa
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryActionButton(
                text = "Iniciar Diagnóstico Breve",
                onClick = { viewModel.startDiagnostic(selectedModule) },
                testTag = "start_diagnostic_btn"
            )
        }

        ConfirmationDialog(
            show = showExitConfirm,
            title = "Salir del diagnóstico",
            message = "¿Deseas regresar al panel principal?",
            confirmText = "Sí, regresar",
            onConfirm = {
                showExitConfirm = false
                viewModel.navigateTo(Screen.DASHBOARD)
            },
            onDismiss = { showExitConfirm = false }
        )
    }
}

/**
 * 28.3.6. Lista de módulos (Distingue módulos oficiales vs refuerzo complementario)
 */
@Composable
fun ModulesListScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val officialModules = uiState.guideModules.filter { it.tipo == "oficial_generico" }
    val reinforcementModules = uiState.guideModules.filter { it.tipo == "refuerzo_carrera" }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Módulos de Preparación",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) },
                syncStatus = uiState.syncStatus
            )
        },
        containerColor = GrisAzuladoFondo
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MetricNoticeBanner()
            }

            item {
                Text(
                    text = "Módulos Genéricos Oficiales (ICFES)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AzulProfundo
                    )
                )
                Text(
                    text = "Contenidos evaluados en la prueba estandarizada oficial para todos los programas.",
                    style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.75f))
                )
            }

            items(officialModules) { mod ->
                ModuleDetailCard(
                    module = mod,
                    isOfficial = true,
                    onClick = {
                        if (mod.id == "comunicacion_escrita") {
                            viewModel.navigateTo(Screen.WRITTEN_COMMUNICATION)
                        } else {
                            viewModel.selectModule(mod.id)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Refuerzo Académico de Carrera (Ing. de Sistemas)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VioletaExploracion
                    )
                )
                Text(
                    text = "Refuerzo complementario del programa académico. No sustituye la oferta de módulos específicos oficiales hasta que sea confirmada por la institución.",
                    style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.75f))
                )
            }

            items(reinforcementModules) { mod ->
                ModuleDetailCard(
                    module = mod,
                    isOfficial = false,
                    onClick = { viewModel.selectModule(mod.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ModuleDetailCard(
    module: GuideModuleConfig,
    isOfficial: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("module_card_${module.id}"),
        colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOfficial) Turquesa.copy(alpha = 0.15f) else VioletaExploracion.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOfficial) Icons.Default.School else Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isOfficial) Turquesa else VioletaExploracion,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = module.nombre,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                }

                StateBadge(
                    label = if (isOfficial) "Oficial" else "Refuerzo",
                    color = if (isOfficial) Turquesa else VioletaExploracion,
                    icon = Icons.Default.Bookmark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = module.descripcion,
                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulacro: ${module.preguntas_simulacro} preg. · ${module.tiempo_minutos} min",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = CarbonTexto.copy(alpha = 0.7f)
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver temas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Turquesa
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Turquesa,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 28.3.7. Temas y competencias por módulo
 */
@Composable
fun TopicsScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val moduleId = uiState.selectedModuleId
    val currentModule = uiState.guideModules.firstOrNull { it.id == moduleId }
    val moduleQuestions = uiState.allQuestions.filter { it.modulo == moduleId }
    val topics = moduleQuestions.map { it.competencia_tema }.distinct()

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = currentModule?.nombre ?: "Temas y Competencias",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.MODULES_LIST) },
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
        ) {
            MetricNoticeBanner()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Competencias evaluadas",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Text(
                text = "Selecciona un tema para practicar con retroalimentación inmediata.",
                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.75f))
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (topics.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmbarEstimulo,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay preguntas cargadas aún para este módulo.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(topics) { topicId ->
                        val mastery = uiState.topicMasteries.firstOrNull { it.moduleId == moduleId && it.topicId == topicId }
                        val masteryPercent = mastery?.masteryPercentage ?: 0
                        val box = (mastery?.averageLeitnerBox ?: 1.0).let { if (it % 1.0 == 0.0) "${it.toInt()}" else "%.1f".format(java.util.Locale.US, it) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = topicId.replace("_", " ").split(" ")
                                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AzulProfundo
                                        )
                                    )
                                    StateBadge(
                                        label = "Caja Leitner $box",
                                        color = Turquesa,
                                        icon = Icons.Default.Layers
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Maestría interna (EMA):",
                                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                                    )
                                    Text(
                                        text = "$masteryPercent% · ${MasteryCalculator.getMasteryLevel(masteryPercent)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (masteryPercent >= 50) VerdeCorrecto else AmbarEstimulo
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { masteryPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (masteryPercent >= 50) VerdeCorrecto else Turquesa,
                                    trackColor = GrisAzuladoFondo
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                PrimaryActionButton(
                                    text = "Practicar este tema",
                                    onClick = { viewModel.startPractice(moduleId, topicId) },
                                    testTag = "practice_topic_${topicId}"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.startPractice(moduleId, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Practicar todo el módulo", color = AzulProfundo, fontWeight = FontWeight.Bold)
            }
        }
    }
}
