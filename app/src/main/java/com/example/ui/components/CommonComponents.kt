package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SyncStatus
import com.example.ui.theme.AmbarEstimulo
import com.example.ui.theme.AzulProfundo
import com.example.ui.theme.BlancoSuperficie
import com.example.ui.theme.CarbonTexto
import com.example.ui.theme.RojoError
import com.example.ui.theme.Turquesa
import com.example.ui.theme.VerdeCorrecto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaberProTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    syncStatus: SyncStatus? = null,
    onSyncClick: () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BlancoSuperficie
                )
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = BlancoSuperficie
                    )
                }
            }
        },
        actions = {
            if (syncStatus != null) {
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier.testTag("sync_action_button")
                ) {
                    val icon = when (syncStatus) {
                        SyncStatus.SOLO_LOCAL -> Icons.Default.CloudDone
                        SyncStatus.PENDIENTE -> Icons.Default.CloudSync
                        SyncStatus.ERROR_REINTENTO -> Icons.Default.Error
                    }
                    val tint = when (syncStatus) {
                        SyncStatus.SOLO_LOCAL -> Turquesa
                        SyncStatus.PENDIENTE -> AmbarEstimulo
                        SyncStatus.ERROR_REINTENTO -> RojoError
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Estado: ${syncStatus.label}",
                        tint = tint
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AzulProfundo,
            titleContentColor = BlancoSuperficie,
            actionIconContentColor = BlancoSuperficie
        )
    )
}

/**
 * Banner informativo obligatorio según especificación:
 * Rotula explícitamente cualquier métrica interna para no confundir con puntajes oficiales del ICFES.
 */
@Composable
fun MetricNoticeBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("metric_notice_banner"),
        colors = CardDefaults.cardColors(
            containerColor = AzulProfundo.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Información pedagógica",
                tint = AzulProfundo,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Métrica interna de la app, no es un puntaje oficial del ICFES.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarbonTexto
                )
            )
        }
    }
}

/**
 * Indicador de estado que cumple la pauta de accesibilidad 29.5:
 * Nunca depende únicamente del color; incluye texto e icono.
 */
@Composable
fun StateBadge(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    textColor: Color = color
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = com.example.ui.theme.TurquesaBoton, // Variante con contraste >= 5.8:1 con texto blanco
            contentColor = BlancoSuperficie,
            disabledContainerColor = com.example.ui.theme.TurquesaBoton.copy(alpha = 0.4f),
            disabledContentColor = BlancoSuperficie.copy(alpha = 0.7f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "secondary_button"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AzulProfundo
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(AzulProfundo)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = AzulProfundo
            )
        )
    }
}

/**
 * Diálogo de confirmación para acciones destructivas o entregas irreversibles (28.5)
 */
@Composable
fun ConfirmationDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isDestructive) RojoError else AzulProfundo
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold)
            }
        },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) RojoError else Turquesa,
                    contentColor = BlancoSuperficie
                )
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = CarbonTexto)
            }
        }
    )
}

/**
 * Modal para reporte de errores en preguntas (Sección 28.5)
 */
@Composable
fun ReportQuestionDialog(
    show: Boolean,
    questionId: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String) -> Unit
) {
    if (!show) return

    var selectedReason by remember { mutableStateOf("Enunciado o redacción confusa") }
    var details by remember { mutableStateOf("") }

    val reasons = listOf(
        "Enunciado o redacción confusa",
        "Clave o respuesta correcta incorrecta",
        "Explicación técnica con error",
        "Opción duplicada o errónea"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reportar pregunta ($questionId)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Selecciona el motivo del reporte pedagógico:",
                    style = MaterialTheme.typography.bodySmall,
                    color = CarbonTexto
                )
                Spacer(modifier = Modifier.height(8.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Detalle adicional (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitReport(selectedReason, details)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Turquesa)
            ) {
                Text("Enviar reporte", color = BlancoSuperficie)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = CarbonTexto)
            }
        }
    )
}
