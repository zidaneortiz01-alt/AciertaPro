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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SaberProTopBar
import com.example.ui.components.SecondaryActionButton
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
 * 28.3.1. Bienvenida y carga
 */
@Composable
fun WelcomeScreen(viewModel: SaberProViewModel) {
    Scaffold(
        containerColor = GrisAzuladoFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logotipo e identidad visual institucional
            Card(
                colors = CardDefaults.cardColors(containerColor = AzulProfundo),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(96.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Logo Saber Pro",
                        tint = Turquesa,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Saber Pro",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo,
                    letterSpacing = 0.5.sp
                )
            )

            Text(
                text = "Tu preparación, tu futuro profesional",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Turquesa
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Entorno de preparación diagnóstica, práctica adaptativa y simulacros cronometrados para el examen de Estado Saber Pro.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CarbonTexto),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Enfoque inicial: Ingeniería de Sistemas · Universidad de La Guajira (Sede Maicao)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AzulProfundo
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryActionButton(
                text = "Comenzar Preparación",
                onClick = { viewModel.navigateTo(Screen.AUTH) },
                testTag = "welcome_start_btn"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryActionButton(
                text = "Configuración y Consentimiento",
                onClick = { viewModel.navigateTo(Screen.INITIAL_CONFIG) },
                testTag = "welcome_config_btn"
            )
        }
    }
}

/**
 * 28.3.2. Registro, inicio de sesión y recuperación (Ítem 2: Cuenta local PBKDF2)
 */
@Composable
fun AuthScreen(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Iniciar sesión, 1: Registro, 2: Recuperar
    var email by remember { mutableStateOf("") } // Campos inician vacíos (Ítem 2)
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Acceso a Saber Pro",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.WELCOME) }
            )
        },
        containerColor = GrisAzuladoFondo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rótulo explícito obligatorio (Ítem 2)
            Card(
                colors = CardDefaults.cardColors(containerColor = AzulProfundo.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AzulProfundo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autenticación segura con Google o cuenta local",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // INICIO DE SESIÓN CON CUENTA DE GOOGLE (RBAC: Estudiante vs Superusuario)
            // =========================================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Iniciar Sesión con Google",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accede directamente con tu cuenta de Google para sincronizar tu perfil y permisos según tu rol.",
                        style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.8f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Opción: Cuenta de Estudiante (Keila)
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithGoogle("keilawhp@gmail.com", "Keila - Estudiante") {
                                viewModel.navigateTo(Screen.DASHBOARD)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("google_login_student_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).background(VerdeCorrecto.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", fontWeight = FontWeight.Bold, color = VerdeCorrecto)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Keila (Estudiante)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AzulProfundo))
                                    Text("keilawhp@gmail.com", style = MaterialTheme.typography.labelSmall.copy(color = CarbonTexto))
                                }
                            }
                            Text("Estudiante", style = MaterialTheme.typography.labelSmall.copy(color = VerdeCorrecto, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opción: Cuenta de Superusuario / Docente (proyectoing2026)
                    OutlinedButton(
                        onClick = {
                            viewModel.loginWithGoogle("proyectoing2026@gmail.com", "Superusuario AciertaPro") {
                                viewModel.navigateTo(Screen.DASHBOARD)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("google_login_superuser_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).background(Turquesa.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", fontWeight = FontWeight.Bold, color = Turquesa)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Docente / Superusuario", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AzulProfundo))
                                    Text("proyectoing2026@gmail.com", style = MaterialTheme.typography.labelSmall.copy(color = CarbonTexto))
                                }
                            }
                            Text("Docente", style = MaterialTheme.typography.labelSmall.copy(color = Turquesa, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BlancoSuperficie,
                contentColor = AzulProfundo
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ingresar", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Registrar", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Recuperar", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Inicio de Sesión Local"
                            1 -> "Crear Cuenta en el Dispositivo"
                            else -> "Recuperación de Contraseña"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 1) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre completo del estudiante") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        singleLine = true
                    )

                    if (selectedTab != 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña (mínimo 8 caracteres)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Alternar visibilidad"
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }

                    // Mensaje honesto sin simulación falsa (Ítem 2)
                    if (selectedTab == 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "La recuperación por correo requiere un servidor y estará disponible en una fase posterior. Al no existir un backend centralizado, puedes optar por borrar la cuenta registrada localmente para crear una nueva.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = com.example.ui.theme.CarbonTexto,
                                lineHeight = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDeleteAccountConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = com.example.ui.theme.RojoTexto
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Borrar cuenta local de este dispositivo", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = com.example.ui.theme.RojoTexto,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (uiState.feedbackMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.feedbackMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = com.example.ui.theme.VerdeTexto,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    if (selectedTab != 2) {
                        Spacer(modifier = Modifier.height(24.dp))

                        PrimaryActionButton(
                            text = if (selectedTab == 0) "Ingresar a la Plataforma" else "Crear Cuenta y Continuar",
                            onClick = {
                                if (selectedTab == 0) {
                                    viewModel.loginLocalAccount(email, password) {
                                        viewModel.navigateTo(Screen.DASHBOARD)
                                    }
                                } else {
                                    viewModel.registerLocalAccount(email, password) {
                                        viewModel.navigateTo(Screen.INITIAL_CONFIG)
                                    }
                                }
                            },
                            testTag = "auth_action_btn"
                        )
                    }
                }
            }
        }

        ConfirmationDialog(
            show = showDeleteAccountConfirm,
            title = "Borrar cuenta local",
            message = "¿Deseas eliminar la cuenta registrada en este dispositivo? Esto te permitirá registrar una nueva contraseña.",
            confirmText = "Sí, borrar cuenta",
            isDestructive = true,
            onConfirm = {
                showDeleteAccountConfirm = false
                viewModel.deleteLocalAccount {
                    selectedTab = 1
                }
            },
            onDismiss = { showDeleteAccountConfirm = false }
        )
    }
}

/**
 * 28.3.3. Configuración inicial del estudiante y Consentimiento de Privacidad (Ley 1581)
 */
@Composable
fun InitialConfigScreen(viewModel: SaberProViewModel) {
    var program by remember { mutableStateOf("Ingeniería de Sistemas") }
    var campus by remember { mutableStateOf("Sede Maicao") }
    var targetDate by remember { mutableStateOf("Noviembre 2026") }
    var studyDays by remember { mutableIntStateOf(4) }

    // Módulos genéricos y refuerzo
    var modLc by remember { mutableStateOf(true) }
    var modRc by remember { mutableStateOf(true) }
    var modCc by remember { mutableStateOf(true) }
    var modIn by remember { mutableStateOf(true) }
    var modCe by remember { mutableStateOf(true) }
    var modIs by remember { mutableStateOf(true) } // Refuerzo complementario

    var consentAccepted by remember { mutableStateOf(false) }
    var consentError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SaberProTopBar(
                title = "Configuración y Consentimiento",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateTo(Screen.AUTH) }
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
            Text(
                text = "1. Perfil Académico",
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
                    OutlinedTextField(
                        value = program,
                        onValueChange = { program = it },
                        label = { Text("Programa Académico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = campus,
                        onValueChange = { campus = it },
                        label = { Text("Sede o Centro") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("Fecha Objetivo de Presentación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "2. Selección de Módulos a Preparar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulProfundo
                )
            )
            Text(
                text = "Los módulos oficiales corresponden a las guías ICFES. El refuerzo de carrera es complementario.",
                style = MaterialTheme.typography.bodySmall.copy(color = CarbonTexto.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BlancoSuperficie),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModuleToggleRow("Lectura Crítica (Oficial)", modLc) { modLc = it }
                    ModuleToggleRow("Razonamiento Cuantitativo (Oficial)", modRc) { modRc = it }
                    ModuleToggleRow("Competencias Ciudadanas (Oficial)", modCc) { modCc = it }
                    ModuleToggleRow("Inglés (Oficial)", modIn) { modIn = it }
                    ModuleToggleRow("Comunicación Escrita (Oficial)", modCe) { modCe = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ModuleToggleRow(
                        "Refuerzo: Ing. de Sistemas (Complementario)",
                        modIs,
                        isComplementary = true
                    ) { modIs = it }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Consentimiento Informado (Sección 13 / Ley 1581)
            Card(
                colors = CardDefaults.cardColors(containerColor = AzulProfundo.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacidad",
                            tint = AzulProfundo
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consentimiento Informado (Ley 1581 de 2012)",
                            fontWeight = FontWeight.Bold,
                            color = AzulProfundo
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "En cumplimiento de la legislación colombiana de protección de datos personales, le informamos que la aplicación almacena de forma estrictamente local sus registros de práctica, respuestas y tiempos únicamente con fines pedagógicos y de seguimiento personal. Sus datos no son compartidos con terceros con fines comerciales ni constituyen una evaluación oficial del ICFES. Puede consultar, exportar (JSON) o eliminar sus datos en cualquier momento desde su perfil.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = CarbonTexto
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = consentAccepted,
                            onCheckedChange = {
                                consentAccepted = it
                                if (it) consentError = false
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Turquesa),
                            modifier = Modifier.testTag("consent_checkbox")
                        )
                        Text(
                            text = "He leído y acepto el tratamiento responsable de mis datos para fines formativos.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = CarbonTexto
                            )
                        )
                    }

                    if (consentError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Debe aceptar el consentimiento informado antes de guardar información personal.",
                            style = MaterialTheme.typography.bodySmall.copy(color = RojoError, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryActionButton(
                text = "Guardar y Entrar al Panel",
                onClick = {
                    if (!consentAccepted) {
                        consentError = true
                    } else {
                        val enabledList = mutableListOf<String>()
                        if (modLc) enabledList.add("lectura_critica")
                        if (modRc) enabledList.add("razonamiento_cuantitativo")
                        if (modCc) enabledList.add("competencias_ciudadanas")
                        if (modIn) enabledList.add("ingles")
                        if (modCe) enabledList.add("comunicacion_escrita")
                        if (modIs) enabledList.add("ingenieria_sistemas")

                        viewModel.grantConsentAndSaveConfig(
                            program = program,
                            campus = campus,
                            targetDate = targetDate,
                            studyDays = studyDays,
                            enabledModules = enabledList
                        )
                    }
                },
                testTag = "config_save_btn"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModuleToggleRow(
    title: String,
    checked: Boolean,
    isComplementary: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isComplementary) AzulProfundo else CarbonTexto
                )
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = if (isComplementary) AzulProfundo else Turquesa
            )
        )
    }
}
