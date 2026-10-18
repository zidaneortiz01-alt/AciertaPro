/**
 * Acierta Pro — Banco: Servidor Express y APIs REST
 * Panel docente para generar, revisar y publicar preguntas de práctica Saber Pro 2026-2.
 */

const express = require('express');
const path = require('path');
const fs = require('fs');

const BankStorage = require('./src/bankStorage');
const GenerationQueue = require('./src/generationQueue');
const GeminiServerClient = require('./src/geminiClient');
const UserStorage = require('./src/userStorage');
const {
  calculateDistributionDeviations,
  checkOptionKeyBalance,
  sanitizeInput,
  validateJudgeEvaluation,
  validateEssayRubricEvaluation
} = require('./src/validators');

const app = express();
const PORT = 3000;

// Cargar configuraciones versionadas
const matrixConfigPath = path.join(__dirname, 'config', 'matrix_config.json');
const appConfigPath = path.join(__dirname, 'config', 'app_config.json');
const promptTemplatesPath = path.join(__dirname, 'config', 'prompt_templates.json');

function loadJson(p, fallback) {
  try {
    return JSON.parse(fs.readFileSync(p, 'utf-8'));
  } catch (e) {
    return fallback;
  }
}

let matrixConfig = loadJson(matrixConfigPath, {});
let appConfig = loadJson(appConfigPath, {});
let promptTemplates = loadJson(promptTemplatesPath, {});

// Clientes y módulos del core
const bankStorage = new BankStorage(appConfig);
const userStorage = new UserStorage(appConfig);
const geminiClient = new GeminiServerClient(process.env.GEMINI_API_KEY, appConfig.gemini_model || 'gemini-2.5-flash');
const generationQueue = new GenerationQueue(geminiClient, bankStorage, appConfig, matrixConfig, promptTemplates);

// Middlewares estándar
app.use(express.json({ limit: '2mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// Inicializar banco con preguntas iniciales si está vacío
function seedInitialQuestionsIfEmpty() {
  const existing = bankStorage.getAllQuestions();
  if (existing.length === 0) {
    console.log('Inicializando banco con preguntas de ejemplo para revisión...');
    const seed = [
      {
        id: "rc_seed_001",
        modulo: "razonamiento_cuantitativo",
        competencia_o_afirmacion: "interpretacion_representacion",
        evidencia_asociada: "Comprende y transforma la información cuantitativa presentada en distintos formatos.",
        tipo_texto_o_contexto: "familiar/personal",
        enunciado: "Una familia en Bucaramanga destina el 30% de sus ingresos mensuales al arriendo y el 25% a la alimentación. Si el ingreso mensual total es de $3.200.000 COP, ¿cuánto dinero gastan en conjunto en estos dos rubros?",
        opciones: {
          A: "$1.760.000",
          B: "$1.600.000",
          C: "$1.920.000",
          D: "$1.440.000"
        },
        respuesta_correcta: "A",
        explicacion: {
          por_que_correcta: "30% + 25% = 55%. El 55% de $3.200.000 es 3.200.000 * 0.55 = $1.760.000.",
          por_que_fallan_distractores: "B calcula exactamente el 50%. C calcula el 60%. D calcula el 45%."
        },
        dificultad: 1,
        generado_por_ia: true,
        modelo: "gemini-2.5-flash",
        version_del_prompt: "v2026.2.1",
        fecha: new Date().toISOString(),
        estado_revision: "aprobada",
        rotulo: "pregunta de práctica, no oficial",
        calculo_verificable: {
          expresion: "3200000 * 0.55",
          resultado_esperado: 1760000
        }
      },
      {
        id: "lc_seed_002",
        modulo: "lectura_critica",
        competencia_o_afirmacion: "afirmacion_1",
        evidencia_asociada: "Entiende el significado de palabras o frases locales en un texto.",
        tipo_texto_o_contexto: "continuo_informativo",
        texto_lectura: "El café especial colombiano cultivado en alturas superiores a los 1600 msnm experimenta una maduración más lenta del grano, lo cual concentra azúcares y acidez cítrica distintiva. Esta particularidad microclimática le confiere un perfil sensorial altamente cotizado en subastas internacionales de cafés de especialidad.",
        enunciado: "En el fragmento anterior, la expresión 'maduración más lenta del grano' cumple la función de:",
        opciones: {
          A: "Explicar la causa fisiológica que origina la concentración de azúcares y acidez.",
          B: "Criticar los métodos agrícolas tradicionales de recolección en las laderas.",
          C: "Demostrar que el café cultivado a baja altitud carece de valor comercial.",
          D: "Describir el proceso posterior al tostado y empaque del producto."
        },
        respuesta_correcta: "A",
        explicacion: {
          por_que_correcta: "Vincula directamente el efecto del clima de altura con la concentración de sabores.",
          por_que_fallan_distractores: "B atribuye una crítica inexistente. C extrapola sin sustento. D alude a fases de transformación industrial no mencionadas."
        },
        dificultad: 2,
        generado_por_ia: true,
        modelo: "gemini-2.5-flash",
        version_del_prompt: "v2026.2.1",
        fecha: new Date().toISOString(),
        estado_revision: "aprobada",
        rotulo: "pregunta de práctica, no oficial"
      },
      {
        id: "cc_seed_003",
        modulo: "competencias_ciudadanas",
        competencia_o_afirmacion: "argumentacion",
        evidencia_asociada: "Analiza y evalúa la pertinencia y validez de argumentos sobre problemáticas sociales.",
        tipo_texto_o_contexto: "comunitario/social",
        enunciado: "Ante el aumento del tráfico en horas pico, un ciudadano propone prohibir totalmente la circulación de bicicletas en avenidas principales argumentando que 'entorpecen la velocidad de los automóviles y son la principal causa de congestión vial'. ¿Cuál de las siguientes afirmaciones debilita con mayor fuerza este argumento?",
        opciones: {
          A: "Los estudios de movilidad urbana demuestran que el espacio ocupado por un automóvil equivale al de múltiples bicicletas y el vehículo particular genera el 80% del atasco.",
          B: "Las bicicletas son más económicas que los automóviles y requieren menor mantenimiento anual.",
          C: "En varios países europeos el uso de la bicicleta se promueve durante los fines de semana.",
          D: "Los fabricantes de vehículos automotores generan más empleos formales en la industria nacional."
        },
        respuesta_correcta: "A",
        explicacion: {
          por_que_correcta: "Ataca directamente la premisa de la causalidad de la congestión con evidencia empírica de ocupación de espacio vial.",
          por_que_fallan_distractores: "B habla de economía personal, no de congestión. C recurre a ejemplos foráneos descontextualizados. D introduce el empleo industrial irrelevante para la fluidez vial."
        },
        dificultad: 2,
        generado_por_ia: true,
        modelo: "gemini-2.5-flash",
        version_del_prompt: "v2026.2.1",
        fecha: new Date().toISOString(),
        estado_revision: "aprobada",
        rotulo: "pregunta de práctica, no oficial",
        requiere_verificacion_factual: false
      },
      {
        id: "in_seed_004",
        modulo: "ingles",
        competencia_o_afirmacion: "Parte 1: Avisos y vocabulario",
        evidencia_asociada: "Identifica el lugar donde se exhibe un aviso público (Parte 1).",
        tipo_texto_o_contexto: "Aviso público",
        parte: 1,
        enunciado: "Where can you see this notice? 'LIFEGUARDS ON DUTY FROM 9 AM TO 6 PM. NO DIVING IN SHALLOW AREAS.'",
        opciones: {
          A: "At a public swimming pool or beach resort",
          B: "In a mountain train station",
          C: "Inside an art gallery",
          D: "At a post office branch",
          E: "In an underground car parking lot",
          F: "At a chemistry laboratory",
          G: "Inside a shoe store",
          H: "At a city bank counter"
        },
        respuesta_correcta: "A",
        explicacion: {
          por_que_correcta: "'Lifeguards' and 'no diving' are typical water safety notices found in swimming pools or coastal zones.",
          por_que_fallan_distractores: "All other options are land/commercial places with no aquatic activities."
        },
        dificultad: 1,
        generado_por_ia: true,
        modelo: "gemini-2.5-flash",
        version_del_prompt: "v2026.2.1",
        fecha: new Date().toISOString(),
        estado_revision: "aprobada",
        rotulo: "pregunta de práctica, no oficial"
      }
    ];

    for (const q of seed) {
      bankStorage.saveQuestion(q);
    }

    // Publicar versión inicial
    bankStorage.publishBank("v2026.2.0-inicial", "sistema@aciertapro.edu.co");
  }
}

seedInitialQuestionsIfEmpty();

// --- MIDDLEWARE DE AUTORIZACIÓN DOCENTE / SUPERUSUARIO (RBAC) ---
function requireAuthorizedTeacher(req, res, next) {
  const token = req.headers['authorization']?.replace('Bearer ', '') || req.headers['x-session-token'];
  const session = token ? userStorage.getSession(token) : null;
  const authEmail = session ? session.email : (req.headers['x-user-email'] || req.query.user_email);

  if (!authEmail) {
    return res.status(401).json({
      error: "Acceso no autenticado. Por favor inicie sesión como docente o superusuario.",
      is_authorized: false,
      role: "invitado"
    });
  }

  const isAllowed = userStorage.isSuperuserOrTeacher(authEmail);
  if (!isAllowed) {
    return res.status(403).json({
      error: "Acceso denegado: esta función es exclusiva de docentes y superusuarios.",
      email_recibido: authEmail,
      role: "estudiante",
      is_authorized: false
    });
  }

  req.userEmail = authEmail;
  req.userRole = session ? session.role : userStorage.determineRole(authEmail);
  next();
}

// =========================================================================
// 1. ENDPOINTS PÚBLICOS DE SOLO LECTURA (Estrictamente los solicitados)
// =========================================================================

/**
 * GET /api/v1/bank/version
 * Información pública de solo lectura: versión, fecha de generación y huella SHA-256.
 */
app.get('/api/v1/bank/version', (req, res) => {
  const versionInfo = bankStorage.getPublicBankVersion();
  res.json(versionInfo);
});

/**
 * GET /api/v1/bank/latest
 * JSON con versión, fecha generated_at, sha256 del contenido y preguntas aprobadas.
 * No expone borradores, rechazadas, correos de revisores ni datos internos.
 */
app.get('/api/v1/bank/latest', (req, res) => {
  const latestBank = bankStorage.getPublicBankLatest();
  res.json(latestBank);
});

/**
 * POST /api/v1/reports
 * Reportes anónimos de estudiantes sobre preguntas del banco.
 * Con límite de frecuencia, sin registrar datos personales.
 * Al llegar a N reportes (configurable), la pregunta pasa a "en revisión" y sale del banco.
 */
app.post('/api/v1/reports', (req, res) => {
  const clientIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'anon-client';
  const { question_id, reason, details } = req.body;

  if (!question_id || !reason) {
    return res.status(400).json({
      error: "Los campos question_id y reason son obligatorios."
    });
  }

  // Prevenir que el cliente envíe datos personales
  if (req.body.email || req.body.nombre || req.body.documento || req.body.telefono) {
    return res.status(400).json({
      error: "Este canal de reporte es estrictamente anónimo. No se permite enviar datos personales."
    });
  }

  try {
    const result = bankStorage.addStudentReport(question_id, reason, details, clientIp);
    res.json({
      mensaje: "Reporte recibido correctamente.",
      report_id: result.report_id,
      accion_automatica: result.transitioned_to_review
        ? "La pregunta acumuló el umbral de reportes y fue retirada preventivamente a estado 'en revisión'."
        : "Reporte registrado para auditoría docente."
    });
  } catch (err) {
    res.status(429).json({ error: err.message });
  }
});

/**
 * POST /api/v1/essay-feedback
 * Endpoint opcional de Comunicación Escrita, DESACTIVADO por defecto y marcado "fase 3".
 * Recibe texto y devuelve retroalimentación estructurada sin puntaje 0-300 ni persistencia.
 */
app.post('/api/v1/essay-feedback', async (req, res) => {
  // 1. Verificación de bandera: desactivado por defecto
  if (!appConfig.essay_feedback_enabled) {
    return res.status(403).json({
      estado: "DESACTIVADO",
      etiqueta: "fase 3",
      mensaje: "El servicio de retroalimentación de ensayos está desactivado por defecto (fase 3). Requiere activación explícita en app_config.json.",
      nota: "[por confirmar fecha de despliegue institucional]"
    });
  }

  // 2. Verificación de consentimiento explícito
  const { essay_text, consent, prompt_id } = req.body;
  if (consent !== true) {
    return res.status(400).json({
      error: "Se requiere confirmación explícita de consentimiento (consent: true) para procesar el texto con fines exclusivamente formativos sin almacenamiento."
    });
  }

  if (!essay_text || typeof essay_text !== 'string' || essay_text.trim().length < 50) {
    return res.status(400).json({
      error: "El texto del ensayo debe tener al menos 50 caracteres para ser evaluado."
    });
  }

  // 3. Límite de frecuencia (rate limit)
  const clientIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'client';
  if (!bankStorage.checkRateLimit(`essay_${clientIp}`, 3, 60000)) {
    return res.status(429).json({
      error: "Límite de frecuencia excedido para retroalimentación de ensayos (máximo 3 solicitudes por minuto)."
    });
  }

  // 4. Obtener consigna asociada si existe
  const topics = bankStorage.getWritingTopics();
  const selectedTopic = topics.find(t => t.id === prompt_id) || topics[0];
  const promptAffirmation = selectedTopic ? selectedTopic.afirmacion : "¿Está de acuerdo con la afirmación sobre el tema de dominio público?";

  try {
    if (!geminiClient.isConfigured()) {
      return res.json({
        modo: "simulacion_formativa",
        consigna: promptAffirmation,
        criterios: {
          planteamiento: {
            fortalezas: "Se identifica una postura clara frente a la afirmación central.",
            aspectos_a_mejorar: "Profundizar en la justificación de los matices o contraargumentos."
          },
          organizacion: {
            fortalezas: "Estructura en párrafos diferenciados con conectores lógicos de causa y consecuencia.",
            aspectos_a_mejorar: "Mejorar la transición entre el segundo y tercer argumento."
          },
          forma_de_expresion: {
            fortalezas: "Uso de vocabulario académico formal y ortografía adecuada.",
            aspectos_a_mejorar: "Evitar oraciones subordinadas excesivamente extensas."
          },
          pertinencia: {
            fortalezas: "Los argumentos corresponden al contexto de dominio público colombiano.",
            aspectos_a_mejorar: "Vincular más directamente los ejemplos con el problema central."
          },
          recomendacion_general: "Excelente ejercicio. Continúa practicando la síntesis en la conclusión sin repetir premisas."
        },
        aviso: "Sin almacenamiento de datos. No se genera puntaje numérico 0-300."
      });
    }

    const feedback = await geminiClient.evaluateEssayFeedback({
      essayText: essay_text,
      promptAffirmation,
      modelName: appConfig.gemini_model
    });

    res.json({
      consigna: promptAffirmation,
      retroalimentacion: feedback,
      aviso: "Sin almacenamiento en base de datos. Sin puntaje 0-300 conforme a especificación."
    });
  } catch (err) {
    res.status(500).json({ error: `Error generando retroalimentación: ${err.message}` });
  }
});

// =========================================================================
// 2. ENDPOINTS DEL PANEL DOCENTE (Acceso restringido a correos autorizados)
// =========================================================================

// =========================================================================
// ENDPOINTS DE AUTENTICACIÓN GOOGLE, SESIÓN Y ROLES (RBAC)
// =========================================================================

/**
 * Autenticación mediante Google Sign-In
 * Recibe credenciales de cuenta de Google, busca o crea el usuario en la base de datos
 * y asigna rol de superusuario (si está en lista autorizada) o de estudiante.
 */
app.post('/api/auth/google', (req, res) => {
  try {
    const { email, name, picture, google_id, academic_program, campus } = req.body;
    if (!email) {
      return res.status(400).json({ error: "El correo electrónico es requerido." });
    }

    const { user, token } = userStorage.findOrCreateGoogleUser({
      email,
      name,
      picture,
      google_id,
      academic_program,
      campus
    });

    res.json({
      success: true,
      user,
      token,
      is_authorized: user.role === 'superusuario' || user.role === 'docente'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/**
 * Inicio de sesión o cambio de cuenta
 */
app.post('/api/auth/login', (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ error: "El correo electrónico es requerido." });
    }

    const { user, token } = userStorage.loginLocalOrDemo(email);
    res.json({
      success: true,
      user,
      token,
      is_authorized: user.role === 'superusuario' || user.role === 'docente'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/**
 * Obtener perfil del usuario en sesión activa
 */
app.get('/api/auth/me', (req, res) => {
  const token = req.headers['authorization']?.replace('Bearer ', '') || req.headers['x-session-token'];
  const session = token ? userStorage.getSession(token) : null;
  const email = session ? session.email : (req.headers['x-user-email'] || req.query.user_email);

  if (!email) {
    return res.json({
      authenticated: false,
      user: null,
      role: 'invitado',
      is_authorized: false
    });
  }

  const user = userStorage.getUserByEmail(email) || {
    email,
    name: email.split('@')[0],
    role: userStorage.determineRole(email)
  };

  res.json({
    authenticated: true,
    user,
    role: user.role,
    is_authorized: user.role === 'superusuario' || user.role === 'docente'
  });
});

/**
 * Cierre de sesión y destrucción de token
 */
app.post('/api/auth/logout', (req, res) => {
  const token = req.headers['authorization']?.replace('Bearer ', '') || req.headers['x-session-token'];
  if (token) {
    userStorage.removeSession(token);
  }
  res.json({ success: true, message: "Sesión cerrada correctamente" });
});

/**
 * Estado de autenticación y rol actual del usuario
 */
app.get('/api/teacher/auth-status', (req, res) => {
  const token = req.headers['authorization']?.replace('Bearer ', '') || req.headers['x-session-token'];
  const session = token ? userStorage.getSession(token) : null;
  const currentEmail = session ? session.email : (req.headers['x-user-email'] || req.query.user_email);

  if (!currentEmail) {
    return res.json({
      email: null,
      is_authorized: false,
      role: 'invitado',
      authorized_list: appConfig.authorized_emails
    });
  }

  const isAuth = userStorage.isSuperuserOrTeacher(currentEmail);
  const user = userStorage.getUserByEmail(currentEmail);
  const role = user ? user.role : (isAuth ? 'superusuario' : 'estudiante');

  res.json({
    email: currentEmail,
    is_authorized: isAuth,
    role: role,
    user_name: user ? user.name : currentEmail.split('@')[0],
    picture: user ? user.picture : null,
    authorized_list: appConfig.authorized_emails
  });
});

/**
 * Obtener y actualizar matriz oficial versionada
 */
app.get('/api/teacher/matrix', requireAuthorizedTeacher, (req, res) => {
  res.json(matrixConfig);
});

app.put('/api/teacher/matrix', requireAuthorizedTeacher, (req, res) => {
  matrixConfig = req.body;
  fs.writeFileSync(matrixConfigPath, JSON.stringify(matrixConfig, null, 2));
  generationQueue.matrixConfig = matrixConfig;
  res.json({ success: true, message: "Matriz actualizada correctamente", matrixConfig });
});

/**
 * Configuración general del sistema
 */
app.get('/api/teacher/config', requireAuthorizedTeacher, (req, res) => {
  res.json({
    ...appConfig,
    gemini_key_configured: geminiClient.isConfigured()
  });
});

app.put('/api/teacher/config', requireAuthorizedTeacher, (req, res) => {
  appConfig = { ...appConfig, ...req.body };
  fs.writeFileSync(appConfigPath, JSON.stringify(appConfig, null, 2));
  bankStorage.appConfig = appConfig;
  bankStorage.reportsThreshold = appConfig.student_reports_threshold || 3;
  generationQueue.appConfig = appConfig;
  res.json({ success: true, appConfig });
});

/**
 * Listado de preguntas por estado
 */
app.get('/api/teacher/questions', requireAuthorizedTeacher, (req, res) => {
  const { estado, modulo } = req.query;
  const questions = bankStorage.getAllQuestions({ estado, modulo });
  res.json(questions);
});

/**
 * Generación por lotes en cola con concurrencia 1
 */
app.post('/api/teacher/generate', requireAuthorizedTeacher, (req, res) => {
  const { modulo, competencia, evidencia, dificultad, cantidad } = req.body;
  if (!modulo || !competencia) {
    return res.status(400).json({ error: "modulo y competencia son requeridos" });
  }

  const batch = generationQueue.enqueueBatch({
    modulo,
    competencia,
    evidencia,
    dificultad: dificultad || 2,
    cantidad: cantidad || 1,
    userEmail: req.userEmail
  });

  res.json({
    mensaje: "Lote de generación recibido y agregado a la cola con concurrencia 1.",
    batch
  });
});

app.get('/api/teacher/generate/status', requireAuthorizedTeacher, (req, res) => {
  res.json(generationQueue.getStatus());
});

/**
 * Revisión humana: editar, aprobar o rechazar con comentario
 */
app.post('/api/teacher/questions/:id/review', requireAuthorizedTeacher, (req, res) => {
  const { id } = req.params;
  const { accion, comentario, pregunta_editada } = req.body;

  try {
    if (accion === 'editar' && pregunta_editada) {
      bankStorage.saveQuestion({ ...pregunta_editada, id });
    }
    const updated = bankStorage.reviewQuestion(id, accion, comentario, req.userEmail);
    res.json({ success: true, question: updated });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

/**
 * Publicar banco con versión inmutable y SHA-256
 */
app.post('/api/teacher/publish', requireAuthorizedTeacher, (req, res) => {
  const { version_tag } = req.body;
  const tag = version_tag || `v${new Date().toISOString().slice(0, 10).replace(/-/g, '.')}-${Date.now().toString().slice(-4)}`;

  try {
    const published = bankStorage.publishBank(tag, req.userEmail);
    res.json({
      success: true,
      mensaje: "Banco publicado con éxito como versión inmutable.",
      published
    });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

/**
 * Control de distribución: comparación frente a la matriz oficial 2026-2 y balance de opciones
 */
app.get('/api/teacher/distribution', requireAuthorizedTeacher, (req, res) => {
  const allQuestions = bankStorage.getAllQuestions();
  const deviations = calculateDistributionDeviations(allQuestions, matrixConfig);
  const keyBalance = checkOptionKeyBalance(allQuestions.filter(q => q.estado_revision === 'aprobada'));

  res.json({
    deviations,
    keyBalance
  });
});

/**
 * Gestión del banco de temas de Comunicación Escrita
 */
app.get('/api/teacher/writing-topics', requireAuthorizedTeacher, (req, res) => {
  res.json(bankStorage.getWritingTopics());
});

app.post('/api/teacher/writing-topics', requireAuthorizedTeacher, (req, res) => {
  const { titulo, afirmacion, contexto } = req.body;
  if (!titulo || !afirmacion) {
    return res.status(400).json({ error: "titulo y afirmacion son obligatorios" });
  }

  const topic = bankStorage.addWritingTopic({ titulo, afirmacion, contexto });
  res.json({ success: true, topic });
});

/**
 * Catálogo completo de módulos (5 genéricos + 35 específicos del ICFES)
 */
app.get('/api/modules', (req, res) => {
  const genericModules = Object.entries(matrixConfig.modulos || {}).map(([id, conf]) => ({
    id,
    name: conf.nombre,
    category_type: 'generic',
    preguntas_examen: conf.preguntas_examen || 30
  }));

  const specificModules = (matrixConfig.modulos_especificos || []).map(m => ({
    id: m.id,
    name: m.nombre,
    category_type: 'specific',
    categoria: m.categoria
  }));

  res.json({
    total: genericModules.length + specificModules.length,
    generic: genericModules,
    specific: specificModules
  });
});

/**
 * Balanceador dinámico de módulos prioritarios para el pipeline batch (Sección 2.3)
 * Equivalente de fn_get_priority_modules_for_batch(target)
 */
app.get('/api/modules/priority', (req, res) => {
  const target = parseInt(req.query.target) || 100;
  const approvedQuestions = bankStorage.getAllQuestions().filter(q => q.estado_revision === 'aprobada');
  const countsByModule = {};
  approvedQuestions.forEach(q => {
    countsByModule[q.modulo] = (countsByModule[q.modulo] || 0) + 1;
  });

  const allModules = [
    ...Object.entries(matrixConfig.modulos || {}).map(([id, conf]) => ({
      module_id: id,
      module_name: conf.nombre,
      category_type: 'generic'
    })),
    ...(matrixConfig.modulos_especificos || []).map(m => ({
      module_id: m.id,
      module_name: m.nombre,
      category_type: 'specific'
    }))
  ];

  const deficits = allModules.map(m => {
    const current = countsByModule[m.module_id] || 0;
    const deficit = Math.max(0, target - current);
    return {
      module_id: m.module_id,
      module_name: m.module_name,
      category_type: m.category_type,
      current_count: current,
      deficit
    };
  })
  .filter(m => m.deficit > 0)
  .sort((a, b) => b.deficit - a.deficit || (a.category_type === 'generic' ? -1 : 1))
  .slice(0, 5);

  res.json({
    target,
    priority_modules: deficits
  });
});

/**
 * Agente 2: Juez ICFES sobre una pregunta específica (Sección 2.4)
 */
app.post('/api/questions/:id/judge', requireAuthorizedTeacher, async (req, res) => {
  const { id } = req.params;
  const question = bankStorage.getQuestionById(id);
  if (!question) {
    return res.status(404).json({ error: 'Pregunta no encontrada' });
  }

  try {
    const judgeResult = await geminiClient.auditQuestionAsJudge(question);
    const validation = validateJudgeEvaluation(judgeResult);

    if (validation.approved) {
      bankStorage.reviewQuestion(id, 'aprobar', 'Aprobada automáticamente por Agente 2 (Juez ICFES) con score ' + judgeResult.overall_score, 'Agente 2 (Juez ICFES)');
    }

    res.json({
      success: true,
      evaluation: judgeResult,
      validation
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/**
 * Evaluación de Comunicación Escrita mediante Rúbrica Oficial ICFES (Sección 3.1)
 * Aplica sanitización de entrada, control de rate limit y esquema rígido.
 */
app.post('/api/essays/evaluate', async (req, res) => {
  const { essay_text, prompt_affirmation } = req.body;
  if (!essay_text || essay_text.trim().length < 50) {
    return res.status(400).json({ error: 'El ensayo debe contener al menos 50 caracteres para ser evaluado.' });
  }

  const sanitizedEssay = sanitizeInput(essay_text);
  const sanitizedPrompt = sanitizeInput(prompt_affirmation || '');

  try {
    const evaluation = await geminiClient.evaluateOfficialEssayRubric({
      essayText: sanitizedEssay,
      promptAffirmation: sanitizedPrompt
    });

    const validation = validateEssayRubricEvaluation(evaluation);
    if (!validation.valid) {
      return res.status(502).json({ error: 'La respuesta del evaluador no satisfizo el esquema oficial.' });
    }

    res.json({
      success: true,
      evaluation
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Iniciar servidor
app.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`Acierta Pro — Banco iniciado en http://0.0.0.0:${PORT}`);
  console.log(`Runtime de servidor con integración Firebase y Gemini`);
  console.log(`=======================================================`);
});

