-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — Control de Costos
-- EJECUTAR MANUALMENTE en el SQL Editor de Supabase.
-- CLP; todos los montos se guardan con IVA incluido (no se separa neto).
-- Idempotente donde es posible (IF NOT EXISTS / ON CONFLICT / CREATE OR REPLACE).
-- ════════════════════════════════════════════════════════════════════════

-- ── 1) Columna categoria en articulos (Requerimiento 3) ─────────────────────
ALTER TABLE articulos
    ADD COLUMN IF NOT EXISTS categoria TEXT NOT NULL DEFAULT 'INGREDIENTES';

ALTER TABLE articulos DROP CONSTRAINT IF EXISTS chk_articulo_categoria;
ALTER TABLE articulos ADD CONSTRAINT chk_articulo_categoria
    CHECK (categoria IN ('INGREDIENTES','PACKAGING','INSUMOS'));

-- ── 2) Costos fijos recurrentes (Requerimientos 1, 2) ───────────────────────
CREATE TABLE IF NOT EXISTS costos_fijos (
    id           SERIAL PRIMARY KEY,
    nombre       TEXT NOT NULL,
    categoria    TEXT NOT NULL,                                   -- CategoriaGasto
    monto        NUMERIC NOT NULL DEFAULT 0 CHECK (monto >= 0),   -- Req 1.4 / 1.6
    periodicidad TEXT NOT NULL DEFAULT 'MENSUAL'
                 CHECK (periodicidad IN ('SEMANAL','MENSUAL','ANUAL')),
    activo       BOOLEAN NOT NULL DEFAULT TRUE,                   -- Req 1.3
    local_id     INTEGER REFERENCES locales(id),
    created_at   TIMESTAMPTZ DEFAULT now(),
    updated_at   TIMESTAMPTZ DEFAULT now(),
    created_by   UUID
);

-- ── 3) Configuración de objetivos por local (Req 10, 11, 15) ────────────────
CREATE TABLE IF NOT EXISTS config_costos (
    local_id INTEGER NOT NULL DEFAULT 0,
    clave    TEXT NOT NULL,
    valor    NUMERIC NOT NULL,
    PRIMARY KEY (local_id, clave)
);

-- Defaults globales (local_id = 0). No pisa valores ya configurados.
INSERT INTO config_costos(local_id, clave, valor) VALUES
    (0, 'pct_food_objetivo',      0.32),
    (0, 'pct_mano_obra_objetivo', 0.30),
    (0, 'pct_arriendo_techo',     0.10),
    (0, 'umbral_contratar_mo',    0)
ON CONFLICT (local_id, clave) DO NOTHING;

-- ── 4) Snapshots de cierre semanal (Req 6, 9) ───────────────────────────────
CREATE TABLE IF NOT EXISTS cierres_semanales (
    id                  SERIAL PRIMARY KEY,
    semana_inicio       DATE NOT NULL,          -- lunes
    semana_fin          DATE NOT NULL,          -- sábado
    ventas_cobradas     NUMERIC NOT NULL DEFAULT 0,
    costo_variable      NUMERIC NOT NULL DEFAULT 0,
    food_teorico        NUMERIC NOT NULL DEFAULT 0,
    mano_obra_pagada    NUMERIC NOT NULL DEFAULT 0,
    fijos_prorrateados  NUMERIC NOT NULL DEFAULT 0,
    resultado           NUMERIC NOT NULL DEFAULT 0,
    food_pct            NUMERIC NOT NULL DEFAULT 0,
    labor_pct           NUMERIC NOT NULL DEFAULT 0,
    break_even          NUMERIC,                -- NULL si no calculable
    margen_contribucion NUMERIC NOT NULL DEFAULT 0,
    estado              TEXT NOT NULL DEFAULT 'CERRADO'
                        CHECK (estado IN ('ABIERTO','CERRADO')),
    local_id            INTEGER REFERENCES locales(id),
    closed_at           TIMESTAMPTZ DEFAULT now(),
    created_by          UUID,
    UNIQUE (local_id, semana_inicio)            -- una semana = un snapshot por local
);

-- ── 5) Pasos de la rutina semanal (Req 14) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS pasos_rutina_semanal (
    id            SERIAL PRIMARY KEY,
    semana_inicio DATE NOT NULL,
    paso          TEXT NOT NULL CHECK (paso IN ('CONTEO','MERMAS','PROVISION','RESULTADO')),
    completado    BOOLEAN NOT NULL DEFAULT FALSE,
    completado_at TIMESTAMPTZ,
    local_id      INTEGER REFERENCES locales(id),
    UNIQUE (local_id, semana_inicio, paso)
);

-- ── 6) RLS (acceso para usuarios autenticados; app single-org) ──────────────
ALTER TABLE costos_fijos          ENABLE ROW LEVEL SECURITY;
ALTER TABLE config_costos         ENABLE ROW LEVEL SECURITY;
ALTER TABLE cierres_semanales     ENABLE ROW LEVEL SECURITY;
ALTER TABLE pasos_rutina_semanal  ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "auth all costos_fijos" ON costos_fijos;
CREATE POLICY "auth all costos_fijos" ON costos_fijos
    FOR ALL USING (auth.role() = 'authenticated') WITH CHECK (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "auth all config_costos" ON config_costos;
CREATE POLICY "auth all config_costos" ON config_costos
    FOR ALL USING (auth.role() = 'authenticated') WITH CHECK (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "auth all cierres_semanales" ON cierres_semanales;
CREATE POLICY "auth all cierres_semanales" ON cierres_semanales
    FOR ALL USING (auth.role() = 'authenticated') WITH CHECK (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "auth all pasos_rutina_semanal" ON pasos_rutina_semanal;
CREATE POLICY "auth all pasos_rutina_semanal" ON pasos_rutina_semanal
    FOR ALL USING (auth.role() = 'authenticated') WITH CHECK (auth.role() = 'authenticated');

-- ════════════════════════════════════════════════════════════════════════
-- NOTA: Las RPCs (registrar_compra v2 "último precio", recalcular_recetas_articulo
-- y confirmar_cierre_semanal) se agregan a este archivo en las tareas 9 y 12.
-- ════════════════════════════════════════════════════════════════════════
