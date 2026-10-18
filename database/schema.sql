-- =====================================================================
-- PLATAFORMA ACIERTAPRO: ESQUEMA DDL, RLS Y FUNCIONES DE BASE DE DATOS
-- Arquitectura Cloud Supabase (PostgreSQL) + Sincronización PowerSync / SQLite
-- =====================================================================

-- 1. EXTENSIONES Y ENUMS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$ BEGIN
    CREATE TYPE public.media_type_enum AS ENUM ('image', 'svg', 'diagram');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 2. TABLA DE MÓDULOS (5 Genéricos + 35 Específicos ICFES)
CREATE TABLE IF NOT EXISTS public.modules (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    category_type TEXT NOT NULL CHECK (category_type IN ('generic', 'specific')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. TABLA DE PREGUNTAS (Con soporte multimedia y accesibilidad Res. 675/2019)
CREATE TABLE IF NOT EXISTS public.questions (
    id TEXT PRIMARY KEY,
    module_id TEXT NOT NULL REFERENCES public.modules(id) ON DELETE CASCADE,
    competencia_tema TEXT NOT NULL,
    evidencia_asociada TEXT,
    tipo_texto_o_contexto TEXT,
    enunciado TEXT NOT NULL,
    opciones JSONB NOT NULL,
    respuesta_correcta TEXT NOT NULL,
    explicacion JSONB,
    dificultad INT DEFAULT 2,
    media_urls JSONB DEFAULT '[]'::jsonb,
    media_type public.media_type_enum DEFAULT 'image',
    alt_text_accessibility TEXT DEFAULT NULL,
    status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'approved', 'rejected')),
    curator_score INT DEFAULT NULL,
    unambiguity_flag BOOLEAN DEFAULT FALSE,
    pedagogical_alignment_score INT DEFAULT NULL,
    difficulty_calibration TEXT DEFAULT NULL,
    rejection_reasons JSONB DEFAULT '[]'::jsonb,
    generado_por_ia BOOLEAN DEFAULT FALSE,
    modelo TEXT DEFAULT 'gemini-1.5-flash',
    version_del_prompt TEXT DEFAULT 'v2026.2.1',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índice GIN para consultas eficientes en metadatos multimedia
CREATE INDEX IF NOT EXISTS idx_questions_media_urls_gin ON public.questions USING GIN (media_urls);
CREATE INDEX IF NOT EXISTS idx_questions_status_module ON public.questions(status, module_id);

-- 4. TABLA DE PERFILES DE USUARIO (Ajustada a la Resolución 675 de 2019 ICFES)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    name TEXT,
    academic_program TEXT DEFAULT 'Ingeniería de Sistemas',
    campus TEXT DEFAULT 'Sede Maicao',
    target_date TEXT DEFAULT 'Noviembre 2026',
    study_days_per_week INT DEFAULT 4,
    session_minutes INT DEFAULT 30,
    has_consented BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMPTZ,
    consent_version TEXT DEFAULT 'Ley 1581 v1.0',
    is_adjusted_booklet BOOLEAN DEFAULT FALSE,
    includes_english_test BOOLEAN DEFAULT TRUE,
    disability_type TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. TABLA DE RESPUESTAS DE USUARIOS (user_answers)
CREATE TABLE IF NOT EXISTS public.user_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    attempt_id TEXT NOT NULL,
    question_id TEXT NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    selected_option TEXT NOT NULL,
    selected_original_key TEXT NOT NULL,
    selected_option_text TEXT DEFAULT '',
    is_correct BOOLEAN NOT NULL,
    time_spent_seconds BIGINT DEFAULT 0,
    marked_for_review BOOLEAN DEFAULT FALSE,
    answered_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_answers_user ON public.user_answers(user_id, attempt_id);

-- 6. TABLA DE ENSAYOS DE COMUNICACIÓN ESCRITA (essays)
CREATE TABLE IF NOT EXISTS public.essays (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    word_count INT DEFAULT 0,
    score_cohesion INT CHECK (score_cohesion BETWEEN 1 AND 5),
    score_coherencia INT CHECK (score_coherencia BETWEEN 1 AND 5),
    score_estructura_argumentativa INT CHECK (score_estructura_argumentativa BETWEEN 1 AND 5),
    score_ortografia_gramatica INT CHECK (score_ortografia_gramatica BETWEEN 1 AND 5),
    performance_level TEXT CHECK (performance_level IN ('Insuficiente', 'Mínimo', 'Satisfactorio', 'Avanzado')),
    qualitative_feedback JSONB,
    self_score_thesis INT DEFAULT 0,
    self_score_arguments INT DEFAULT 0,
    self_score_structure INT DEFAULT 0,
    self_score_language INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_essays_user ON public.essays(user_id);

-- 7. FUNCIÓN PL/pgSQL DE BALANCEO DINÁMICO DE MÓDULOS DEFICITARIOS
CREATE OR REPLACE FUNCTION public.fn_get_priority_modules_for_batch(
    target_questions_per_module INT DEFAULT 100
)
RETURNS TABLE (
    module_id TEXT,
    module_name TEXT,
    category_type TEXT,
    current_count BIGINT,
    deficit INT
) 
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    WITH module_counts AS (
        SELECT 
            q.module_id,
            COUNT(q.id) AS total
        FROM public.questions q
        WHERE q.status = 'approved'
        GROUP BY q.module_id
    )
    SELECT 
        m.id AS module_id,
        m.name AS module_name,
        m.category_type,
        COALESCE(mc.total, 0) AS current_count,
        (target_questions_per_module - COALESCE(mc.total, 0))::INT AS deficit
    FROM public.modules m
    LEFT JOIN module_counts mc ON m.id = mc.module_id
    WHERE COALESCE(mc.total, 0) < target_questions_per_module
    ORDER BY deficit DESC, m.category_type ASC
    LIMIT 3;
END;
$$;

-- 8. POLÍTICAS DE SEGURIDAD A NIVEL DE FILA (ROW LEVEL SECURITY - RLS)
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.essays ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modules ENABLE ROW LEVEL SECURITY;

-- Módulos: Lectura pública
CREATE POLICY "Permitir lectura publica de modulos"
ON public.modules FOR SELECT TO public USING (true);

-- Preguntas: Lectura pública solo aprobadas
CREATE POLICY "Permitir lectura publica de preguntas aprobadas"
ON public.questions FOR SELECT TO public USING (status = 'approved');

-- Bloqueo absoluto de modificación directa desde clientes
CREATE POLICY "Bloquear modificacion publica de preguntas"
ON public.questions FOR UPDATE TO authenticated, anon USING (false);

CREATE POLICY "Bloquear eliminacion publica de preguntas"
ON public.questions FOR DELETE TO authenticated, anon USING (false);

-- Respuestas (user_answers)
CREATE POLICY "Permitir insercion de respuestas asignadas al propio usuario"
ON public.user_answers FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Permitir lectura de respuestas propias"
ON public.user_answers FOR SELECT TO authenticated
USING (auth.uid() = user_id);

CREATE POLICY "Permitir actualizacion optimista de respuestas propias"
ON public.user_answers FOR UPDATE TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Ensayos (essays)
CREATE POLICY "Permitir insercion de ensayos propios"
ON public.essays FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Permitir lectura de ensayos propios"
ON public.essays FOR SELECT TO authenticated
USING (auth.uid() = user_id);

CREATE POLICY "Permitir actualizacion de ensayos propios"
ON public.essays FOR UPDATE TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Perfiles (profiles)
CREATE POLICY "Permitir lectura y actualizacion de perfil propio"
ON public.profiles FOR ALL TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- 9. POBLACIÓN DEL CATÁLOGO (5 Genéricos + 35 Específicos ICFES)
INSERT INTO public.modules (id, name, category_type) VALUES
    -- 5 Módulos Genéricos
    ('lectura_critica', 'Lectura Crítica', 'generic'),
    ('razonamiento_cuantitativo', 'Razonamiento Cuantitativo', 'generic'),
    ('competencias_ciudadanas', 'Competencias Ciudadanas', 'generic'),
    ('ingles', 'Inglés', 'generic'),
    ('comunicacion_escrita', 'Comunicación Escrita', 'generic'),
    -- 35 Módulos Específicos
    ('analisis_economico', 'Análisis Económico', 'specific'),
    ('atencion_salud', 'Atención en Salud', 'specific'),
    ('cuidado_enfermeria_clinico_comunitario', 'Cuidado de Enfermería en los Ámbitos Clínico y Comunitario', 'specific'),
    ('comunicacion_juridica', 'Comunicación Jurídica', 'specific'),
    ('diseno_software', 'Diseño de Software', 'specific'),
    ('ensenar_formar_evaluar', 'Enseñar, Formar, Evaluar', 'specific'),
    ('pensamiento_cientifico', 'Pensamiento Científico', 'specific'),
    ('formulacion_proyectos_ingenieria', 'Formulación de Proyectos de Ingeniería', 'specific'),
    ('gestion_conflicto', 'Gestión del Conflicto', 'specific'),
    ('gestion_organizaciones', 'Gestión de Organizaciones', 'specific'),
    ('investigacion_ciencias_sociales', 'Investigación en Ciencias Sociales', 'specific'),
    ('diseno_sistemas_productivos', 'Diseño de Sistemas Productivos y Logísticos', 'specific'),
    ('diseno_mecanico', 'Diseño Mecánico', 'specific'),
    ('fundamentacion_diagnostico_tratamiento_medico', 'Fundamentación en Diagnóstico y Tratamiento Médico', 'specific'),
    ('salud_publica', 'Salud Pública', 'specific'),
    ('produccion_agricola', 'Producción Agrícola', 'specific'),
    ('produccion_pecuaria', 'Producción Pecuaria', 'specific'),
    ('gestion_ambiental', 'Gestión Ambiental', 'specific'),
    ('intervencion_psicologica', 'Intervención Psicológica', 'specific'),
    ('evaluacion_diagnostico_psicologico', 'Evaluación y Diagnóstico Psicológico', 'specific'),
    ('procesos_psicologicos', 'Procesos Psicológicos', 'specific'),
    ('derecho_publico', 'Derecho Público', 'specific'),
    ('derecho_privado', 'Derecho Privado', 'specific'),
    ('informacion_control_financiero', 'Información y Control Financiero', 'specific'),
    ('gestion_financiera', 'Gestión Financiera', 'specific'),
    ('tributaria', 'Tributaria', 'specific'),
    ('arquitectura_proyectos', 'Proyecto de Arquitectura', 'specific'),
    ('urbanismo', 'Urbanismo', 'specific'),
    ('diseno_comunicacion_visual', 'Diseño de Comunicación Visual', 'specific'),
    ('diseno_industrial', 'Diseño Industrial', 'specific'),
    ('comunicacion_social_periodismo', 'Comunicación Social y Periodismo', 'specific'),
    ('educacion_pedagogia', 'Educación y Pedagogía', 'specific'),
    ('ciencias_farmaceuticas', 'Ciencias Farmacéuticas', 'specific'),
    ('procesamiento_alimentos', 'Procesamiento de Alimentos', 'specific'),
    ('infraestructura_vial', 'Infraestructura Vial y de Transporte', 'specific')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    category_type = EXCLUDED.category_type;
