package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Prueba unitaria para verificación estricta de contraste WCAG 2.2 AA (Ítem 12).
 * Exige >= 4.5:1 para texto normal y >= 3.0:1 para texto grande.
 */
class ColorContrastTest {

    private fun channelToLinear(channel: Float): Double {
        return if (channel <= 0.04045f) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun calculateRelativeLuminance(color: Color): Double {
        val r = channelToLinear(color.red)
        val g = channelToLinear(color.green)
        val b = channelToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val l1 = calculateRelativeLuminance(color1)
        val l2 = calculateRelativeLuminance(color2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun checkAndPrintContrast(
        foregroundName: String,
        foreground: Color,
        backgroundName: String,
        background: Color,
        minRequired: Double = 4.5
    ): Double {
        val ratio = calculateContrastRatio(foreground, background)
        val formatted = String.format(java.util.Locale.US, "%.2f", ratio)
        val status = if (ratio >= minRequired) "CUMPLE (>= $minRequired:1)" else "FALLA (< $minRequired:1)"
        println("[CONTRASTE] $foregroundName sobre $backgroundName -> CR = $formatted:1 | $status")
        return ratio
    }

    @Test
    fun testOriginalTokens_failOnWhite_andCorrectedVariants_meetWcagAA() {
        println("=== ÍTEM 12: CONTRASTE WCAG 2.2 AA - TOKENS ORIGINALES Y VARIANTES CORREGIDAS ===")

        // Tokens originales evaluados sobre BlancoSuperficie
        val crTurquesaOrig = checkAndPrintContrast("Turquesa (Original #20B4B7)", Turquesa, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crAmbarOrig = checkAndPrintContrast("AmbarEstimulo (Original #F6A535)", AmbarEstimulo, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crVerdeOrig = checkAndPrintContrast("VerdeCorrecto (Original #23865B)", VerdeCorrecto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crRojoOrig = checkAndPrintContrast("RojoError (Original #C84646)", RojoError, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crVioletaOrig = checkAndPrintContrast("VioletaExploracion (Original #7962B5)", VioletaExploracion, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crBotonOrig = checkAndPrintContrast("BlancoSuperficie (#FFFFFF)", BlancoSuperficie, "Turquesa (Original #20B4B7 como fondo)", Turquesa)

        // Verificar tokens que fallan sobre blanco (< 4.5:1)
        assertTrue("Turquesa original sobre blanco no alcanza 4.5:1", crTurquesaOrig < 4.5)
        assertTrue("AmbarEstimulo original sobre blanco no alcanza 4.5:1", crAmbarOrig < 4.5)
        assertTrue("Texto blanco sobre Turquesa original no alcanza 4.5:1", crBotonOrig < 4.5)

        println("\n=== VARIANTES CORREGIDAS PARA TEXTO Y BOTÓN ===")
        // Texto principal
        val crCarbonOnWhite = checkAndPrintContrast("CarbonTexto (#24313D)", CarbonTexto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crCarbonOnGray = checkAndPrintContrast("CarbonTexto (#24313D)", CarbonTexto, "GrisAzuladoFondo (#F3F6F8)", GrisAzuladoFondo)
        assertTrue(crCarbonOnWhite >= 4.5)
        assertTrue(crCarbonOnGray >= 4.5)

        // Variantes de texto corregidas sobre Blanco y Gris
        val crTurquesaOnWhite = checkAndPrintContrast("TurquesaTexto (#0C6B6D)", TurquesaTexto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crTurquesaOnGray = checkAndPrintContrast("TurquesaTexto (#0C6B6D)", TurquesaTexto, "GrisAzuladoFondo (#F3F6F8)", GrisAzuladoFondo)
        assertTrue(crTurquesaOnWhite >= 4.5)
        assertTrue(crTurquesaOnGray >= 4.5)

        val crAmbarOnWhite = checkAndPrintContrast("AmbarTexto (#8A4E00)", AmbarTexto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crAmbarOnGray = checkAndPrintContrast("AmbarTexto (#8A4E00)", AmbarTexto, "GrisAzuladoFondo (#F3F6F8)", GrisAzuladoFondo)
        assertTrue(crAmbarOnWhite >= 4.5)
        assertTrue(crAmbarOnGray >= 4.5)

        val crVerdeOnWhite = checkAndPrintContrast("VerdeTexto (#186644)", VerdeTexto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crVerdeOnGray = checkAndPrintContrast("VerdeTexto (#186644)", VerdeTexto, "GrisAzuladoFondo (#F3F6F8)", GrisAzuladoFondo)
        assertTrue(crVerdeOnWhite >= 4.5)
        assertTrue(crVerdeOnGray >= 4.5)

        val crRojoOnWhite = checkAndPrintContrast("RojoTexto (#A62E2E)", RojoTexto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crRojoOnGray = checkAndPrintContrast("RojoTexto (#A62E2E)", RojoTexto, "GrisAzuladoFondo (#F3F6F8)", GrisAzuladoFondo)
        assertTrue(crRojoOnWhite >= 4.5)
        assertTrue(crRojoOnGray >= 4.5)

        val crVioletaOnWhite = checkAndPrintContrast("VioletaTexto (#513D84)", VioletaTexto, "BlancoSuperficie (#FFFFFF)", BlancoSuperficie)
        val crVioletaOnGray = checkAndPrintContrast("VioletaTexto (#513D84)", VioletaTexto, "GrisAzuladoFondo (#F3F6F8)", GrisAzuladoFondo)
        assertTrue(crVioletaOnWhite >= 4.5)
        assertTrue(crVioletaOnGray >= 4.5)

        // Botones primarios
        println("\n=== BOTONES PRIMARIOS CON TEXTO BLANCO ===")
        val crButton = checkAndPrintContrast("BlancoSuperficie (#FFFFFF)", BlancoSuperficie, "TurquesaBoton (#0E7678)", TurquesaBoton)
        val crAzulButton = checkAndPrintContrast("BlancoSuperficie (#FFFFFF)", BlancoSuperficie, "AzulProfundo (#17324D)", AzulProfundo)
        assertTrue(crButton >= 4.5)
        assertTrue(crAzulButton >= 4.5)

        // Modo Oscuro
        println("\n=== MODO OSCURO (SUPERFICIE OSCURA #16283C) ===")
        val crTextDark = checkAndPrintContrast("TextoClaro (#F0F4F8)", TextoClaro, "AzulOscuroSuperficie (#16283C)", AzulOscuroSuperficie)
        val crTurquesaDark = checkAndPrintContrast("TurquesaTextoDark (#4EE2E5)", TurquesaTextoDark, "AzulOscuroSuperficie (#16283C)", AzulOscuroSuperficie)
        val crAmbarDark = checkAndPrintContrast("AmbarTextoDark (#FFC066)", AmbarTextoDark, "AzulOscuroSuperficie (#16283C)", AzulOscuroSuperficie)
        val crVerdeDark = checkAndPrintContrast("VerdeTextoDark (#63DB9F)", VerdeTextoDark, "AzulOscuroSuperficie (#16283C)", AzulOscuroSuperficie)
        val crRojoDark = checkAndPrintContrast("RojoTextoDark (#FF8585)", RojoTextoDark, "AzulOscuroSuperficie (#16283C)", AzulOscuroSuperficie)
        assertTrue(crTextDark >= 4.5)
        assertTrue(crTurquesaDark >= 4.5)
        assertTrue(crAmbarDark >= 4.5)
        assertTrue(crVerdeDark >= 4.5)
        assertTrue(crRojoDark >= 4.5)
    }
}
