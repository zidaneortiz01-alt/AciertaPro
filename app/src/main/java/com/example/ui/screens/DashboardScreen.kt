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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MetricNoticeBanner
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SaberProTopBar
import com.example.ui.components.StateBadge
import com.example.ui.theme.AmbarEstimulo
import com.example.ui.theme.AzulProfundo
import com.example.ui.theme.BlancoSuperficie
import com.example.ui.theme.CarbonTexto
import com.example.ui.theme.GrisAzuladoFondo
import com.example.ui.theme.RojoError
import com.example.ui.theme.Turquesa
import com.example.ui.theme.VerdeCorrecto
import com.example.ui.theme.VioletaExploracion
import com.example.ui.viewmodel.SaberProViewModel
import com.example.ui.viewmodel.Screen

/**
 * 28.3.4. Inicio o panel principal (Dashboard del Estudiante)
 */
@Composable
fun DashboardScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val masteries = uiState.topicMasteries
    val avgMastery = if (masteries.isNotEmpty()) {
        masteries.map { it.masteryPercentage }.average().toInt()
    } else {
        0
    }
    val topRecommendation = uiState.recommendations.firstOrNull()

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Saber Pro · Maicao",
                syncStatus = uiState.syncStatus,
                onSyncClick = {
                    if (com.example.BuildConfig.DEBUG) {
                        viewModel.toggleOrRetrySyncDemo()
                    }
                },
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.navigateTo(Screen.PROFILE_SETTINGS) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Perfil y configuración",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Saludo personalizado y datos del programa
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val displayName = profile.name.trim().ifBlank { "Estudiante" }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hola, ${displayName.split(" ").firstOrNull() ?: displayName}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isSuper = profile.role == com.example.domain.model.UserRole.SUPERUSER || profile.role == com.example.domain.model.UserRole.TEACHER
                        Box(
                            modifier = Modifier
                                .background(if (isSuper) Turquesa.copy(alpha = 0.15f) else VerdeCorrecto.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = profile.role.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuper) Turquesa else VerdeCorrecto
                                )
                            )
                        }
                    }
                    Text(
                        text = "${profile.academicProgram} · ${profile.campus}",
                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.75f))
                    )
                }

                StateBadge(
                    label = uiState.syncStatus.label,
                    color = when (uiState.syncStatus) {
                        com.example.domain.model.SyncStatus.SOLO_LOCAL -> Turquesa
                        com.example.domain.model.SyncStatus.PENDIENTE -> AmbarEstimulo
                        com.example.domain.model.SyncStatus.ERROR_REINTENTO -> RojoError
                    },
                    icon = Icons.Default.CloudDone
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rotulado pedagógico obligatorio
            MetricNoticeBanner()

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta de Progreso General Interno y Meta
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso Global de Aprendizaje",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                        Text(
                            text = "$avgMastery%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Turquesa
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { avgMastery / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Turquesa,
                        trackColor = GrisAzuladoFondo
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Meta: ${profile.targetDate}",
                            style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                        )
                        Text(
                            text = "${profile.studyDaysPerWeek} días/sem · ${profile.sessionMinutes} min/día",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = AmbarEstimulo
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recomendación prioritaria explicable (Sección 23 / RF-07)
            if (topRecommendation != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AzulProfundo.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Recomendación explicable",
                                tint = AmbarEstimulo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Siguiente sesión recomendada",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AzulProfundo
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = topRecommendation.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CarbonTexto
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topRecommendation.reason,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CarbonTexto.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PrimaryActionButton(
                            text = "Iniciar Sesión Sugerida (${topRecommendation.estimatedMinutes} min)",
                            onClick = {
                                viewModel.startPractice(
                                    moduleId = topRecommendation.moduleId,
                                    topicId = topRecommendation.topicId
                                )
                            },
                            testTag = "start_recommended_session_btn"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Accesos Rápidos Principales
            Text(
                text = "Modos de Preparación",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardActionCard(
                    title = "Diagnóstico Inicial",
                    subtitle = "3 preguntas breves",
                    icon = Icons.Default.Assessment,
                    color = AzulProfundo,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.DIAGNOSTIC) }
                )
                DashboardActionCard(
                    title = "Simulacro",
                    subtitle = "Cronometrado",
                    icon = Icons.Default.Timer,
                    color = Turquesa,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.SIMULATION_CONFIG) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardActionCard(
                    title = "Módulos y Temas",
                    subtitle = "Explora y practica",
                    icon = Icons.Default.School,
                    color = VioletaExploracion,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.MODULES_LIST) }
                )
                DashboardActionCard(
                    title = "Com. Escrita",
                    subtitle = "Editor y rúbrica",
                    icon = Icons.Default.EditNote,
                    color = AmbarEstimulo,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.WRITTEN_COMMUNICATION) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardActionCard(
                    title = "Plan de Estudio",
                    subtitle = "Metas y Leitner",
                    icon = Icons.Default.Schedule,
                    color = AzulProfundo,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.STUDY_PLAN) }
                )
                DashboardActionCard(
                    title = "Historial y Progreso",
                    subtitle = "Intentos y evolución",
                    icon = Icons.Default.History,
                    color = VerdeCorrecto,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(Screen.PROGRESS_HISTORY) }
                )
            }

            // Herramientas de Superusuario / Docente (Visibles ÚNICAMENTE para Superusuarios)
            if (profile.role == com.example.domain.model.UserRole.SUPERUSER || profile.role == com.example.domain.model.UserRole.TEACHER) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Herramientas de Docente / Superusuario",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AzulProfundo
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Turquesa.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Turquesa,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Panel Administrativo ICFES Activo",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AzulProfundo
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sesión autorizada como Superusuario (${profile.email}). Acceso habilitado a la matriz ICFES 2026-2, auditoría de ítems y estadísticas de banco.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto, fontSize = 12.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("action_card_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CarbonTexto.copy(alpha = 0.7f),
                    fontSize = 11.5.sp
                ),
                maxLines = 1
            )
        }
    }
}
