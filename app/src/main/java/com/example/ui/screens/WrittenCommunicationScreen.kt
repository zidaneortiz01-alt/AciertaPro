package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.ui.theme.Turquesa
import com.example.ui.theme.VerdeCorrecto
import com.example.ui.viewmodel.SaberProViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

/**
 * RF-10 y Sección 4.4 / 28.5: Comunicación Escrita
 * Editor de texto con contador de palabras y caracteres, guardado local y rúbrica de autoevaluación.
 * Sin IA (la IA queda para Fase 3 opcional), rotulada como orientativa.
 */
@Composable
fun WrittenCommunicationScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val essay = uiState.writtenEssay
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var essayContent by remember { mutableStateOf(essay.content) }
    var scoreThesis by remember { mutableIntStateOf(essay.selfScoreThesis) }
    var scoreArgs by remember { mutableIntStateOf(essay.selfScoreArguments) }
    var scoreStruct by remember { mutableIntStateOf(essay.selfScoreStructure) }
    var scoreLang by remember { mutableIntStateOf(essay.selfScoreLanguage) }

    val wordCount = if (essayContent.trim().isEmpty()) 0 else essayContent.trim().split("\\s+".toRegex()).size
    val charCount = essayContent.length

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SaberProTopBar(
                title = "Comunicación Escrita",
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

            // Consigna de Producción Escrita
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
                            text = "Consigna de Ensayo Argumentativo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                        StateBadge("Orientativo", Turquesa, Icons.Default.Edit)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "En los últimos años, la adopción masiva de herramientas de inteligencia artificial en la formación superior ha abierto un debate crucial sobre la autonomía del pensamiento crítico y la ética profesional.\n\nEscriba un texto argumentativo donde fije una postura clara sobre si la educación universitaria debe integrar o restringir el uso de estas tecnologías, sustentando su posición con al menos dos argumentos sólidos.",
                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto, lineHeight = 20.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Editor de Texto
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
                            text = "Área de Redacción",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AzulProfundo
                            )
                        )
                        Text(
                            text = "$wordCount palabras · $charCount caracteres",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (wordCount >= 200) VerdeCorrecto else AmbarEstimulo
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = essayContent,
                        onValueChange = { essayContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("written_essay_input"),
                        placeholder = { Text("Escribe aquí tu ensayo argumentativo...") },
                        maxLines = 15
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rúbrica de Autoevaluación Guiada (Sin IA, puramente pedagógica)
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rúbrica de Autoevaluación (Niveles 1 a 4)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Evalúa tu propio texto contra las dimensiones evaluativas orientativas de Saber Pro:",
                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.75f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    RubricSliderRow(
                        dimension = "1. Planteamiento de la Tesis",
                        description = "La postura es explícita, delimitada y coherente desde la introducción.",
                        value = scoreThesis,
                        onValueChange = { scoreThesis = it }
                    )

                    RubricSliderRow(
                        dimension = "2. Solidez de los Argumentos",
                        description = "Uso de razones de peso, evidencias y ejemplos pertinentes que respaldan la postura.",
                        value = scoreArgs,
                        onValueChange = { scoreArgs = it }
                    )

                    RubricSliderRow(
                        dimension = "3. Organización y Estructura Lógica",
                        description = "División clara en párrafos con progresión temática natural y conclusión sintética.",
                        value = scoreStruct,
                        onValueChange = { scoreStruct = it }
                    )

                    RubricSliderRow(
                        dimension = "4. Cohesión y Lenguaje",
                        description = "Uso apropiado de conectores lógicos, vocabulario formal y ortografía rigurosa.",
                        value = scoreLang,
                        onValueChange = { scoreLang = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryActionButton(
                text = "Guardar Redacción y Autoevaluación",
                onClick = {
                    viewModel.saveEssay(
                        title = essay.title,
                        content = essayContent,
                        thesis = scoreThesis,
                        args = scoreArgs,
                        struct = scoreStruct,
                        lang = scoreLang
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Ensayo guardado localmente en tu dispositivo.")
                    }
                },
                testTag = "save_essay_btn"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RubricSliderRow(
    dimension: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dimension,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Text(
                text = if (value == 0) "Sin calificar" else "Nivel $value / 4",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (value >= 3) VerdeCorrecto else Turquesa
                )
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = CarbonTexto.copy(alpha = 0.7f)
            )
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..4f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = Turquesa,
                activeTrackColor = Turquesa,
                inactiveTrackColor = GrisAzuladoFondo
            )
        )
    }
}
