/**
 * Acierta Pro — Banco: Lógica del Panel Docente
 */

let currentUserEmail = 'proyectoing2026@gmail.com';
let matrixConfigCache = null;
let appConfigCache = null;

// Inicialización
document.addEventListener('DOMContentLoaded', () => {
  setupTabs();
  setupModuleChange();
  setupFormGeneracion();
  setupPublishModal();
  setupEssayFeedback();
  setupStudentReportsSimulation();
  setupConfigForms();
  setupUserSwitch();

  // Cargar datos iniciales
  loadMatrixAndInitDropdowns();
  loadQueueStatus();
  loadDrafts();
  loadPublishedBank();
  loadDistribution();
  loadWritingTopics();
  loadConfig();

  // Polling de cola cada 3 segundos
  setInterval(loadQueueStatus, 3000);
});

// GESTIÓN DE PESTAÑAS
function setupTabs() {
  const tabs = document.querySelectorAll('.nav-tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));

      tab.classList.add('active');
      const targetId = tab.getAttribute('data-tab');
      const panel = document.getElementById(targetId);
      if (panel) panel.classList.add('active');

      if (targetId === 'tab-review') loadDrafts();
      if (targetId === 'tab-published') loadPublishedBank();
      if (targetId === 'tab-distribution') loadDistribution();
      if (targetId === 'tab-writing') loadWritingTopics();
      if (targetId === 'tab-reports') loadUnderReviewQuestions();
    });
  });
}

// CAMBIO DE CORREO DOCENTE
function setupUserSwitch() {
  const btn = document.getElementById('btnSwitchUser');
  btn.addEventListener('click', () => {
    const nuevo = prompt('Ingrese el correo electrónico para simular la sesión docente:', currentUserEmail);
    if (nuevo && nuevo.trim()) {
      currentUserEmail = nuevo.trim();
      document.getElementById('currentUserEmail').textContent = currentUserEmail;
      checkAuthStatus();
    }
  });
  checkAuthStatus();
}

async function checkAuthStatus() {
  try {
    const res = await fetch(`/api/teacher/auth-status?user_email=${encodeURIComponent(currentUserEmail)}`);
    const data = await res.json();
    const badge = document.getElementById('authBadge');
    if (data.is_authorized) {
      badge.className = 'auth-badge authorized';
      badge.innerHTML = `<span class="status-dot"></span><span>${data.email}</span><span class="badge-role">Docente Autorizado</span>`;
    } else {
      badge.className = 'auth-badge';
      badge.innerHTML = `<span class="status-dot" style="background:#dc2626"></span><span>${data.email}</span><span class="badge-role" style="background:#fee2e2;color:#991b1b">No Autorizado</span>`;
    }
  } catch (err) {
    console.error('Error comprobando autorización:', err);
  }
}

// MATRIZ Y DROPDOWNS
async function loadMatrixAndInitDropdowns() {
  try {
    const res = await fetch('/api/teacher/matrix', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (!res.ok) return;
    matrixConfigCache = await res.json();
    updateCompetenciasDropdown();
  } catch (err) {
    console.error('Error cargando matriz:', err);
  }
}

function setupModuleChange() {
  const selMod = document.getElementById('selectModulo');
  selMod.addEventListener('change', updateCompetenciasDropdown);
}

function updateCompetenciasDropdown() {
  const selMod = document.getElementById('selectModulo');
  const selComp = document.getElementById('selectCompetencia');
  const modKey = selMod.value;
  selComp.innerHTML = '';

  if (!matrixConfigCache || !matrixConfigCache.modulos || !matrixConfigCache.modulos[modKey]) {
    selComp.innerHTML = '<option value="general">Competencia general</option>';
    return;
  }

  const mod = matrixConfigCache.modulos[modKey];

  if (modKey === 'razonamiento_cuantitativo' && mod.competencias) {
    mod.competencias.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `${c.nombre} (${c.porcentaje_objetivo}%)`;
      selComp.appendChild(opt);
    });
  } else if (modKey === 'lectura_critica' && mod.afirmaciones) {
    mod.afirmaciones.forEach(a => {
      const opt = document.createElement('option');
      opt.value = a.id;
      opt.textContent = `${a.nombre} (${a.porcentaje_objetivo}%)`;
      selComp.appendChild(opt);
    });
  } else if (modKey === 'competencias_ciudadanas' && mod.competencias) {
    mod.competencias.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `${c.nombre} (${c.porcentaje_objetivo}%)`;
      selComp.appendChild(opt);
    });
  } else if (modKey === 'ingles' && mod.partes) {
    mod.partes.forEach(p => {
      const opt = document.createElement('option');
      opt.value = `parte_${p.parte}`;
      opt.textContent = `${p.nombre} (${p.porcentaje_objetivo}% | ${p.opciones_cantidad} opciones)`;
      selComp.appendChild(opt);
    });
  }
}

// FORMULARIO DE GENERACIÓN
function setupFormGeneracion() {
  const form = document.getElementById('generateForm');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const modulo = document.getElementById('selectModulo').value;
    const competencia = document.getElementById('selectCompetencia').value;
    const dificultad = document.getElementById('selectDificultad').value;
    const cantidad = document.getElementById('inputCantidad').value;
    const evidencia = document.getElementById('inputEvidencia').value;

    const btn = document.getElementById('btnSubmitGenerate');
    btn.disabled = true;
    btn.innerHTML = '<span>⏳ Encolando lote...</span>';

    try {
      const res = await fetch('/api/teacher/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-user-email': currentUserEmail
        },
        body: JSON.stringify({ modulo, competencia, dificultad, cantidad, evidencia })
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Error al encolar');

      alert(`✅ Lote encolado con éxito (ID: ${data.batch.id}). La cola procesará con concurrencia 1.`);
      loadQueueStatus();
    } catch (err) {
      alert(`❌ Error: ${err.message}`);
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<span>🚀 Encolar Lote de Generación (Concurrencia 1)</span>';
    }
  });
}

// ESTADO DE LA COLA
async function loadQueueStatus() {
  try {
    const res = await fetch('/api/teacher/generate/status', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (!res.ok) return;
    const status = await res.json();

    document.getElementById('queueStatusText').textContent = status.isProcessing ? '⚡ Procesando (1 a 1)' : 'Inactiva (En espera)';
    document.getElementById('queueStatusText').style.color = status.isProcessing ? '#16a34a' : '#64748b';
    document.getElementById('queuePendingCount').textContent = status.pendingBatches;
    document.getElementById('queueDailyCount').textContent = `${status.dailyCount} / ${status.dailyCap}`;

    const list = document.getElementById('recentBatchesList');
    if (!status.recentBatches || status.recentBatches.length === 0) {
      list.innerHTML = '<p class="text-muted">No hay lotes encolados recientemente.</p>';
      return;
    }

    list.innerHTML = status.recentBatches.map(b => `
      <div class="batch-item">
        <div>
          <strong>${b.modulo}</strong> (${b.cantidad} ítems)
          <br><small class="text-muted">${b.id} | ${new Date(b.createdAt).toLocaleTimeString()}</small>
        </div>
        <div>
          <span class="badge ${b.status === 'completado' ? 'badge-success' : (b.status === 'procesando' ? 'badge-draft' : 'badge-module')}">
            ${b.status} (${b.generadas}/${b.cantidad})
          </span>
        </div>
      </div>
    `).join('');
  } catch (err) {
    console.error('Error cargando cola:', err);
  }
}

// COLA DE BORRADORES
async function loadDrafts() {
  const container = document.getElementById('draftsStream');
  const modFilter = document.getElementById('filterModuloReview').value;
  container.innerHTML = '<p class="text-muted">Cargando borradores...</p>';

  try {
    let url = '/api/teacher/questions?estado=borrador';
    if (modFilter) url += `&modulo=${modFilter}`;
    const res = await fetch(url, { headers: { 'x-user-email': currentUserEmail } });
    const questions = await res.json();

    document.getElementById('draftCountBadge').textContent = questions.length;
    document.getElementById('reviewCountBadge').textContent = `${questions.length} borradores`;

    if (questions.length === 0) {
      container.innerHTML = '<div class="card"><p class="text-muted">No hay preguntas pendientes de revisión en este filtro. ¡Excelente trabajo!</p></div>';
      return;
    }

    container.innerHTML = questions.map(q => renderQuestionCard(q, true)).join('');
    setupDraftActions();
  } catch (err) {
    container.innerHTML = `<p class="text-danger">Error: ${err.message}</p>`;
  }
}

function renderQuestionCard(q, isDraft = false) {
  const optionsHtml = Object.entries(q.opciones || {}).map(([k, text]) => `
    <div class="q-option ${k === q.respuesta_correcta ? 'correct-option' : ''}">
      <span class="option-key">${k}.</span>
      <span>${escapeHtml(text)}</span>
    </div>
  `).join('');

  return `
    <div class="question-card" id="card-${q.id}">
      <div class="q-header">
        <div class="q-meta">
          <span class="badge badge-module">${q.modulo}</span>
          <span class="badge badge-${q.estado_revision === 'aprobada' ? 'success' : (q.estado_revision === 'borrador' ? 'draft' : 'danger')}">
            ${q.estado_revision}
          </span>
          <span class="small text-muted">ID: ${q.id}</span>
          <span class="small text-muted">Dificultad: Nivel ${q.dificultad}</span>
          <span class="tag-confirm">${q.rotulo}</span>
        </div>
        <div class="small text-muted">${q.modelo} | ${q.version_del_prompt}</div>
      </div>

      ${q.texto_lectura ? `<div class="q-text-passage"><strong>Texto de Lectura (${countWords(q.texto_lectura)} palabras):</strong><br>${escapeHtml(q.texto_lectura)}</div>` : ''}

      <div class="q-enunciado">${escapeHtml(q.enunciado)}</div>

      <div class="q-options">
        ${optionsHtml}
      </div>

      <div class="q-explanation">
        <strong>Explicación:</strong>
        <p><strong>Por qué es correcta:</strong> ${escapeHtml(q.explicacion?.por_que_correcta || '')}</p>
        <p><strong>Por qué fallan distractores:</strong> ${escapeHtml(q.explicacion?.por_que_fallan_distractores || '')}</p>
      </div>

      ${isDraft ? `
        <div class="q-actions">
          <button class="btn btn-secondary btn-sm btn-edit-q" data-id="${q.id}">✏️ Editar</button>
          <button class="btn btn-danger btn-sm btn-reject-q" data-id="${q.id}">✕ Rechazar con comentario</button>
          <button class="btn btn-success btn-sm btn-approve-q" data-id="${q.id}">✓ Aprobar para Publicar</button>
        </div>
      ` : ''}
    </div>
  `;
}

function setupDraftActions() {
  document.querySelectorAll('.btn-approve-q').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      await reviewQuestion(id, 'aprobar', 'Pregunta validada y aprobada por docente.');
    });
  });

  document.querySelectorAll('.btn-reject-q').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const comment = prompt('Motivo del rechazo:', 'Distractor ambiguo o enunciado confuso');
      if (comment !== null) {
        await reviewQuestion(id, 'rechazar', comment);
      }
    });
  });

  document.querySelectorAll('.btn-edit-q').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = btn.getAttribute('data-id');
      const nuevoEnunciado = prompt('Editar enunciado de la pregunta:');
      if (nuevoEnunciado && nuevoEnunciado.trim()) {
        reviewQuestion(id, 'editar', 'Edición menor de redacción', { enunciado: nuevoEnunciado.trim() });
      }
    });
  });

  document.getElementById('btnRefreshReview').addEventListener('click', loadDrafts);
  document.getElementById('filterModuloReview').addEventListener('change', loadDrafts);
}

async function reviewQuestion(id, accion, comentario, pregunta_editada = null) {
  try {
    const res = await fetch(`/api/teacher/questions/${id}/review`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-user-email': currentUserEmail
      },
      body: JSON.stringify({ accion, comentario, pregunta_editada })
    });
    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.error);
    }
    loadDrafts();
    loadPublishedBank();
  } catch (err) {
    alert(`Error: ${err.message}`);
  }
}

// BANCO PUBLICADO
async function loadPublishedBank() {
  try {
    const vRes = await fetch('/api/v1/bank/version');
    const vData = await vRes.json();
    document.getElementById('publishedVersionTag').textContent = `Versión actual: ${vData.version}`;
    document.getElementById('currentSha256Value').textContent = vData.sha256 || 'Sin huella (banco vacío o sin publicar)';
    document.getElementById('publishedCountBadge').textContent = vData.total_preguntas_aprobadas || 0;

    const bRes = await fetch('/api/v1/bank/latest');
    const bData = await bRes.json();
    const stream = document.getElementById('publishedStream');

    if (!bData.preguntas || bData.preguntas.length === 0) {
      stream.innerHTML = '<div class="card"><p class="text-muted">Aún no hay preguntas publicadas en esta versión. Aprueba preguntas de la cola de borradores y presiona "Publicar Nueva Versión Inmutable".</p></div>';
      return;
    }

    stream.innerHTML = bData.preguntas.map(q => renderQuestionCard({ ...q, estado_revision: 'aprobada' }, false)).join('');
  } catch (err) {
    console.error('Error cargando banco publicado:', err);
  }
}

// MODAL DE PUBLICACIÓN
function setupPublishModal() {
  const modal = document.getElementById('publishModal');
  document.getElementById('btnOpenPublishModal').addEventListener('click', () => {
    document.getElementById('inputVersionTag').value = `v${new Date().toISOString().slice(0, 10).replace(/-/g, '.')}-${Date.now().toString().slice(-4)}`;
    modal.classList.add('active');
  });

  document.getElementById('btnClosePublishModal').addEventListener('click', () => {
    modal.classList.remove('active');
  });

  document.getElementById('btnConfirmPublish').addEventListener('click', async () => {
    const tag = document.getElementById('inputVersionTag').value;
    try {
      const res = await fetch('/api/teacher/publish', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-user-email': currentUserEmail
        },
        body: JSON.stringify({ version_tag: tag })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);

      alert(`✅ Banco publicado exitosamente como versión inmutable!\nHuella SHA-256: ${data.published.sha256}`);
      modal.classList.remove('active');
      loadPublishedBank();
      loadDistribution();
    } catch (err) {
      alert(`Error al publicar: ${err.message}`);
    }
  });
}

// CONTROL DE DISTRIBUCIÓN
async function loadDistribution() {
  try {
    const res = await fetch('/api/teacher/distribution', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (!res.ok) return;
    const data = await res.json();

    const container = document.getElementById('distributionContainer');
    const rep = data.deviations?.reportePorModulo || {};

    container.innerHTML = Object.entries(rep).map(([modKey, modInfo]) => {
      const itemsHtml = (modInfo.breakdown || []).map(b => `
        <div class="dist-item">
          <div style="display:flex;justify-content:space-between">
            <span>${b.nombre}</span>
            <span><strong>${b.actual_pct}%</strong> (Objetivo: ${b.objetivo_pct}%)</span>
          </div>
          <div class="dist-progress-bg">
            <div class="dist-progress-fill ${Math.abs(b.desviacion) > 10 ? 'warning' : ''}" style="width: ${Math.min(b.actual_pct, 100)}%"></div>
          </div>
          <small class="${b.desviacion === 0 ? 'text-muted' : (Math.abs(b.desviacion) > 10 ? 'text-danger' : 'text-muted')}">
            Desviación: ${b.desviacion > 0 ? '+' : ''}${b.desviacion}% (${b.actual_count} preguntas)
          </small>
        </div>
      `).join('');

      return `
        <div class="dist-module-card">
          <h3>${modKey.replace(/_/g, ' ').toUpperCase()}</h3>
          <p class="small text-muted">Total Aprobadas: ${modInfo.total} preguntas</p>
          ${itemsHtml || '<p class="text-muted small">Sin desglose disponible</p>'}
        </div>
      `;
    }).join('');

    // Balance de claves A-D
    const distBars = document.getElementById('keyBalanceBars');
    const kb = data.keyBalance?.distribution || {};
    distBars.innerHTML = ['A', 'B', 'C', 'D'].map(k => `
      <div class="key-bar-box">
        <div class="key-letter">${k}</div>
        <div class="key-pct">${kb[k]?.percentage || 0}%</div>
        <small class="text-muted">${kb[k]?.count || 0} ítems</small>
      </div>
    `).join('');
  } catch (err) {
    console.error('Error cargando distribución:', err);
  }
}

// COMUNICACIÓN ESCRITA
async function loadWritingTopics() {
  try {
    const res = await fetch('/api/teacher/writing-topics', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (!res.ok) return;
    const topics = await res.json();
    const grid = document.getElementById('writingTopicsGrid');

    grid.innerHTML = topics.map(t => `
      <div class="topic-card">
        <h3>${escapeHtml(t.titulo)}</h3>
        <div class="topic-prompt">${escapeHtml(t.afirmacion)}</div>
        <p class="small text-muted">${escapeHtml(t.contexto || '')}</p>
        <div class="generation-notice" style="margin-top:0.5rem">
          <strong>Criterios ICFES:</strong> Planteamiento, Organización, Forma de expresión, Pertinencia.
        </div>
      </div>
    `).join('');
  } catch (err) {
    console.error('Error cargando temas:', err);
  }
}

function setupEssayFeedback() {
  const toggle = document.getElementById('toggleEssayFeedback');
  toggle.addEventListener('change', async () => {
    try {
      await fetch('/api/teacher/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'x-user-email': currentUserEmail },
        body: JSON.stringify({ essay_feedback_enabled: toggle.checked })
      });
      document.getElementById('essayServiceStatusBadge').textContent = toggle.checked
        ? 'Fase 3 — Activo'
        : 'Fase 3 — Desactivado por defecto';
    } catch (err) {
      alert(`Error actualizando bandera: ${err.message}`);
    }
  });

  document.getElementById('btnTestEssayFeedback').addEventListener('click', async () => {
    const text = document.getElementById('testerEssayText').value;
    const consent = document.getElementById('testerConsentCheck').checked;
    const resultBox = document.getElementById('essayFeedbackResult');

    resultBox.style.display = 'block';
    resultBox.innerHTML = '⏳ Procesando ensayo con rúbrica estructurada...';

    try {
      const res = await fetch('/api/v1/essay-feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ essay_text: text, consent, prompt_id: 'tema_ce_01' })
      });
      const data = await res.json();
      if (!res.ok) {
        resultBox.innerHTML = `<span class="badge badge-danger">Error ${res.status}</span><p>${data.error || data.mensaje}</p>`;
        return;
      }

      resultBox.innerHTML = `
        <span class="badge badge-success">Retroalimentación Generada (Sin Puntaje 0-300 | Sin Almacenamiento)</span>
        <h4 style="margin-top:0.75rem">${escapeHtml(data.consigna || '')}</h4>
        <pre style="background:#f8fafc;padding:0.75rem;border-radius:6px;font-size:0.82rem;overflow-x:auto">${JSON.stringify(data.retroalimentacion || data.criterios, null, 2)}</pre>
      `;
    } catch (err) {
      resultBox.innerHTML = `<span class="badge badge-danger">Fallo de red:</span> ${err.message}`;
    }
  });

  document.getElementById('btnNewTopic').addEventListener('click', async () => {
    const titulo = prompt('Título del tema de dominio público:', 'Regulación de Redes Sociales en Menores');
    if (!titulo) return;
    const afirmacion = prompt('Afirmación a debatir:', '¿Está de acuerdo con la afirmación: "El Estado colombiano debe restringir el acceso a redes sociales a menores de 16 años"?');
    if (!afirmacion) return;
    const contexto = prompt('Contexto:', 'Preocupaciones de salud mental infantil frente al derecho al libre desarrollo.');

    try {
      await fetch('/api/teacher/writing-topics', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-user-email': currentUserEmail },
        body: JSON.stringify({ titulo, afirmacion, contexto })
      });
      loadWritingTopics();
    } catch (err) {
      alert(`Error creando tema: ${err.message}`);
    }
  });
}

// REPORTES ESTUDIANTILES
function setupStudentReportsSimulation() {
  document.getElementById('btnSimulateReport').addEventListener('click', async () => {
    const qId = document.getElementById('reportSimQuestionId').value.trim();
    const reason = document.getElementById('reportSimReason').value;
    const msg = document.getElementById('reportSimMessage');

    if (!qId) {
      alert('Ingresa el ID de la pregunta para reportar.');
      return;
    }

    try {
      const res = await fetch('/api/v1/reports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question_id: qId, reason, details: "Reporte simulado desde panel de pruebas" })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);

      msg.innerHTML = `✅ ${data.mensaje} <strong>${data.accion_automatica}</strong>`;
      loadUnderReviewQuestions();
      loadPublishedBank();
    } catch (err) {
      msg.innerHTML = `❌ Error: ${err.message}`;
    }
  });
}

async function loadUnderReviewQuestions() {
  try {
    const res = await fetch('/api/teacher/questions?estado=en_revision', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (!res.ok) return;
    const questions = await res.json();
    const stream = document.getElementById('underReviewStream');

    if (questions.length === 0) {
      stream.innerHTML = '<div class="card"><p class="text-muted">No hay preguntas retiradas a "en revisión" actualmente.</p></div>';
      return;
    }

    stream.innerHTML = questions.map(q => `
      <div class="question-card" style="border-left:4px solid #f59e0b">
        <div class="q-header">
          <div class="q-meta">
            <span class="badge badge-review">en_revision</span>
            <span class="badge badge-module">${q.modulo}</span>
            <span class="small text-muted">ID: ${q.id}</span>
          </div>
          <span class="tag-confirm">${q.motivo_en_revision || 'Retirada por acumulación de reportes'}</span>
        </div>
        <div class="q-enunciado">${escapeHtml(q.enunciado)}</div>
        <div class="q-actions">
          <button class="btn btn-secondary btn-sm" onclick="reviewQuestion('${q.id}', 'editar', 'Subsanado reporte')">Editar y Subsanar</button>
          <button class="btn btn-success btn-sm" onclick="reviewQuestion('${q.id}', 'aprobar', 'Re-aprobada tras revisión')">Re-Aprobar y Regresar al Banco</button>
        </div>
      </div>
    `).join('');
  } catch (err) {
    console.error('Error cargando preguntas en revisión:', err);
  }
}

// CONFIGURACIÓN Y MATRIZ
async function loadConfig() {
  try {
    const res = await fetch('/api/teacher/config', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (!res.ok) return;
    appConfigCache = await res.json();

    document.getElementById('cfgModel').value = appConfigCache.gemini_model || '';
    document.getElementById('cfgAuthorizedEmails').value = (appConfigCache.authorized_emails || []).join(', ');
    document.getElementById('cfgDailyCap').value = appConfigCache.daily_generation_cap || 250;
    document.getElementById('cfgReportThreshold').value = appConfigCache.student_reports_threshold || 3;

    const toggle = document.getElementById('toggleEssayFeedback');
    toggle.checked = Boolean(appConfigCache.essay_feedback_enabled);
    document.getElementById('essayServiceStatusBadge').textContent = toggle.checked
      ? 'Fase 3 — Activo'
      : 'Fase 3 — Desactivado por defecto';

    const matRes = await fetch('/api/teacher/matrix', {
      headers: { 'x-user-email': currentUserEmail }
    });
    if (matRes.ok) {
      const mat = await matRes.json();
      document.getElementById('matrixJsonEditor').value = JSON.stringify(mat, null, 2);
    }
  } catch (err) {
    console.error('Error cargando config:', err);
  }
}

function setupConfigForms() {
  document.getElementById('appConfigForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const emails = document.getElementById('cfgAuthorizedEmails').value.split(',').map(s => s.trim()).filter(Boolean);
    const gemini_model = document.getElementById('cfgModel').value.trim();
    const daily_generation_cap = parseInt(document.getElementById('cfgDailyCap').value, 10);
    const student_reports_threshold = parseInt(document.getElementById('cfgReportThreshold').value, 10);

    try {
      const res = await fetch('/api/teacher/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'x-user-email': currentUserEmail },
        body: JSON.stringify({ authorized_emails: emails, gemini_model, daily_generation_cap, student_reports_threshold })
      });
      if (!res.ok) throw new Error('Error guardando configuración');
      alert('✅ Configuración guardada correctamente.');
      loadConfig();
    } catch (err) {
      alert(`Error: ${err.message}`);
    }
  });

  document.getElementById('btnSaveMatrix').addEventListener('click', async () => {
    try {
      const parsed = JSON.parse(document.getElementById('matrixJsonEditor').value);
      const res = await fetch('/api/teacher/matrix', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'x-user-email': currentUserEmail },
        body: JSON.stringify(parsed)
      });
      if (!res.ok) throw new Error('Error guardando matriz');
      alert('✅ Matriz oficial versionada guardada correctamente.');
      loadMatrixAndInitDropdowns();
    } catch (err) {
      alert(`Error en JSON de matriz: ${err.message}`);
    }
  });
}

function countWords(str) {
  if (!str) return 0;
  return str.trim().split(/\s+/).filter(Boolean).length;
}

function escapeHtml(text) {
  if (!text) return '';
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
