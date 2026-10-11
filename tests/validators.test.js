/**
 * Pruebas unitarias automatizadas de validadores para Acierta Pro — Banco
 * Ejecutables con 'node --test'
 */

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');

const {
  validateSchema,
  validateOptionsCount,
  validateReadingWordCount,
  validateQuantitativeCalculations,
  checkDeduplication,
  checkOptionKeyBalance,
  calculateDistributionDeviations,
  safeEvaluateMathExpression
} = require('../src/validators');

const BankStorage = require('../src/bankStorage');

// Cargar matriz oficial para pruebas
const matrixConfig = JSON.parse(
  fs.readFileSync(path.join(__dirname, '..', 'config', 'matrix_config.json'), 'utf-8')
);

describe('1. Validador de Esquema y Metadatos Obligatorios', () => {
  test('Pregunta válida pasa la validación de esquema', () => {
    const validQuestion = {
      id: "q_test_01",
      modulo: "razonamiento_cuantitativo",
      competencia_o_afirmacion: "interpretacion_representacion",
      evidencia_asociada: "Transforma representaciones cuantitativas",
      tipo_texto_o_contexto: "familiar/personal",
      enunciado: "¿Cuál es el valor del 20% de 500?",
      opciones: { A: "100", B: "150", C: "200", D: "250" },
      respuesta_correcta: "A",
      explicacion: {
        por_que_correcta: "20% de 500 es 0.20 * 500 = 100",
        por_que_fallan_distractores: "B, C y D calculan porcentajes superiores"
      },
      dificultad: 1,
      generado_por_ia: true,
      modelo: "gemini-2.5-flash",
      version_del_prompt: "v2026.2.1",
      fecha: new Date().toISOString(),
      estado_revision: "borrador",
      rotulo: "pregunta de práctica, no oficial"
    };

    const res = validateSchema(validQuestion);
    assert.equal(res.valid, true, `Errores: ${res.errors.join(', ')}`);
  });

  test('Falla si falta el rótulo exacto "pregunta de práctica, no oficial"', () => {
    const invalidQuestion = {
      id: "q_test_02",
      modulo: "razonamiento_cuantitativo",
      competencia_o_afirmacion: "interpretacion_representacion",
      evidencia_asociada: "Evidencia",
      tipo_texto_o_contexto: "familiar",
      enunciado: "Pregunta sin rotulo oficial",
      opciones: { A: "1", B: "2", C: "3", D: "4" },
      respuesta_correcta: "A",
      explicacion: { por_que_correcta: "ok", por_que_fallan_distractores: "fallo" },
      dificultad: 2,
      generado_por_ia: true,
      modelo: "gemini-2.5-flash",
      version_del_prompt: "v2026.2.1",
      fecha: new Date().toISOString(),
      estado_revision: "borrador",
      rotulo: "pregunta oficial icfes" // Rótulo incorrecto
    };

    const res = validateSchema(invalidQuestion);
    assert.equal(res.valid, false);
    assert.ok(res.errors.some(e => e.includes('rótulo debe ser exactamente')));
  });

  test('Competencias Ciudadanas requiere campo requiere_verificacion_factual', () => {
    const ccQuestion = {
      id: "q_cc_01",
      modulo: "competencias_ciudadanas",
      competencia_o_afirmacion: "conocimientos",
      evidencia_asociada: "Conoce la Constitución",
      tipo_texto_o_contexto: "comunitario",
      enunciado: "¿Qué mecanismo protege los derechos fundamentales en Colombia?",
      opciones: { A: "Acción de tutela", B: "Derecho de petición", C: "Habeas corpus", D: "Referendo" },
      respuesta_correcta: "A",
      explicacion: { por_que_correcta: "Artículo 86 de la CP", por_que_fallan_distractores: "Otros mecanismos" },
      dificultad: 2,
      generado_por_ia: true,
      modelo: "gemini-2.5-flash",
      version_del_prompt: "v2026.2.1",
      fecha: new Date().toISOString(),
      estado_revision: "borrador",
      rotulo: "pregunta de práctica, no oficial"
      // Omite requiere_verificacion_factual
    };

    const res = validateSchema(ccQuestion);
    assert.equal(res.valid, false);
    assert.ok(res.errors.some(e => e.includes('requiere_verificacion_factual')));
  });
});

describe('2. Validador de Opciones según Módulo y Matriz', () => {
  test('Razonamiento Cuantitativo debe tener exactamente 4 opciones por defecto [por confirmar]', () => {
    const q3options = {
      modulo: "razonamiento_cuantitativo",
      opciones: { A: "10", B: "20", C: "30" }
    };
    const res = validateOptionsCount(q3options, matrixConfig);
    assert.equal(res.valid, false);
    assert.ok(res.errors.some(e => e.includes('se esperaban 4 opciones')));
  });

  test('Inglés Parte 1 debe tener 8 opciones (A-H)', () => {
    const qInglesP1 = {
      modulo: "ingles",
      parte: 1,
      opciones: { A: "1", B: "2", C: "3", D: "4", E: "5", F: "6", G: "7", H: "8" }
    };
    const res = validateOptionsCount(qInglesP1, matrixConfig);
    assert.equal(res.valid, true);
  });

  test('Inglés Parte 3 debe tener 3 opciones', () => {
    const qInglesP3 = {
      modulo: "ingles",
      parte: 3,
      opciones: { A: "Yes, I do", B: "No, she isn't", C: "Tomorrow morning" }
    };
    const res = validateOptionsCount(qInglesP3, matrixConfig);
    assert.equal(res.valid, true);
  });
});

describe('3. Validador de Límite de Palabras en Lectura Crítica (<= 500 palabras)', () => {
  test('Texto de Lectura Crítica de 50 palabras es válido', () => {
    const text50Words = Array(50).fill('palabra').join(' ');
    const q = {
      modulo: "lectura_critica",
      texto_lectura: text50Words,
      enunciado: "¿Cuál es la idea principal?"
    };
    const res = validateReadingWordCount(q, 500);
    assert.equal(res.valid, true);
    assert.ok(res.wordCount <= 500);
  });

  test('Texto de Lectura Crítica de más de 500 palabras es rechazado', () => {
    const text600Words = Array(600).fill('evaluacion').join(' ');
    const q = {
      modulo: "lectura_critica",
      texto_lectura: text600Words,
      enunciado: "¿Qué concluye el autor?"
    };
    const res = validateReadingWordCount(q, 500);
    assert.equal(res.valid, false);
    assert.ok(res.wordCount > 500);
    assert.ok(res.error.includes('excede el límite'));
  });
});

describe('4. Validador de Cálculos en Razonamiento Cuantitativo', () => {
  test('Evaluador matemático seguro calcula expresiones correctamente', () => {
    assert.equal(safeEvaluateMathExpression('1200 / 8'), 150);
    assert.equal(safeEvaluateMathExpression('(30 + 25) * 10'), 550);
    assert.equal(safeEvaluateMathExpression('2^3 + 5'), 13);
  });

  test('Rechaza expresiones matemáticas que no coinciden con resultado_esperado', () => {
    const q = {
      modulo: "razonamiento_cuantitativo",
      calculo_verificable: {
        expresion: "1000 * 0.15",
        resultado_esperado: 200 // Incorrecto, debería ser 150
      }
    };
    const res = validateQuantitativeCalculations(q);
    assert.equal(res.valid, false);
    assert.ok(res.error.includes('Discrepancia en cálculo'));
  });

  test('Aprueba cuando el cálculo coincide con el resultado esperado', () => {
    const q = {
      modulo: "razonamiento_cuantitativo",
      calculo_verificable: {
        expresion: "3200000 * 0.55",
        resultado_esperado: 1760000
      }
    };
    const res = validateQuantitativeCalculations(q);
    assert.equal(res.valid, true);
    assert.ok(Math.abs(res.computed - 1760000) < 1e-4);
  });
});

describe('5. Validador de Deduplicación contra el Banco', () => {
  test('Detecta textos idénticos o con alta similitud', () => {
    const existing = [
      {
        id: "q_exist_1",
        enunciado: "Una empresa de transporte en Colombia evalúa la ruta entre Bogotá y Medellín con camiones de carga pesada."
      }
    ];

    const duplicateCandidate = {
      id: "q_new_1",
      enunciado: "Una empresa de transporte en Colombia evalúa la ruta entre Bogotá y Medellín con camiones de carga pesada para combustible."
    };

    const res = checkDeduplication(duplicateCandidate, existing, 0.65);
    assert.equal(res.isDuplicate, true);
    assert.equal(res.duplicateWithId, "q_exist_1");
  });

  test('Pregunta con temática distinta no se marca como duplicada', () => {
    const existing = [
      {
        id: "q_exist_1",
        enunciado: "Una empresa de transporte en Colombia evalúa la ruta entre Bogotá y Medellín."
      }
    ];

    const novelQuestion = {
      id: "q_new_2",
      enunciado: "El índice de precios al consumidor experimentó una variación del 0.4% en el sector de alimentos durante marzo."
    };

    const res = checkDeduplication(novelQuestion, existing, 0.65);
    assert.equal(res.isDuplicate, false);
  });
});

describe('6. Balance de Opciones A-D', () => {
  test('Calcula frecuencias y distribución equilibrada de respuestas', () => {
    const questions = [
      { respuesta_correcta: "A" },
      { respuesta_correcta: "B" },
      { respuesta_correcta: "C" },
      { respuesta_correcta: "D" },
      { respuesta_correcta: "A" },
      { respuesta_correcta: "B" },
      { respuesta_correcta: "C" },
      { respuesta_correcta: "D" }
    ];

    const balance = checkOptionKeyBalance(questions);
    assert.equal(balance.total, 8);
    assert.equal(balance.distribution.A.percentage, 25);
    assert.equal(balance.distribution.B.percentage, 25);
    assert.equal(balance.distribution.C.percentage, 25);
    assert.equal(balance.distribution.D.percentage, 25);
    assert.equal(balance.isBalanced, true);
  });
});

describe('7. Control de Distribución y Desviaciones de la Matriz Saber Pro 2026-2', () => {
  test('Calcula porcentajes y desviaciones por módulo y competencia', () => {
    const approvedQuestions = [
      { modulo: "razonamiento_cuantitativo", competencia_o_afirmacion: "interpretacion_representacion", estado_revision: "aprobada" },
      { modulo: "razonamiento_cuantitativo", competencia_o_afirmacion: "formulacion_ejecucion", estado_revision: "aprobada" },
      { modulo: "razonamiento_cuantitativo", competencia_o_afirmacion: "argumentacion", estado_revision: "aprobada" }
    ];

    const devResult = calculateDistributionDeviations(approvedQuestions, matrixConfig);
    assert.equal(devResult.totalAprobadas, 3);
    const rcReport = devResult.reportePorModulo.razonamiento_cuantitativo;
    assert.equal(rcReport.total, 3);
    assert.equal(rcReport.breakdown.length, 3);
  });
});

describe('8. Publicación Inmutable con Huella SHA-256 y Reportes Estudiantiles', () => {
  test('Publicar banco genera una huella SHA-256 inmutable consistente', () => {
    const storage = new BankStorage({ student_reports_threshold: 3 });
    storage.saveQuestion({
      id: "q_approved_test",
      modulo: "razonamiento_cuantitativo",
      competencia_o_afirmacion: "interpretacion_representacion",
      evidencia_asociada: "Evidencia",
      tipo_texto_o_contexto: "familiar",
      enunciado: "Pregunta aprobada para publicación inmutable.",
      opciones: { A: "1", B: "2", C: "3", D: "4" },
      respuesta_correcta: "A",
      explicacion: { por_que_correcta: "ok", por_que_fallan_distractores: "fallo" },
      dificultad: 1,
      generado_por_ia: true,
      modelo: "gemini-2.5-flash",
      version_del_prompt: "v2026.2.1",
      fecha: new Date().toISOString(),
      estado_revision: "aprobada",
      rotulo: "pregunta de práctica, no oficial"
    });
    const published = storage.publishBank("vTest-1.0.0", "admin@aciertapro.edu.co");

    assert.ok(published.sha256);
    assert.equal(published.sha256.length, 64, "SHA-256 debe tener 64 caracteres hex");
    assert.equal(published.version, "vTest-1.0.0");
    assert.ok(published.cantidad_preguntas >= 1);
  });

  test('Acumulación de N reportes de estudiantes traslada pregunta a "en_revision"', () => {
    const storage = new BankStorage({ student_reports_threshold: 3 });

    const testQId = "q_para_reportar_" + Date.now();
    const testQ = {
      id: testQId,
      modulo: "razonamiento_cuantitativo",
      enunciado: "Pregunta con posible error tipográfico en enunciado.",
      estado_revision: "aprobada"
    };
    storage.saveQuestion(testQ);

    // Enviar reportes simulados
    storage.addStudentReport(testQId, "Opción ambigua", "Detalle 1", "estudiante_1_" + Date.now());
    storage.addStudentReport(testQId, "Error de redacción", "Detalle 2", "estudiante_2_" + Date.now());
    const rep3 = storage.addStudentReport(testQId, "Respuesta incorrecta", "Detalle 3", "estudiante_3_" + Date.now());

    assert.equal(rep3.transitioned_to_review, true);

    const reloadedQ = storage.getQuestionById(testQId);
    assert.equal(reloadedQ.estado_revision, "en_revision");
    assert.ok(reloadedQ.motivo_en_revision.includes('umbral de 3 reportes'));
  });
});
