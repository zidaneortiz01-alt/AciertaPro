/**
 * Cola de generación asíncrona de preguntas con concurrencia 1,
 * reintentos con backoff exponencial, tope diario configurable y validación previa a borrador.
 * Nunca se genera por estudiante en vivo.
 */

const { runFullValidationPipeline } = require('./validators');

class GenerationQueue {
  constructor(geminiClient, bankStorage, appConfig, matrixConfig, promptTemplates) {
    this.geminiClient = geminiClient;
    this.bankStorage = bankStorage;
    this.appConfig = appConfig;
    this.matrixConfig = matrixConfig;
    this.promptTemplates = promptTemplates;

    this.queue = [];
    this.isProcessing = false;
    this.dailyCount = 0;
    this.lastResetDate = new Date().toISOString().slice(0, 10);
    this.history = []; // Historial de lotes procesados
  }

  getDailyCap() {
    return (this.appConfig && this.appConfig.daily_generation_cap) || 250;
  }

  checkAndResetDailyCount() {
    const today = new Date().toISOString().slice(0, 10);
    if (this.lastResetDate !== today) {
      this.dailyCount = 0;
      this.lastResetDate = today;
    }
  }

  getStatus() {
    this.checkAndResetDailyCount();
    return {
      isProcessing: this.isProcessing,
      pendingBatches: this.queue.length,
      dailyCount: this.dailyCount,
      dailyCap: this.getDailyCap(),
      capReached: this.dailyCount >= this.getDailyCap(),
      concurrency: 1,
      recentBatches: this.history.slice(0, 10)
    };
  }

  /**
   * Encola un nuevo lote de generación solicitado por un docente autorizado
   */
  enqueueBatch({ modulo, competencia, evidencia, dificultad, cantidad, userEmail }) {
    this.checkAndResetDailyCount();

    const batchId = `batch_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const batch = {
      id: batchId,
      modulo,
      competencia,
      evidencia,
      dificultad: parseInt(dificultad, 10) || 2,
      cantidad: Math.min(Math.max(parseInt(cantidad, 10) || 1, 1), 10), // Máximo 10 por lote en UI docente
      userEmail,
      status: 'en_espera',
      generadas: 0,
      rechazadas: 0,
      errores: [],
      preguntasIds: [],
      createdAt: new Date().toISOString(),
      completedAt: null
    };

    this.queue.push(batch);
    this.history.unshift(batch);

    // Iniciar procesamiento si la cola no está activa
    if (!this.isProcessing) {
      this.processNextInQueue();
    }

    return batch;
  }

  async processNextInQueue() {
    if (this.queue.length === 0) {
      this.isProcessing = false;
      return;
    }

    this.isProcessing = true;
    const currentBatch = this.queue.shift();
    currentBatch.status = 'procesando';

    for (let i = 0; i < currentBatch.cantidad; i++) {
      this.checkAndResetDailyCount();
      if (this.dailyCount >= this.getDailyCap()) {
        currentBatch.errores.push(`Tope diario de ${this.getDailyCap()} preguntas alcanzado. Deteniendo lote.`);
        break;
      }

      try {
        const question = await this.generateWithExponentialBackoff(currentBatch, 3);
        if (question) {
          // Validar con pipeline automático
          const existingQuestions = this.bankStorage.getAllQuestions();
          const validationResult = runFullValidationPipeline(question, {
            matrixConfig: this.matrixConfig,
            existingQuestions
          });

          if (!validationResult.isValid) {
            currentBatch.rechazadas++;
            currentBatch.errores.push(`Pregunta descartada por validación automática: ${validationResult.reasons.join('; ')}`);
            continue;
          }

          // Pasada de verificación ciega (si Gemini está configurado)
          if (this.geminiClient.isConfigured()) {
            try {
              const blindResult = await this.geminiClient.verifyQuestionBlindly(question, this.appConfig.gemini_model);
              if (!blindResult.match) {
                currentBatch.rechazadas++;
                currentBatch.errores.push(`Pregunta descartada en verificación ciega: el modelo eligió ${blindResult.predicted} pero la clave era ${blindResult.expected}`);
                continue;
              }
            } catch (err) {
              console.warn('Advertencia en verificación ciega:', err.message);
            }
          }

          // Pregunta validada con éxito -> Guardar como borrador para revisión humana
          question.estado_revision = 'borrador';
          this.bankStorage.saveQuestion(question);
          this.dailyCount++;
          currentBatch.generadas++;
          currentBatch.preguntasIds.push(question.id);
        }
      } catch (err) {
        currentBatch.rechazadas++;
        currentBatch.errores.push(`Error generando ítem: ${err.message}`);
      }
    }

    currentBatch.status = currentBatch.generadas > 0 ? 'completado' : 'fallido';
    currentBatch.completedAt = new Date().toISOString();

    // Concurrencia 1: procesar el siguiente secuencialmente
    setTimeout(() => {
      this.processNextInQueue();
    }, 500);
  }

  async generateWithExponentialBackoff(batch, maxRetries = 3) {
    let attempt = 0;
    let delay = 1000; // 1s inicial

    while (attempt < maxRetries) {
      try {
        if (!this.geminiClient.isConfigured()) {
          // Generador simulado seguro para pruebas locales cuando no hay API key
          return this.createSimulatedQuestion(batch);
        }

        return await this.geminiClient.generateQuestion({
          modulo: batch.modulo,
          competencia: batch.competencia,
          evidencia: batch.evidencia,
          dificultad: batch.dificultad,
          promptTemplates: this.promptTemplates,
          appConfig: this.appConfig,
          matrixConfig: this.matrixConfig
        });
      } catch (err) {
        attempt++;
        if (attempt >= maxRetries) {
          throw err;
        }
        // Espera exponencial con jitter
        const jitter = Math.random() * 200;
        await new Promise(r => setTimeout(r, delay + jitter));
        delay *= 2;
      }
    }
    return null;
  }

  createSimulatedQuestion(batch) {
    const id = `q_${batch.modulo.slice(0, 3)}_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const promptVersion = this.appConfig.prompt_version || 'v2026.2.1';

    if (batch.modulo === 'razonamiento_cuantitativo') {
      return {
        id,
        modulo: 'razonamiento_cuantitativo',
        competencia_o_afirmacion: batch.competencia || 'interpretacion_representacion',
        evidencia_asociada: batch.evidencia || 'Comprende y transforma información cuantitativa presentada en distintos formatos.',
        tipo_texto_o_contexto: 'laboral/ocupacional',
        enunciado: 'Una empresa distribuidora en Colombia analiza el consumo mensual de combustible de su flota. Si un camión recorre 1200 km al mes y su rendimiento es de 8 km por galón, ¿cuántos galones requiere al mes?',
        opciones: {
          A: '150 galones',
          B: '120 galones',
          C: '96 galones',
          D: '180 galones'
        },
        respuesta_correcta: 'A',
        explicacion: {
          por_que_correcta: 'El cálculo directo es 1200 km dividido por 8 km/galón, lo cual da exactamente 150 galones.',
          por_que_fallan_distractores: 'B confunde 1200 / 10. C calcula 1200 * 0.08. D agrega un margen del 20% no solicitado.'
        },
        dificultad: batch.dificultad,
        generado_por_ia: true,
        modelo: this.appConfig.gemini_model,
        version_del_prompt: promptVersion,
        fecha: new Date().toISOString(),
        estado_revision: 'borrador',
        rotulo: 'pregunta de práctica, no oficial',
        calculo_verificable: {
          expresion: '1200 / 8',
          resultado_esperado: 150
        }
      };
    }

    if (batch.modulo === 'lectura_critica') {
      return {
        id,
        modulo: 'lectura_critica',
        competencia_o_afirmacion: batch.competencia || 'afirmacion_2',
        evidencia_asociada: batch.evidencia || 'Reconoce la estructura y el propósito comunicativo global de un texto.',
        tipo_texto_o_contexto: 'continuo_informativo',
        texto_lectura: 'La preservación de los páramos en la cordillera de los Andes colombianos representa un desafío estructural para la seguridad hídrica urbana. A pesar de los marcos regulatorios que delimitan estas zonas frágiles, la presión derivada de la pequeña minería y de la agricultura de subsistencia continúa erosionando la cobertura vegetal autóctona de frailejones. Esta degradación compromete la función de captación y regulación de humedad atmosférica, vital para los acueductos metropolitanos.',
        enunciado: 'A partir del texto anterior, ¿cuál es la tesis central que articula la argumentación del autor?',
        opciones: {
          A: 'La conservación de los páramos es indispensable para la sostenibilidad del suministro de agua potable frente a presiones productivas.',
          B: 'La agricultura campesina debe erradicarse completamente de la cordillera andina para evitar la contaminación.',
          C: 'La delimitación jurídica ha resuelto de forma definitiva los conflictos socioambientales en las altas montañas.',
          D: 'Los frailejones son la única especie vegetal capaz de retener niebla en los ecosistemas de alta montaña.'
        },
        respuesta_correcta: 'A',
        explicacion: {
          por_que_correcta: 'Sintetiza la relación entre el valor regulador del agua en los páramos y las amenazas de actividades humanas.',
          por_que_fallan_distractores: 'B es una generalización extrema no respaldada. C contradice directamente el texto. D es una aseveración biológica reduccionista no planteada.'
        },
        dificultad: batch.dificultad,
        generado_por_ia: true,
        modelo: this.appConfig.gemini_model,
        version_del_prompt: promptVersion,
        fecha: new Date().toISOString(),
        estado_revision: 'borrador',
        rotulo: 'pregunta de práctica, no oficial'
      };
    }

    if (batch.modulo === 'competencias_ciudadanas') {
      return {
        id,
        modulo: 'competencias_ciudadanas',
        competencia_o_afirmacion: batch.competencia || 'multiperspectivismo',
        evidencia_asociada: batch.evidencia || 'Reconoce que las posiciones de los actores sociales reflejan distintos intereses y principios.',
        tipo_texto_o_contexto: 'comunitario/social',
        enunciado: 'En un municipio ribereño colombiano, la alcaldía propone peatonalizar el malecón turístico para incentivar el comercio gastronómico y reducir accidentes. Un colectivo de transportadores informales y pescadores artesanales se opone argumentando que la medida restringe el descargue temprano de mercancías. En esta situación, ¿qué intereses se encuentran en tensión?',
        opciones: {
          A: 'El impulso al turismo y seguridad de transeúntes frente al derecho al trabajo y la logística de productores tradicionales.',
          B: 'La regulación fiscal de los restaurantes frente al pago de peajes municipales.',
          C: 'La protección de especies marinas frente a la construcción de vías terrestres.',
          D: 'El orden público policial frente a la autonomía de comunidades indígenas locales.'
        },
        respuesta_correcta: 'A',
        explicacion: {
          por_que_correcta: 'Identifica con exactitud las dos perspectivas legítimas en conflicto: dinamización comercial/peatonal vs acceso laboral de pescadores/transportadores.',
          por_que_fallan_distractores: 'B inventa disputas impositivas inexistentes. C y D introducen dimensiones ambientales o étnicas ajenas a los actores descritos.'
        },
        dificultad: batch.dificultad,
        generado_por_ia: true,
        modelo: this.appConfig.gemini_model,
        version_del_prompt: promptVersion,
        fecha: new Date().toISOString(),
        estado_revision: 'borrador',
        rotulo: 'pregunta de práctica, no oficial',
        requiere_verificacion_factual: false
      };
    }

    // Por defecto Inglés (ejemplo Parte 1 con 8 opciones)
    return {
      id,
      modulo: 'ingles',
      competencia_o_afirmacion: 'Parte 1: Avisos y vocabulario',
      evidencia_asociada: 'Relaciona avisos con los lugares donde se encontrarían.',
      tipo_texto_o_contexto: 'Aviso público cotidiano',
      parte: 1,
      enunciado: '¿Dónde se puede ver este aviso? "SILENCE PLEASE. EXAMINATIONS IN PROGRESS. TURN OFF CELL PHONES."',
      opciones: {
        A: 'In a school library or testing hall',
        B: 'At a football stadium',
        C: 'In a supermarket cashier line',
        D: 'On a public bus',
        E: 'At a gas station',
        F: 'In an open street market',
        G: 'Inside a cinema hall',
        H: 'At an amusement park'
      },
      respuesta_correcta: 'A',
      explicacion: {
        por_que_correcta: 'El aviso exige silencio estricto por exámenes académicos, característico de bibliotecas o aulas de prueba.',
        por_que_fallan_distractores: 'Las otras opciones corresponden a lugares comerciales, deportivos o de transporte sin aplicación de pruebas formales.'
      },
      dificultad: batch.dificultad,
      generado_por_ia: true,
      modelo: this.appConfig.gemini_model,
      version_del_prompt: promptVersion,
      fecha: new Date().toISOString(),
      estado_revision: 'borrador',
      rotulo: 'pregunta de práctica, no oficial'
    };
  }
}

module.exports = GenerationQueue;
