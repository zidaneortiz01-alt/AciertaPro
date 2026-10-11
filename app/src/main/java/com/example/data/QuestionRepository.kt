package com.example.data

import android.content.Context
import com.example.domain.model.FuenteVersion
import com.example.domain.model.GuideModuleConfig
import com.example.domain.model.Question
import org.json.JSONArray
import org.json.JSONObject

class QuestionRepository(private val context: Context) {

    private var cachedQuestions: List<Question>? = null
    private var cachedModules: List<GuideModuleConfig>? = null
    var guideVersionInfo: String = "2026-2 (por confirmar con la guía ICFES vigente)"
        private set

    fun getQuestions(): List<Question> {
        cachedQuestions?.let { return it }

        val list = mutableListOf<Question>()
        try {
            val jsonString = context.assets.open("content/questions.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val opcionesObj = obj.optJSONObject("opciones") ?: JSONObject()
                val opcionesMap = mutableMapOf<String, String>()
                val keys = opcionesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    opcionesMap[key] = opcionesObj.getString(key)
                }

                val fuenteObj = obj.optJSONObject("fuente_version")
                val fuente = if (fuenteObj != null) {
                    FuenteVersion(
                        origen = fuenteObj.optString("origen", "original"),
                        guia = fuenteObj.optString("guia", "2026-2 (por confirmar)"),
                        fecha = fuenteObj.optString("fecha", "2026-10")
                    )
                } else {
                    FuenteVersion()
                }

                list.add(
                    Question(
                        id_pregunta = obj.getString("id_pregunta"),
                        modulo = obj.getString("modulo"),
                        competencia_tema = obj.getString("competencia_tema"),
                        enunciado = obj.getString("enunciado"),
                        tipo_pregunta = obj.optString("tipo_pregunta", "seleccion_multiple_unica"),
                        opciones = opcionesMap,
                        respuesta_correcta = obj.getString("respuesta_correcta"),
                        explicacion = obj.getString("explicacion"),
                        dificultad = obj.optInt("dificultad", 2),
                        fuente_version = fuente,
                        estado_revision = obj.optString("estado_revision", "borrador")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedQuestions = list
        return list
    }

    fun getGuideModules(): List<GuideModuleConfig> {
        cachedModules?.let { return it }

        val list = mutableListOf<GuideModuleConfig>()
        try {
            val jsonString = context.assets.open("content/guide_versions.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val version = root.optString("version", "2026-2")
            val estado = root.optString("estado", "por confirmar con la guía ICFES vigente")
            guideVersionInfo = "Versión: $version · $estado"

            val modArray = root.getJSONArray("modulos")
            for (i in 0 until modArray.length()) {
                val m = modArray.getJSONObject(i)
                list.add(
                    GuideModuleConfig(
                        id = m.getString("id"),
                        nombre = m.getString("nombre"),
                        tipo = m.getString("tipo"),
                        preguntas_simulacro = m.getInt("preguntas_simulacro"),
                        tiempo_minutos = m.getInt("tiempo_minutos"),
                        descripcion = m.getString("descripcion")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedModules = list
        return list
    }

    fun getQuestionsByModule(moduleId: String): List<Question> {
        return getQuestions().filter { it.modulo == moduleId }
    }

    fun getQuestionsByTopic(moduleId: String, topicId: String): List<Question> {
        return getQuestions().filter { it.modulo == moduleId && it.competencia_tema == topicId }
    }

    fun getQuestionById(questionId: String): Question? {
        return getQuestions().firstOrNull { it.id_pregunta == questionId }
    }
}
