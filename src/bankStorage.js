/**
 * Capa de almacenamiento y ciclo de vida del banco de preguntas
 * Estados: borrador -> aprobada | rechazada | en_revision
 * Publicación inmutable con huella digital SHA-256
 * Gestión anónima de reportes estudiantiles con auto-retiro a "en_revision"
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const DATA_DIR = path.join(__dirname, '..', 'data');
const QUESTIONS_FILE = path.join(DATA_DIR, 'bank_questions.json');
const PUBLISHED_FILE = path.join(DATA_DIR, 'published_versions.json');
const REPORTS_FILE = path.join(DATA_DIR, 'reports.json');
const WRITING_TOPICS_FILE = path.join(DATA_DIR, 'writing_topics.json');

function ensureDataDir() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
}

function readJsonFile(filePath, defaultValue = []) {
  ensureDataDir();
  if (!fs.existsSync(filePath)) {
    fs.writeFileSync(filePath, JSON.stringify(defaultValue, null, 2), 'utf-8');
    return defaultValue;
  }
  try {
    const raw = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(raw);
  } catch (err) {
    console.error(`Error leyendo ${filePath}:`, err.message);
    return defaultValue;
  }
}

function writeJsonFile(filePath, data) {
  ensureDataDir();
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
}

class BankStorage {
  constructor(appConfig) {
    this.appConfig = appConfig;
    this.reportsThreshold = (appConfig && appConfig.student_reports_threshold) || 3;
    this.rateLimits = new Map(); // Para limitar frecuencia de reportes y ensayos
  }

  // --- PREGUNTAS ---

  getAllQuestions(filter = {}) {
    let questions = readJsonFile(QUESTIONS_FILE, []);
    if (filter.estado) {
      questions = questions.filter(q => q.estado_revision === filter.estado);
    }
    if (filter.modulo) {
      questions = questions.filter(q => q.modulo === filter.modulo);
    }
    return questions;
  }

  getQuestionById(id) {
    const questions = readJsonFile(QUESTIONS_FILE, []);
    return questions.find(q => q.id === id) || null;
  }

  saveQuestion(question) {
    const questions = readJsonFile(QUESTIONS_FILE, []);
    const existingIndex = questions.findIndex(q => q.id === question.id);
    if (existingIndex >= 0) {
      questions[existingIndex] = { ...questions[existingIndex], ...question };
    } else {
      questions.push(question);
    }
    writeJsonFile(QUESTIONS_FILE, questions);
    return question;
  }

  reviewQuestion(id, action, comment = '', reviewerEmail = '') {
    const questions = readJsonFile(QUESTIONS_FILE, []);
    const qIndex = questions.findIndex(q => q.id === id);
    if (qIndex === -1) {
      throw new Error(`Pregunta con ID "${id}" no encontrada`);
    }

    const q = questions[qIndex];
    const now = new Date().toISOString();

    if (action === 'aprobar') {
      q.estado_revision = 'aprobada';
      q.revisado_por = reviewerEmail;
      q.fecha_aprobacion = now;
      q.comentario_revision = comment;
    } else if (action === 'rechazar') {
      q.estado_revision = 'rechazada';
      q.revisado_por = reviewerEmail;
      q.fecha_rechazo = now;
      q.comentario_revision = comment;
    } else if (action === 'editar') {
      q.editado_por = reviewerEmail;
      q.fecha_edicion = now;
      if (comment) q.comentario_revision = comment;
    } else {
      throw new Error(`Acción de revisión desconocida: "${action}"`);
    }

    questions[qIndex] = q;
    writeJsonFile(QUESTIONS_FILE, questions);
    return q;
  }

  // --- PUBLICACIÓN INMUTABLE Y HASH SHA-256 ---

  publishBank(versionTag, publisherEmail = '') {
    const questions = readJsonFile(QUESTIONS_FILE, []);
    // Solo preguntas con estado 'aprobada' entran al banco publicado
    const approvedQuestions = questions.filter(q => q.estado_revision === 'aprobada');

    if (approvedQuestions.length === 0) {
      throw new Error('No hay preguntas con estado "aprobada" para publicar');
    }

    // Ordenar preguntas por id para consistencia y determinismo en el hash
    const canonicalQuestions = [...approvedQuestions].sort((a, b) => a.id.localeCompare(b.id));

    const generatedAt = new Date().toISOString();
    const payloadForHash = JSON.stringify({
      version: versionTag,
      generated_at: generatedAt,
      questions: canonicalQuestions
    });

    const sha256 = crypto.createHash('sha256').update(payloadForHash).digest('hex');

    const publishedVersion = {
      version: versionTag,
      generated_at: generatedAt,
      sha256,
      publicado_por: publisherEmail,
      cantidad_preguntas: canonicalQuestions.length,
      questions: canonicalQuestions
    };

    const history = readJsonFile(PUBLISHED_FILE, []);
    history.unshift(publishedVersion); // La más reciente al inicio
    writeJsonFile(PUBLISHED_FILE, history);

    return publishedVersion;
  }

  getLatestPublishedVersion() {
    const history = readJsonFile(PUBLISHED_FILE, []);
    return history[0] || null;
  }

  getPublicBankLatest() {
    const latest = this.getLatestPublishedVersion();
    if (!latest) {
      return {
        version: "sin_publicar",
        generated_at: new Date().toISOString(),
        sha256: null,
        preguntas: []
      };
    }

    // Limpieza de campos internos para lectura pública
    const publicQuestions = latest.questions.map(q => ({
      id: q.id,
      modulo: q.modulo,
      competencia_o_afirmacion: q.competencia_o_afirmacion,
      evidencia_asociada: q.evidencia_asociada,
      tipo_texto_o_contexto: q.tipo_texto_o_contexto,
      texto_lectura: q.texto_lectura || null,
      enunciado: q.enunciado,
      opciones: q.opciones,
      respuesta_correcta: q.respuesta_correcta,
      explicacion: q.explicacion,
      dificultad: q.dificultad,
      rotulo: q.rotulo
    }));

    return {
      version: latest.version,
      generated_at: latest.generated_at,
      sha256: latest.sha256,
      preguntas: publicQuestions
    };
  }

  getPublicBankVersion() {
    const latest = this.getLatestPublishedVersion();
    return {
      version: latest ? latest.version : "sin_publicar",
      generated_at: latest ? latest.generated_at : null,
      sha256: latest ? latest.sha256 : null,
      total_preguntas_aprobadas: latest ? latest.cantidad_preguntas : 0
    };
  }

  // --- REPORTES DE ESTUDIANTES (ANÓNIMOS, CON RATE LIMIT) ---

  checkRateLimit(key, maxRequests = 10, windowMs = 60000) {
    const now = Date.now();
    const record = this.rateLimits.get(key) || { count: 0, resetAt: now + windowMs };
    if (now > record.resetAt) {
      record.count = 1;
      record.resetAt = now + windowMs;
      this.rateLimits.set(key, record);
      return true;
    }
    if (record.count >= maxRequests) {
      return false;
    }
    record.count++;
    this.rateLimits.set(key, record);
    return true;
  }

  addStudentReport(questionId, reason, details = '', clientIdentifier = 'anon') {
    // 1. Rate limiting
    if (!this.checkRateLimit(`report_${clientIdentifier}`, 5, 60000)) {
      throw new Error('Límite de frecuencia excedido. Intente más tarde.');
    }

    if (!questionId || !reason) {
      throw new Error('question_id y reason son obligatorios para reportar');
    }

    // 2. Registrar reporte (estrictamente anónimo, sin almacenar IP ni datos personales)
    const reports = readJsonFile(REPORTS_FILE, []);
    const reportEntry = {
      report_id: crypto.randomUUID(),
      question_id: questionId,
      reason,
      details,
      created_at: new Date().toISOString()
    };
    reports.push(reportEntry);
    writeJsonFile(REPORTS_FILE, reports);

    // 3. Contar reportes para esta pregunta
    const questionReportsCount = reports.filter(r => r.question_id === questionId).length;

    // 4. Si alcanza el umbral, pasar a 'en_revision' y sacarla del banco
    let transitionedToReview = false;
    if (questionReportsCount >= this.reportsThreshold) {
      const questions = readJsonFile(QUESTIONS_FILE, []);
      const qIndex = questions.findIndex(q => q.id === questionId);
      if (qIndex !== -1 && questions[qIndex].estado_revision === 'aprobada') {
        questions[qIndex].estado_revision = 'en_revision';
        questions[qIndex].motivo_en_revision = `Alcanzó el umbral de ${this.reportsThreshold} reportes de estudiantes`;
        questions[qIndex].reportes_recibidos = questionReportsCount;
        writeJsonFile(QUESTIONS_FILE, questions);
        transitionedToReview = true;
      }
    }

    return {
      success: true,
      report_id: reportEntry.report_id,
      total_reports_for_question: questionReportsCount,
      transitioned_to_review: transitionedToReview
    };
  }

  getReportsForQuestion(questionId) {
    const reports = readJsonFile(REPORTS_FILE, []);
    return reports.filter(r => r.question_id === questionId);
  }

  // --- COMUNICACIÓN ESCRITA (TEMAS DE DOMINIO PÚBLICO) ---

  getWritingTopics() {
    return readJsonFile(WRITING_TOPICS_FILE, [
      {
        id: "tema_ce_01",
        titulo: "Inteligencia Artificial en la Educación Superior",
        afirmacion: "¿Está de acuerdo con la afirmación: 'El uso no regulado de herramientas de IA generativa por parte de estudiantes universitarios deteriora el pensamiento crítico y la capacidad argumentativa autónoma'?",
        contexto: "El auge de modelos de lenguaje en tareas académicas ha generado debate sobre el plagio, la formación del criterio propio y la necesidad de nuevas formas de evaluación.",
        posturas_posibles: ["De acuerdo", "En desacuerdo", "Parcialmente"],
        criterios: ["Planteamiento", "Organización", "Forma de expresión", "Pertinencia"],
        creado_en: new Date().toISOString()
      },
      {
        id: "tema_ce_02",
        titulo: "Transición Energética y Desarrollo Económico en Colombia",
        afirmacion: "¿Está de acuerdo con la afirmación: 'Colombia debe suspender de manera inmediata la exploración de nuevos yacimientos de hidrocarburos como medida indispensable para liderar la acción climática global, aun si esto impacta las finanzas públicas'?",
        contexto: "Colombia aporta un porcentaje menor de emisiones globales pero depende significativamente de regalías petroleras y de carbón para su presupuesto.",
        posturas_posibles: ["De acuerdo", "En desacuerdo", "Parcialmente"],
        criterios: ["Planteamiento", "Organización", "Forma de expresión", "Pertinencia"],
        creado_en: new Date().toISOString()
      }
    ]);
  }

  addWritingTopic(topic) {
    const topics = this.getWritingTopics();
    const newTopic = {
      id: topic.id || `tema_ce_${Date.now()}`,
      titulo: topic.titulo,
      afirmacion: topic.afirmacion.startsWith('¿Está de acuerdo con') ? topic.afirmacion : `¿Está de acuerdo con la afirmación: '${topic.afirmacion}'?`,
      contexto: topic.contexto || '',
      posturas_posibles: ["De acuerdo", "En desacuerdo", "Parcialmente"],
      criterios: ["Planteamiento", "Organización", "Forma de expresión", "Pertinencia"],
      creado_en: new Date().toISOString()
    };
    topics.push(newTopic);
    writeJsonFile(WRITING_TOPICS_FILE, topics);
    return newTopic;
  }
}

module.exports = BankStorage;
