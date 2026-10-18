/**
 * Validadores automáticos para Acierta Pro — Banco
 * Valida esquema, opciones, conteo de palabras, cálculos de Razonamiento Cuantitativo,
 * deduplicación, balance de clave y desviaciones respecto a la matriz oficial Saber Pro 2026-2.
 */

// Evaluación aritmética segura para Razonamiento Cuantitativo (sin eval)
function safeEvaluateMathExpression(expr) {
  if (typeof expr !== 'string') return null;
  // Solo permitir números, operadores básicos, paréntesis y espacios
  const clean = expr.trim();
  if (!/^[0-9+\-*/().\s^%]+$/.test(clean)) {
    return null;
  }
  try {
    // Parser recursivo básico de expresiones aritméticas
    let pos = 0;
    function peek() {
      while (pos < clean.length && clean[pos] === ' ') pos++;
      return pos < clean.length ? clean[pos] : null;
    }
    function get() {
      const c = peek();
      if (c !== null) pos++;
      return c;
    }
    function parseNumber() {
      peek();
      let numStr = '';
      if (peek() === '-') {
        numStr += get();
      }
      while (peek() !== null && /[0-9.]/.test(peek())) {
        numStr += get();
      }
      if (numStr === '' || numStr === '-') throw new Error('Número inválido');
      return parseFloat(numStr);
    }
    function parseFactor() {
      peek();
      if (peek() === '(') {
        get(); // '('
        const val = parseExpression();
        if (get() !== ')') throw new Error('Paréntesis sin cerrar');
        return val;
      }
      return parseNumber();
    }
    function parsePower() {
      let val = parseFactor();
      while (peek() === '^') {
        get();
        const exp = parseFactor();
        val = Math.pow(val, exp);
      }
      return val;
    }
    function parseTerm() {
      let val = parsePower();
      while (peek() === '*' || peek() === '/' || peek() === '%') {
        const op = get();
        const next = parsePower();
        if (op === '*') val = val * next;
        else if (op === '/') {
          if (next === 0) throw new Error('División por cero');
          val = val / next;
        } else if (op === '%') val = val % next;
      }
      return val;
    }
    function parseExpression() {
      let val = parseTerm();
      while (peek() === '+' || peek() === '-') {
        const op = get();
        const next = parseTerm();
        if (op === '+') val = val + next;
        else if (op === '-') val = val - next;
      }
      return val;
    }

    const result = parseExpression();
    if (pos < clean.length) {
      peek();
      if (pos < clean.length) throw new Error('Caracteres adicionales');
    }
    return result;
  } catch (err) {
    return null;
  }
}

/**
 * 1. Valida esquema JSON y presencia de campos obligatorios
 */
function validateSchema(question) {
  const errors = [];
  if (!question || typeof question !== 'object') {
    return { valid: false, errors: ['La pregunta debe ser un objeto JSON válido'] };
  }

  const requiredFields = [
    'id',
    'modulo',
    'competencia_o_afirmacion',
    'evidencia_asociada',
    'tipo_texto_o_contexto',
    'enunciado',
    'opciones',
    'respuesta_correcta',
    'explicacion',
    'dificultad',
    'generado_por_ia',
    'modelo',
    'version_del_prompt',
    'fecha',
    'estado_revision',
    'rotulo'
  ];

  for (const field of requiredFields) {
    if (question[field] === undefined || question[field] === null || question[field] === '') {
      errors.push(`Campo obligatorio faltante o vacío: ${field}`);
    }
  }

  if (question.rotulo !== 'pregunta de práctica, no oficial') {
    errors.push('El rótulo debe ser exactamente "pregunta de práctica, no oficial"');
  }

  if (question.generado_por_ia !== true) {
    errors.push('El campo generado_por_ia debe ser true');
  }

  if (question.estado_revision !== 'borrador') {
    errors.push('El estado_revision inicial debe ser "borrador"');
  }

  if (![1, 2, 3].includes(question.dificultad)) {
    errors.push(`Dificultad debe ser 1, 2 o 3 (recibido: ${question.dificultad})`);
  }

  if (typeof question.opciones !== 'object' || Array.isArray(question.opciones) || question.opciones === null) {
    errors.push('El campo "opciones" debe ser un mapa de clave a texto');
  } else {
    const keys = Object.keys(question.opciones);
    if (!keys.includes(question.respuesta_correcta)) {
      errors.push(`La respuesta_correcta "${question.respuesta_correcta}" no existe entre las opciones [${keys.join(', ')}]`);
    }
  }

  if (typeof question.explicacion !== 'object' || question.explicacion === null) {
    errors.push('El campo "explicacion" debe ser un objeto con "por_que_correcta" y "por_que_fallan_distractores"');
  } else {
    if (!question.explicacion.por_que_correcta) {
      errors.push('Falta explicacion.por_que_correcta');
    }
    if (!question.explicacion.por_que_fallan_distractores) {
      errors.push('Falta explicacion.por_que_fallan_distractores');
    }
  }

  // Regla especial para Competencias Ciudadanas
  if (question.modulo === 'competencias_ciudadanas') {
    if (question.requiere_verificacion_factual === undefined) {
      // Debe venir marcado explícitamente
      errors.push('En competencias_ciudadanas debe incluirse el campo requiere_verificacion_factual (boolean)');
    }
  }

  return {
    valid: errors.length === 0,
    errors
  };
}

/**
 * 2. Valida número de opciones según módulo y parte oficial
 */
function validateOptionsCount(question, matrixConfig) {
  const errors = [];
  if (!question.opciones || typeof question.opciones !== 'object') {
    return { valid: false, errors: ['Opciones no válidas'] };
  }
  const optionKeys = Object.keys(question.opciones);
  const count = optionKeys.length;

  if (question.modulo === 'ingles') {
    const parteNum = question.parte || (question.evidencia_asociada && question.evidencia_asociada.match(/parte\s*(\d)/i) ? parseInt(question.evidencia_asociada.match(/parte\s*(\d)/i)[1]) : null);
    const parteConfig = matrixConfig && matrixConfig.modulos && matrixConfig.modulos.ingles && matrixConfig.modulos.ingles.partes
      ? matrixConfig.modulos.ingles.partes.find(p => p.parte === parteNum)
      : null;

    const expected = parteConfig ? parteConfig.opciones_cantidad : (parteNum === 1 ? 8 : (parteNum >= 2 && parteNum <= 5 ? 3 : 4));
    if (count !== expected) {
      errors.push(`Módulo Inglés (Parte ${parteNum || 'desconocida'}): se esperaban ${expected} opciones, pero se encontraron ${count}`);
    }
  } else {
    // Razonamiento Cuantitativo, Lectura Crítica y Competencias Ciudadanas: 4 opciones por defecto (A–D) [por confirmar]
    const expected = 4;
    if (count !== expected) {
      errors.push(`Módulo ${question.modulo}: se esperaban ${expected} opciones por defecto (A-D) [por confirmar], recibidas ${count}`);
    }
    const standardKeys = ['A', 'B', 'C', 'D'];
    const hasStandardKeys = standardKeys.every(k => optionKeys.includes(k));
    if (!hasStandardKeys && count === 4) {
      errors.push(`Se esperaban las claves A, B, C, D; recibidas: [${optionKeys.join(', ')}]`);
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    receivedCount: count
  };
}

/**
 * 3. Valida longitud de textos en Lectura Crítica (máximo 500 palabras)
 */
function validateReadingWordCount(question, maxWords = 500) {
  if (question.modulo !== 'lectura_critica') {
    return { valid: true, wordCount: 0, maxWords, applies: false };
  }

  const textToEvaluate = (question.texto_lectura || question.tipo_texto_o_contexto || '') + ' ' + (question.enunciado || '');
  // Contar palabras limpiando signos de puntuación y espacios
  const words = textToEvaluate.trim().split(/\s+/).filter(w => w.length > 0);
  const wordCount = words.length;

  const valid = wordCount <= maxWords;
  return {
    valid,
    wordCount,
    maxWords,
    applies: true,
    error: valid ? null : `Lectura Crítica excede el límite de ${maxWords} palabras (actual: ${wordCount} palabras)`
  };
}

/**
 * 4. Valida cálculos en Razonamiento Cuantitativo mediante evaluador seguro de código
 */
function validateQuantitativeCalculations(question) {
  if (question.modulo !== 'razonamiento_cuantitativo') {
    return { valid: true, applies: false, message: 'No aplica para este módulo' };
  }

  if (!question.calculo_verificable) {
    // Si no incluye calculo_verificable explícito, pasa con advertencia [por confirmar]
    return { valid: true, applies: true, verified: false, message: 'Pregunta conceptual o sin expresión explícita [por confirmar]' };
  }

  const { expresion, resultado_esperado } = question.calculo_verificable;
  if (!expresion || resultado_esperado === undefined) {
    return { valid: false, applies: true, error: 'calculo_verificable debe contener "expresion" y "resultado_esperado"' };
  }

  const computed = safeEvaluateMathExpression(expresion);
  if (computed === null) {
    return { valid: false, applies: true, error: `No se pudo evaluar la expresión matemática de forma segura: "${expresion}"` };
  }

  const diff = Math.abs(computed - resultado_esperado);
  const isMatch = diff < 1e-4;

  return {
    valid: isMatch,
    applies: true,
    verified: true,
    expresion,
    computed,
    resultado_esperado,
    error: isMatch ? null : `Discrepancia en cálculo: expresión "${expresion}" dio ${computed}, pero se esperaba ${resultado_esperado}`
  };
}

/**
 * 5. Deduplicación contra el banco existente (usando similitud de n-gramas Jaccard)
 */
function tokenizeAndNGrams(text, n = 3) {
  if (!text) return new Set();
  const clean = text.toLowerCase().replace(/[^a-záéíóúüñ0-9\s]/gi, ' ').replace(/\s+/g, ' ').trim();
  const words = clean.split(' ');
  const ngrams = new Set();
  if (words.length < n) {
    ngrams.add(words.join(' '));
    return ngrams;
  }
  for (let i = 0; i <= words.length - n; i++) {
    ngrams.add(words.slice(i, i + n).join(' '));
  }
  return ngrams;
}

function calculateJaccardSimilarity(setA, setB) {
  if (setA.size === 0 && setB.size === 0) return 1.0;
  if (setA.size === 0 || setB.size === 0) return 0.0;
  let intersectionCount = 0;
  for (const item of setA) {
    if (setB.has(item)) intersectionCount++;
  }
  const unionCount = setA.size + setB.size - intersectionCount;
  return unionCount === 0 ? 0 : intersectionCount / unionCount;
}

function checkDeduplication(newQuestion, existingQuestions = [], threshold = 0.65) {
  if (!Array.isArray(existingQuestions) || existingQuestions.length === 0) {
    return { isDuplicate: false, maxSimilarity: 0, duplicateWithId: null };
  }

  const newGrams = tokenizeAndNGrams((newQuestion.enunciado || '') + ' ' + (newQuestion.texto_lectura || ''));
  let maxSim = 0;
  let duplicateId = null;

  for (const q of existingQuestions) {
    if (q.id === newQuestion.id) continue;
    const existingGrams = tokenizeAndNGrams((q.enunciado || '') + ' ' + (q.texto_lectura || ''));
    const sim = calculateJaccardSimilarity(newGrams, existingGrams);
    if (sim > maxSim) {
      maxSim = sim;
      duplicateId = q.id;
    }
  }

  return {
    isDuplicate: maxSim >= threshold,
    maxSimilarity: Math.round(maxSim * 100) / 100,
    duplicateWithId: maxSim >= threshold ? duplicateId : null,
    threshold
  };
}

/**
 * 6. Control de balance de posición de la clave correcta (A–D)
 */
function checkOptionKeyBalance(questions = []) {
  const counts = { A: 0, B: 0, C: 0, D: 0, otros: 0 };
  let total = 0;

  for (const q of questions) {
    const key = (q.respuesta_correcta || '').toUpperCase();
    if (['A', 'B', 'C', 'D'].includes(key)) {
      counts[key]++;
      total++;
    } else if (key) {
      counts.otros++;
      total++;
    }
  }

  const distribution = {};
  const idealPercentage = 25.0;
  let maxDeviation = 0;

  for (const k of ['A', 'B', 'C', 'D']) {
    const pct = total === 0 ? 0 : Math.round((counts[k] / total) * 1000) / 10;
    distribution[k] = { count: counts[k], percentage: pct };
    const dev = Math.abs(pct - idealPercentage);
    if (dev > maxDeviation) maxDeviation = dev;
  }

  return {
    total,
    counts,
    distribution,
    idealPercentage,
    maxDeviation: Math.round(maxDeviation * 10) / 10,
    isBalanced: total < 8 || maxDeviation <= 20.0 // Margen razonable con pocas preguntas
  };
}

/**
 * 7. Control de distribución respecto a porcentajes de la matriz oficial Saber Pro 2026-2
 */
function calculateDistributionDeviations(questions = [], matrixConfig = {}) {
  const modulosConfig = (matrixConfig && matrixConfig.modulos) || {};
  const approved = questions.filter(q => q.estado_revision === 'aprobada');
  const report = {};

  for (const [modKey, modData] of Object.entries(modulosConfig)) {
    const modQuestions = approved.filter(q => q.modulo === modKey);
    const modTotal = modQuestions.length;

    if (modKey === 'razonamiento_cuantitativo' && modData.competencias) {
      const breakdown = modData.competencias.map(comp => {
        const actualCount = modQuestions.filter(q =>
          q.competencia_o_afirmacion && q.competencia_o_afirmacion.toLowerCase().includes(comp.id.replace('_', ' ')) ||
          (q.competencia_o_afirmacion && q.competencia_o_afirmacion.toLowerCase().includes(comp.nombre.toLowerCase().slice(0, 10)))
        ).length;
        const actualPct = modTotal === 0 ? 0 : Math.round((actualCount / modTotal) * 100);
        return {
          id: comp.id,
          nombre: comp.nombre,
          objetivo_pct: comp.porcentaje_objetivo,
          actual_count: actualCount,
          actual_pct: actualPct,
          desviacion: actualPct - comp.porcentaje_objetivo
        };
      });
      report[modKey] = { total: modTotal, breakdown };
    } else if (modKey === 'lectura_critica' && modData.afirmaciones) {
      const breakdown = modData.afirmaciones.map(af => {
        const actualCount = modQuestions.filter(q =>
          (q.competencia_o_afirmacion && q.competencia_o_afirmacion.includes(af.id)) ||
          (q.competencia_o_afirmacion && q.competencia_o_afirmacion.includes(af.nombre.slice(0, 15)))
        ).length;
        const actualPct = modTotal === 0 ? 0 : Math.round((actualCount / modTotal) * 100);
        return {
          id: af.id,
          nombre: af.nombre,
          objetivo_pct: af.porcentaje_objetivo,
          actual_count: actualCount,
          actual_pct: actualPct,
          desviacion: actualPct - af.porcentaje_objetivo
        };
      });
      report[modKey] = { total: modTotal, breakdown };
    } else if (modKey === 'competencias_ciudadanas' && modData.competencias) {
      const breakdown = modData.competencias.map(comp => {
        const actualCount = modQuestions.filter(q =>
          q.competencia_o_afirmacion && q.competencia_o_afirmacion.toLowerCase().includes(comp.id)
        ).length;
        const actualPct = modTotal === 0 ? 0 : Math.round((actualCount / modTotal) * 100);
        return {
          id: comp.id,
          nombre: comp.nombre,
          objetivo_pct: comp.porcentaje_objetivo,
          actual_count: actualCount,
          actual_pct: actualPct,
          desviacion: actualPct - comp.porcentaje_objetivo
        };
      });
      report[modKey] = { total: modTotal, breakdown };
    } else if (modKey === 'ingles' && modData.partes) {
      const breakdown = modData.partes.map(p => {
        const actualCount = modQuestions.filter(q =>
          (q.parte === p.parte) ||
          (q.evidencia_asociada && q.evidencia_asociada.toLowerCase().includes(`parte ${p.parte}`))
        ).length;
        const actualPct = modTotal === 0 ? 0 : Math.round((actualCount / modTotal) * 100);
        return {
          parte: p.parte,
          nombre: p.nombre,
          objetivo_pct: p.porcentaje_objetivo,
          actual_count: actualCount,
          actual_pct: actualPct,
          desviacion: actualPct - p.porcentaje_objetivo
        };
      });
      report[modKey] = { total: modTotal, breakdown };
    } else {
      report[modKey] = { total: modTotal };
    }
  }

  return {
    totalAprobadas: approved.length,
    reportePorModulo: report
  };
}

/**
 * 8. Pipeline integral de validación antes de revisión humana
 */
function runFullValidationPipeline(question, options = {}) {
  const { matrixConfig = {}, existingQuestions = [] } = options;
  const results = {
    isValid: true,
    reasons: [],
    details: {}
  };

  // Validación 1: Esquema
  const schemaRes = validateSchema(question);
  results.details.schema = schemaRes;
  if (!schemaRes.valid) {
    results.isValid = false;
    results.reasons.push(...schemaRes.errors);
  }

  // Validación 2: Opciones
  const optionsRes = validateOptionsCount(question, matrixConfig);
  results.details.optionsCount = optionsRes;
  if (!optionsRes.valid) {
    results.isValid = false;
    results.reasons.push(...optionsRes.errors);
  }

  // Validación 3: Límite de palabras en Lectura Crítica (<=500)
  const wordCountRes = validateReadingWordCount(question, 500);
  results.details.wordCount = wordCountRes;
  if (wordCountRes.applies && !wordCountRes.valid) {
    results.isValid = false;
    results.reasons.push(wordCountRes.error);
  }

  // Validación 4: Verificación matemática en Razonamiento Cuantitativo
  const mathRes = validateQuantitativeCalculations(question);
  results.details.math = mathRes;
  if (mathRes.applies && !mathRes.valid) {
    results.isValid = false;
    results.reasons.push(mathRes.error);
  }

  // Validación 5: Deduplicación
  const dedupRes = checkDeduplication(question, existingQuestions);
  results.details.deduplication = dedupRes;
  if (dedupRes.isDuplicate) {
    results.isValid = false;
    results.reasons.push(`Posible duplicado detectado (similitud: ${Math.round(dedupRes.maxSimilarity * 100)}% con ${dedupRes.duplicateWithId})`);
  }

  return results;
}

/**
 * Sanitización de entrada contra inyecciones SQL y XSS (Sección 5.2)
 */
function sanitizeInput(inputText) {
  if (!inputText || typeof inputText !== 'string') return '';
  return inputText
    // Eliminar etiquetas HTML y fragmentos de código ejecutable (Protección XSS)
    .replace(/<[^>]*>/g, '')
    // Escapar comillas simples y dobles para prevenir rompimiento de cadenas en SQL
    .replace(/'/g, "''")
    .replace(/"/g, '&quot;')
    // Remover caracteres de control y nulos
    .replace(/\0/g, '')
    // Neutralizar palabras clave reservadas de manipulación SQL (Protección SQLi)
    .replace(/(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( INTO)?|MERGE|SELECT|UPDATE|UNION( ALL)?)\s+/gui, '')
    .trim();
}

/**
 * Validador de evaluación de calidad del Agente 2 (Juez ICFES) (Sección 2.4)
 */
function validateJudgeEvaluation(evalObj) {
  if (!evalObj || typeof evalObj !== 'object') {
    return { valid: false, approved: false, error: 'La evaluación del Juez debe ser un objeto JSON válido' };
  }
  const { overall_score, unambiguity_flag, pedagogical_alignment_score, difficulty_calibration, rejection_reasons } = evalObj;

  const validScore = typeof overall_score === 'number' && overall_score >= 0 && overall_score <= 100;
  const validFlag = typeof unambiguity_flag === 'boolean';
  const validPedagogy = typeof pedagogical_alignment_score === 'number' && pedagogical_alignment_score >= 0 && pedagogical_alignment_score <= 100;
  const validDiff = ['Bajo', 'Medio', 'Alto'].includes(difficulty_calibration);
  const validReasons = Array.isArray(rejection_reasons);

  if (!validScore || !validFlag || !validPedagogy || !validDiff || !validReasons) {
    return { valid: false, approved: false, error: 'Estructura o rangos inválidos en la evaluación del Juez ICFES' };
  }

  // Criterio de aprobación estricto: puntaje >= 85 y unambiguity_flag true
  const approved = overall_score >= 85 && unambiguity_flag === true;
  return { valid: true, approved, overall_score, unambiguity_flag, rejection_reasons };
}

/**
 * Validador del esquema de evaluación de Comunicación Escrita (Sección 3.1)
 */
function validateEssayRubricEvaluation(evalObj) {
  if (!evalObj || typeof evalObj !== 'object') {
    return { valid: false, error: 'Evaluación de ensayo inválida' };
  }
  const levels = ['Insuficiente', 'Mínimo', 'Satisfactorio', 'Avanzado'];
  const scores = [
    evalObj.score_cohesion,
    evalObj.score_coherencia,
    evalObj.score_estructura_argumentativa,
    evalObj.score_ortografia_gramatica
  ];

  const scoresValid = scores.every(s => typeof s === 'number' && s >= 1 && s <= 5);
  const levelValid = levels.includes(evalObj.performance_level);
  const feedbackValid = evalObj.qualitative_feedback &&
    Array.isArray(evalObj.qualitative_feedback.strengths) &&
    Array.isArray(evalObj.qualitative_feedback.improvements) &&
    typeof evalObj.qualitative_feedback.general_comment === 'string';

  if (!scoresValid || !levelValid || !feedbackValid) {
    return { valid: false, error: 'Estructura de rúbrica de Comunicación Escrita no cumple con el esquema oficial' };
  }

  return { valid: true };
}

module.exports = {
  safeEvaluateMathExpression,
  validateSchema,
  validateOptionsCount,
  validateReadingWordCount,
  validateQuantitativeCalculations,
  tokenizeAndNGrams,
  calculateJaccardSimilarity,
  checkDeduplication,
  checkOptionKeyBalance,
  calculateDistributionDeviations,
  runFullValidationPipeline,
  sanitizeInput,
  validateJudgeEvaluation,
  validateEssayRubricEvaluation
};

