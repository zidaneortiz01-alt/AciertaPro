package com.example.domain.rules

import com.example.domain.model.Question
import java.util.Random

/**
 * Baraja de opciones para neutralizar sesgos en las claves correctas (Ítem 9).
 * Permite presentar opciones balanceadas (A, B, C, D) mapeando la clave correcta
 * sin modificar el archivo estático questions.json.
 */
object QuestionShuffler {
    private val OPTION_KEYS = listOf("A", "B", "C", "D")

    fun shuffleQuestion(question: Question, sessionSeed: Long): Question {
        if (question.opciones.size <= 1) return question

        val entries = question.opciones.entries.toList()
        val random = Random(sessionSeed xor question.id_pregunta.hashCode().toLong())
        val shuffledEntries = entries.shuffled(random)

        val newOptions = mutableMapOf<String, String>()
        val displayedToOriginal = mutableMapOf<String, String>()
        var newCorrectKey = question.respuesta_correcta

        shuffledEntries.forEachIndexed { index, entry ->
            val assignedKey = OPTION_KEYS.getOrElse(index) { ('A' + index).toString() }
            newOptions[assignedKey] = entry.value
            displayedToOriginal[assignedKey] = entry.key
            if (entry.key == question.respuesta_correcta) {
                newCorrectKey = assignedKey
            }
        }

        return question.copy(
            opciones = newOptions,
            respuesta_correcta = newCorrectKey,
            clave_original_por_mostrada = displayedToOriginal
        )
    }
}
