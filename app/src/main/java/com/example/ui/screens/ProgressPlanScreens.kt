package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SyncStatus
import com.example.domain.rules.MasteryCalculator
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.MetricNoticeBanner
import com.example.ui.components.PrimaryActionButton
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 28.3.13. Historial y progreso (Evolución temporal, % aciertos internos, tiempo promedio)
 */
@Composable
fun ProgressScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val attempts = uiState.attemptsHistory
    val masteries = uiState.topicMasteries

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Historial y Progreso",
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

            // Resumen de Métricas Globales
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Indicadores de Aprendizaje Interno",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = AzulProfundo)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sesiones", style = MaterialTheme.typography.labelSmall, color = CarbonTexto.copy(alpha = 0.7f))
                            Text("${attempts.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AzulProfundo)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val avgScore = if (attempts.isNotEmpty()) {
                                attempts.map { if (it.totalQuestions > 0) (it.correctAnswers * 100) / it.totalQuestions else 0 }.average().toInt()
                            } else 0
                            Text("Promedio de Acierto", style = MaterialTheme.typography.labelSmall, color = CarbonTexto.copy(alpha = 0.7f))
                            Text("$avgScore%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Turquesa)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalTime = attempts.sumOf { it.durationSeconds }
                            val avgTime = if (attempts.isNotEmpty()) totalTime / attempts.size else 0
                            Text("Tiempo Medio", style = MaterialTheme.typography.labelSmall, color = CarbonTexto.copy(alpha = 0.7f))
                            Text("${avgTime / 60} min", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AmbarEstimulo)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Desglose de Maestría por Tema (EMA)
            Text(
                text = "Maestría por Competencia (Regla EMA)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (masteries.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aún no has completado prácticas para generar la curva de maestría exponencial.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                masteries.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.topicId.replace("_", " "),
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulProfundo
                                )
                                Text(
                                    text = "${item.masteryPercentage}% (${MasteryCalculator.getMasteryLevel(item.masteryPercentage)})",
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.masteryPercentage >= 50) VerdeCorrecto else AmbarEstimulo
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { item.masteryPercentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (item.masteryPercentage >= 50) VerdeCorrecto else Turquesa,
                                trackColor = GrisAzuladoFondo
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Historial de Sesiones Anteriores
            Text(
                text = "Historial de Intentos",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (attempts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No se registran intentos previos en el almacenamiento local.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                attempts.forEach { att ->
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(att.startedAt))
                    val pct = if (att.totalQuestions > 0) (att.correctAnswers * 100) / att.totalQuestions else 0

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
                            Column {
                                Text(
                                    text = att.kind.label,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulProfundo
                                )
                                Text(
                                    text = "$dateStr · ${att.durationSeconds / 60} min",
                                    style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.7f))
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$pct%",
                                    fontWeight = FontWeight.Bold,
                                    color = if (pct >= 60) VerdeCorrecto else AmbarEstimulo
                                )
                                Text(
                                    text = "${att.correctAnswers}/${att.totalQuestions} aciertos",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 28.3.14. Plan de estudio (Metas, Leitner y recomendaciones explicables)
 */
@Composable
fun StudyPlanScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val recommendations = uiState.recommendations

    var targetDate by remember { mutableStateOf(profile.targetDate) }
    var studyDays by remember { mutableIntStateOf(profile.studyDaysPerWeek) }
    var minutesPerDay by remember { mutableIntStateOf(profile.sessionMinutes) }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Plan de Estudio",
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

            // Configuración de Metas Locales
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Metas de Preparación",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("Fecha objetivo del examen") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Días de estudio a la semana: $studyDays",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Row {
                            TextButton(onClick = { if (studyDays > 1) studyDays-- }) { Text("-", fontSize = 20.sp) }
                            TextButton(onClick = { if (studyDays < 7) studyDays++ }) { Text("+", fontSize = 20.sp) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duración por sesión: $minutesPerDay min",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Row {
                            TextButton(onClick = { if (minutesPerDay > 15) minutesPerDay -= 15 }) { Text("-", fontSize = 20.sp) }
                            TextButton(onClick = { if (minutesPerDay < 120) minutesPerDay += 15 }) { Text("+", fontSize = 20.sp) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recomendaciones Explicables del Motor de Reglas (Sección 23)
            Text(
                text = "Sesiones Sugeridas (Motor de Reglas)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Text(
                text = "Cada recomendación fundamenta su razón pedagógica de forma transparente.",
                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.75f))
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (recommendations.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Realiza tu diagnóstico inicial para generar recomendaciones automáticas.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                recommendations.forEach { rec ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rec.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AzulProfundo
                                    )
                                )
                                StateBadge(
                                    label = "${rec.estimatedMinutes} min",
                                    color = AmbarEstimulo,
                                    icon = Icons.Default.Schedule
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rec.reason,
                                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto, fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PrimaryActionButton(
                                text = "Iniciar esta práctica",
                                onClick = { viewModel.startPractice(rec.moduleId, rec.topicId) },
                                testTag = "start_plan_rec_${rec.topicId}"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 28.3.15. Perfil, privacidad y ajustes (Ley 1581 / RF-11: exportar y eliminar)
 */
@Composable
fun ProfileScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var exportedJsonDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SaberProTopBar(
                title = "Perfil y Ajustes",
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
            // Datos del Estudiante
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AzulProfundo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BlancoSuperficie,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AzulProfundo)
                            )
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.7f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Programa: ${profile.academicProgram} (${profile.campus})",
                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto)
                    )
                    Text(
                        text = "Consentimiento: Aceptado el ${profile.consentDate.ifEmpty { "Fecha de registro" }} (${profile.consentVersion})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = VerdeCorrecto)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preferencias de Interfaz
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferencias Visuales",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = AzulProfundo)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isDarkMode) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                                contentDescription = null,
                                tint = AzulProfundo
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Modo Oscuro")
                        }
                        Switch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Turquesa)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulación de Sincronización Offline-first (RF-09 / 28.5)
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Estado de Sincronización y Red",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = AzulProfundo)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cola de sincronización:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        StateBadge(
                            label = uiState.syncStatus.label,
                            color = when (uiState.syncStatus) {
                                SyncStatus.SOLO_LOCAL -> Turquesa
                                SyncStatus.PENDIENTE -> AmbarEstimulo
                                SyncStatus.ERROR_REINTENTO -> RojoError
                            },
                            icon = Icons.Default.Sync
                        )
                    }

                    // Ítem 10: Botones de simulación solo visibles en DEBUG
                    if (com.example.BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "[Modo Depuración]: Simulación de cola",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = com.example.ui.theme.CarbonTexto.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.toggleOrRetrySyncDemo() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Forzar Sync", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.simulateSyncErrorDemo() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Simular Error", fontSize = 12.sp, color = RojoError)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gestión de Datos Personales (RF-11 / Ley 1581)
            Text(
                text = "Protección de Datos Personales",
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
                    SecondaryActionButton(
                        text = "Exportar mis datos (JSON)",
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.exportDataJson()
                                exportedJsonDialog = json
                            }
                        },
                        testTag = "export_data_btn"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_data_btn"),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = RojoError
                        ),
                        border = androidx.compose.material3.ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(RojoError)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = RojoError)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar mis datos (Derecho de supresión)", color = RojoError, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cerrar Sesión
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = AzulProfundo)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cerrar Sesión", color = AzulProfundo, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Diálogo de Exportación de Datos en JSON
        if (exportedJsonDialog != null) {
            AlertDialog(
                onDismissRequest = { exportedJsonDialog = null },
                title = { Text("Exportación de Datos (JSON)") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "A continuación se muestra el archivo JSON con la exportación completa de los datos registrados localmente conforme a la Ley 1581 de 2012:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(GrisAzuladoFondo, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = exportedJsonDialog!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CarbonTexto
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Saber Pro Data Export", exportedJsonDialog))
                        coroutineScope.launch { snackbarHostState.showSnackbar("Datos copiados al portapapeles") }
                        exportedJsonDialog = null
                    }) {
                        Text("Copiar al Portapapeles")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exportedJsonDialog = null }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        ConfirmationDialog(
            show = showDeleteConfirm,
            title = "Eliminar todos mis datos",
            message = "Esta acción eliminará de forma irreversible tu perfil, consentimientos y todo el historial de prácticas y simulacros almacenado en este dispositivo. ¿Confirmas la eliminación definitiva?",
            confirmText = "Sí, eliminar definitivamente",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteAllUserData()
            },
            onDismiss = { showDeleteConfirm = false }
        )

        ConfirmationDialog(
            show = showLogoutConfirm,
            title = "Cerrar sesión",
            message = "¿Deseas cerrar tu sesión? Tus datos locales se mantendrán seguros en el dispositivo.",
            confirmText = "Sí, salir",
            onConfirm = {
                showLogoutConfirm = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}
