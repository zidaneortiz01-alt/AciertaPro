/**
 * Cliente seguro de Gemini API exclusivo del servidor
 * La clave de API se obtiene de process.env.GEMINI_API_KEY y nunca se expone al cliente.
 * Salida estructurada con esquema JSON estricto.
 */

const https = require('https');

class GeminiServerClient {
  constructor(apiKey, defaultModel = 'gemini-2.5-flash') {
    this.apiKey = apiKey || process.env.GEMINI_API_KEY || '';
    this.defaultModel = defaultModel;
  }

  isConfigured() {
    return Boolean(this.apiKey && this.apiKey.trim().length > 0);
  }

  async callGeminiApi(modelName, payload) {
    if (!this.isConfigured()) {
      throw new Error('GEMINI_API_KEY no está configurada en los secretos del servidor.');
    }

    const model = modelName || this.defaultModel;
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${this.apiKey}`;

    const bodyString = JSON.stringify(payload);

    return new Promise((resolve, reject) => {
      const parsedUrl = new URL(url);
      const options = {
        hostname: parsedUrl.hostname,
        path: parsedUrl.pathname + parsedUrl.search,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(bodyString)
        },
        timeout: 45000
      };

      const req = https.request(options, (res) => {
        let rawData = '';
        res.on('data', chunk => rawData += chunk);
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const parsed = JSON.parse(rawData);
              resolve(parsed);
            } catch (err) {
              reject(new Error(`Respuesta de Gemini no es JSON válido: ${err.message}`));
            }
          } else {
            let errorMsg = `Error HTTP ${res.statusCode} de Gemini API`;
            try {
              const errJson = JSON.parse(rawData);
              if (errJson.error && errJson.error.message) {
                errorMsg += `: ${errJson.error.message}`;
              }
            } catch (_) {}
            reject(new Error(errorMsg));
          }
        });
      });

      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Timeout esperando respuesta de Gemini API'));
      });

      req.on('error', err => reject(err));
      req.write(bodyString);
      req.end();
    });
  }

  /**
   * Genera preguntas individuales o en lote con salida estructurada JSON
   */
  async generateQuestion({ modulo, competencia, evidencia, dificultad, promptTemplates, appConfig, matrixConfig }) {
    const model = (appConfig && appConfig.gemini_model) || this.defaultModel;
    const promptVersion = (appConfig && appConfig.prompt_version) || 'v2026.2.1';

    // Determinar opciones esperadas
    let optionCount = 4; // Por defecto [por confirmar]
    if (modulo === 'ingles') {
      const parteMatch = (evidencia || '').match(/parte\s*(\d)/i) || (competencia || '').match(/parte\s*(\d)/i);
      const pNum = parteMatch ? parseInt(parteMatch[1]) : 1;
      const partes = matrixConfig?.modulos?.ingles?.partes || [];
      const pConf = partes.find(p => p.parte === pNum);
      optionCount = pConf ? pConf.opciones_cantidad : 4;
    }

    const systemPrompt = promptTemplates.system_instructions;
    const userPrompt = `
Genera una pregunta de práctica Saber Pro 2026-2 con las siguientes especificaciones:
- Módulo: ${modulo}
- Competencia / Afirmación: ${competencia}
- Evidencia / Contexto: ${evidencia || 'Contexto profesional o cotidiano en Colombia'}
- Nivel de dificultad: ${dificultad} (1=Básico, 2=Medio, 3=Avanzado)
- Cantidad de opciones requeridas: exactamente ${optionCount} opciones (A, B, C, D${optionCount > 4 ? ', ...' : ''}).

Reglas estrictas obligatorias:
1. La pregunta debe ser COMPLETAMENTE ORIGINAL. Nunca reproduzcas ni adaptes ítems de cuadernillos del ICFES.
2. Una sola respuesta correcta; distractores plausibles basados en errores de razonamiento comunes.
3. Contexto colombiano neutro, sin conocimiento especializado no brindado.
4. Si es Lectura Crítica, incluye texto_lectura de máximo 500 palabras.
5. Si es Razonamiento Cuantitativo, incluye calculo_verificable con {"expresion": "...", "resultado_esperado": ...}.
6. Si es Competencias Ciudadanas, si mencionas conceptos normativos y dudas, marca requiere_verificacion_factual=true.
7. Rótulo: exactamente "pregunta de práctica, no oficial".
8. estado_revision: "borrador".
9. generado_por_ia: true.
10. version_del_prompt: "${promptVersion}".
11. modelo: "${model}".
`;

    const payload = {
      contents: [
        {
          role: 'user',
          parts: [{ text: `${systemPrompt}\n\n${userPrompt}` }]
        }
      ],
      generationConfig: {
        temperature: 0.3,
        responseMimeType: 'application/json',
        responseSchema: promptTemplates.json_schema_prompt
      }
    };

    const response = await this.callGeminiApi(model, payload);
    const candidateText = response?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!candidateText) {
      throw new Error('Gemini API no retornó contenido para la pregunta');
    }

    const question = JSON.parse(candidateText);
    // Garantizar metadatos obligatorios
    question.id = question.id || `q_${modulo.slice(0, 3)}_${Date.now()}_${Math.floor(Math.random()*1000)}`;
    question.modelo = model;
    question.version_del_prompt = promptVersion;
    question.generado_por_ia = true;
    question.estado_revision = 'borrador';
    question.rotulo = 'pregunta de práctica, no oficial';
    question.fecha = new Date().toISOString();

    return question;
  }

  /**
   * Pasada de verificación ciega: el modelo debe resolver la pregunta sin conocer la clave
   */
  async verifyQuestionBlindly(question, modelName) {
    const model = modelName || this.defaultModel;

    const blindPrompt = `
Lee con atención la siguiente pregunta de evaluación Saber Pro y determina cuál de las opciones es la correcta.
NO conoces la clave previa. Razona paso a paso brevemente y selecciona una única letra.

Módulo: ${question.modulo}
Texto/Contexto: ${question.texto_lectura || question.tipo_texto_o_contexto || ''}
Enunciado: ${question.enunciado}
Opciones:
${Object.entries(question.opciones || {}).map(([k, v]) => `${k}) ${v}`).join('\n')}

Devuelve ÚNICAMENTE un objeto JSON con el siguiente esquema:
{
  "letra_elegida": "A|B|C|D...",
  "justificacion_breve": "string"
}
`;

    const payload = {
      contents: [{ role: 'user', parts: [{ text: blindPrompt }] }],
      generationConfig: {
        temperature: 0.1,
        responseMimeType: 'application/json',
        responseSchema: {
          type: 'object',
          properties: {
            letra_elegida: { "type": "string" },
            justificacion_breve: { "type": "string" }
          },
          required: ["letra_elegida", "justificacion_breve"]
        }
      }
    };

    const response = await this.callGeminiApi(model, payload);
    const candidateText = response?.candidates?.[0]?.content?.parts?.[0]?.text;
    const solved = JSON.parse(candidateText);

    const match = (solved.letra_elegida || '').toUpperCase() === (question.respuesta_correcta || '').toUpperCase();
    return {
      match,
      predicted: (solved.letra_elegida || '').toUpperCase(),
      expected: (question.respuesta_correcta || '').toUpperCase(),
      justificacion: solved.justificacion_breve
    };
  }

  /**
   * Retroalimentación estructurada para Comunicación Escrita (Fase 3, desactivado por defecto)
   * NO devuelve puntaje 0-300 y NO persiste datos.
   */
  async evaluateEssayFeedback({ essayText, promptAffirmation, modelName }) {
    const model = modelName || this.defaultModel;

    const evaluationPrompt = `
Eres un evaluador pedagógico de Comunicación Escrita para el examen Saber Pro de Colombia.
Evalúa el siguiente texto argumentativo de un estudiante en respuesta a la consigna:
"${promptAffirmation}"

Criterios oficiales a evaluar (cualitativos, constructivos):
1. Planteamiento: claridad de la tesis y toma de postura (de acuerdo, en desacuerdo o parcialmente).
2. Organización: coherencia, cohesión y estructura lógica de los párrafos.
3. Forma de expresión: precisión léxica, sintaxis y ortografía.
4. Pertinencia: relevancia de los argumentos frente al tema propuesto.

REGLAS CRÍTICAS:
- NO asignes puntajes numéricos ni escalas de 0 a 300.
- Brinda sugerencias específicas de mejora y fortalezas para cada criterio.

Texto del estudiante:
"${essayText}"
`;

    const payload = {
      contents: [{ role: 'user', parts: [{ text: evaluationPrompt }] }],
      generationConfig: {
        temperature: 0.2,
        responseMimeType: 'application/json',
        responseSchema: {
          type: 'object',
          properties: {
            planteamiento: {
              type: 'object',
              properties: {
                fortalezas: { type: 'string' },
                aspectos_a_mejorar: { type: 'string' }
              },
              required: ['fortalezas', 'aspectos_a_mejorar']
            },
            organizacion: {
              type: 'object',
              properties: {
                fortalezas: { type: 'string' },
                aspectos_a_mejorar: { type: 'string' }
              },
              required: ['fortalezas', 'aspectos_a_mejorar']
            },
            forma_de_expresion: {
              type: 'object',
              properties: {
                fortalezas: { type: 'string' },
                aspectos_a_mejorar: { type: 'string' }
              },
              required: ['fortalezas', 'aspectos_a_mejorar']
            },
            pertinencia: {
              type: 'object',
              properties: {
                fortalezas: { type: 'string' },
                aspectos_a_mejorar: { type: 'string' }
              },
              required: ['fortalezas', 'aspectos_a_mejorar']
            },
            recomendacion_general: { type: 'string' }
          },
          required: ['planteamiento', 'organizacion', 'forma_de_expresion', 'pertinencia', 'recomendacion_general']
        }
      }
    };

    const response = await this.callGeminiApi(model, payload);
    const candidateText = response?.candidates?.[0]?.content?.parts?.[0]?.text;
    return JSON.parse(candidateText);
  }
}

module.exports = GeminiServerClient;
