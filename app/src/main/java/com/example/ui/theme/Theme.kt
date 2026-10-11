package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AzulProfundo,
    onPrimary = BlancoSuperficie,
    primaryContainer = Turquesa.copy(alpha = 0.15f),
    onPrimaryContainer = AzulProfundo,
    secondary = Turquesa,
    onSecondary = BlancoSuperficie,
    secondaryContainer = Turquesa.copy(alpha = 0.2f),
    onSecondaryContainer = AzulProfundo,
    tertiary = AmbarEstimulo,
    onTertiary = CarbonTexto,
    background = GrisAzuladoFondo,
    onBackground = CarbonTexto,
    surface = BlancoSuperficie,
    onSurface = CarbonTexto,
    surfaceVariant = GrisAzuladoFondo,
    onSurfaceVariant = CarbonTexto.copy(alpha = 0.8f),
    error = RojoError,
    onError = BlancoSuperficie,
    outline = Color(0xFFD2DCE6),
    outlineVariant = Color(0xFFE2E9F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = TurquesaClaro,
    onPrimary = AzulOscuroFondo,
    primaryContainer = AzulOscuroSuperficie,
    onPrimaryContainer = TextoClaro,
    secondary = Turquesa,
    onSecondary = AzulOscuroFondo,
    secondaryContainer = AzulProfundo,
    onSecondaryContainer = TextoClaro,
    tertiary = AmbarEstimuloClaro,
    onTertiary = AzulOscuroFondo,
    background = AzulOscuroFondo,
    onBackground = TextoClaro,
    surface = AzulOscuroSuperficie,
    onSurface = TextoClaro,
    surfaceVariant = AzulProfundo,
    onSurfaceVariant = TextoSecundarioClaro,
    error = RojoErrorClaro,
    onError = AzulOscuroFondo,
    outline = Color(0xFF2E4662),
    outlineVariant = Color(0xFF22354A)
)

@Composable
fun SaberProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
