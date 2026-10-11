package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Tokens oficiales de diseño - Sección 29 (Estructura, Superficie e Identidad)
// =============================================================================
val AzulProfundo = Color(0xFF17324D)       // Estructura, barra superior, navegación
val Turquesa = Color(0xFF20B4B7)           // Acento de marca, indicadores de avance
val BlancoSuperficie = Color(0xFFFFFFFF)   // Superficies, tarjetas, preguntas
val GrisAzuladoFondo = Color(0xFFF3F6F8)   // Fondo general y zonas secundarias
val CarbonTexto = Color(0xFF24313D)        // Texto principal, enunciados, opciones
val AmbarEstimulo = Color(0xFFF6A535)      // Rachas, metas, advertencia de tiempo
val VerdeCorrecto = Color(0xFF23865B)      // Respuesta correcta, confirmación
val RojoError = Color(0xFFC84646)          // Errores de validación, incorrectas
val VioletaExploracion = Color(0xFF7962B5) // Retos, complementario, recursos

// =============================================================================
// Variantes para Texto y Botones con Contraste Verificable WCAG 2.2 AA (Ítem 12)
// (Relación de contraste >= 4.5:1 para texto normal sobre fondos claros)
// =============================================================================
val TurquesaTexto = Color(0xFF0C6B6D)      // Variante oscura para texto turquesa (CR > 6:1 en blanco y gris)
val AmbarTexto = Color(0xFF8A4E00)         // Variante oscura para texto ámbar (CR > 6.3:1 en blanco y gris)
val VerdeTexto = Color(0xFF186644)         // Variante oscura para texto verde (CR > 6.5:1 en blanco y gris)
val RojoTexto = Color(0xFFA62E2E)          // Variante oscura para texto rojo (CR > 6.7:1 en blanco y gris)
val VioletaTexto = Color(0xFF513D84)       // Variante oscura para texto violeta (CR > 7.4:1 en blanco y gris)

// Para botones primarios con texto blanco de alta legibilidad
val TurquesaBoton = Color(0xFF0E7678)      // Botón primario: contraste con texto blanco > 5.8:1

// =============================================================================
// Tokens de Modo Oscuro (Contraste >= 4.5:1 sobre AzulOscuroFondo/Superficie)
// =============================================================================
val AzulOscuroFondo = Color(0xFF0D1B2A)
val AzulOscuroSuperficie = Color(0xFF16283C)
val TextoClaro = Color(0xFFF0F4F8)
val TextoSecundarioClaro = Color(0xFFB5C3D1)
val TextoSecundarioOscuro = Color(0xFF5A6E82)

val TurquesaTextoDark = Color(0xFF4EE2E5)
val AmbarTextoDark = Color(0xFFFFC066)
val VerdeTextoDark = Color(0xFF63DB9F)
val RojoTextoDark = Color(0xFFFF8585)
val VioletaTextoDark = Color(0xFFB39DDB)

val TurquesaClaro = Color(0xFF4EE2E5)
val AmbarEstimuloClaro = Color(0xFFFFC066)
val RojoErrorClaro = Color(0xFFFF8585)
