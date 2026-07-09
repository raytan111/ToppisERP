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

-- ── 7) Recalcular recetas (preparaciones + items_menu) ──────────────────────
-- Recalcula el costo de TODAS las preparaciones (3 pasadas para propagar
-- preparaciones anidadas) y luego el costo teórico de todos los items de menú.
-- Se llama tras cambiar el costo de un artículo. Se mantiene el parámetro por
-- claridad de intención aunque el recálculo es global (dataset chico).
CREATE OR REPLACE FUNCTION recalcular_recetas_articulo(p_articulo_id INTEGER)
RETURNS VOID AS $$
DECLARE v_pass INT;
BEGIN
    FOR v_pass IN 1..3 LOOP
        UPDATE preparaciones p SET costo_lote = COALESCE((
            SELECT SUM(pc.cantidad_base *
                CASE pc.tipo_componente
                    WHEN 'ARTICULO' THEN (SELECT costo_base FROM articulos WHERE id = pc.componente_id)
                    ELSE (SELECT costo_base FROM preparaciones WHERE id = pc.componente_id)
                END)
            FROM preparacion_componentes pc WHERE pc.preparacion_id = p.id), 0)
        WHERE p.id > 0;
        UPDATE preparaciones p SET costo_base =
            CASE WHEN COALESCE(NULLIF(p.rendimiento_lote,0),1) > 0
                 THEN p.costo_lote / COALESCE(NULLIF(p.rendimiento_lote,0),1) ELSE 0 END
        WHERE p.id > 0;
    END LOOP;

    UPDATE items_menu i SET costo_teorico = COALESCE((
        SELECT SUM(rm.cantidad_base *
            CASE rm.tipo_componente
                WHEN 'ARTICULO' THEN (SELECT costo_base FROM articulos WHERE id = rm.componente_id)
                ELSE (SELECT costo_base FROM preparaciones WHERE id = rm.componente_id)
            END)
        FROM recetas_menu rm WHERE rm.item_menu_id = i.id), 0)
    WHERE i.id > 0;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── 8) registrar_compra v2 — ÚLTIMO PRECIO (Requerimiento 5) ─────────────────
-- Reemplaza el costo promedio ponderado por el ÚLTIMO precio pagado.
-- Solo actualiza el costo y recalcula recetas si el costo cambió. Mantiene la
-- misma firma que la versión de supabase-fase9-stamping.sql (no rompe Kotlin).
DROP FUNCTION IF EXISTS registrar_compra(INTEGER, BOOLEAN, TEXT, JSONB, INTEGER, UUID);
CREATE OR REPLACE FUNCTION registrar_compra(
    p_proveedor_id INTEGER, p_tiene_iva BOOLEAN, p_nota TEXT, p_items JSONB,
    p_sobre_id INTEGER, p_usuario UUID, p_local_id INTEGER DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    v_total NUMERIC := 0; v_compra_id INTEGER; v_item JSONB; v_art RECORD;
    v_cant NUMERIC; v_costo_base NUMERIC; v_subtotal NUMERIC;
    v_nuevo_costo_base NUMERIC; v_venc DATE;
    v_gasto_id BIGINT := NULL; v_neto NUMERIC; v_iva NUMERIC;
BEGIN
    SELECT COALESCE(SUM((it->>'cantidad_base')::numeric * (it->>'costo_por_base')::numeric), 0)
        INTO v_total FROM jsonb_array_elements(p_items) it;

    -- Pago desde sobre (opcional): genera gasto vinculado + egreso del sobre
    IF p_sobre_id IS NOT NULL AND v_total > 0 THEN
        IF p_tiene_iva THEN v_neto := round(v_total / 1.19); v_iva := v_total - v_neto;
        ELSE v_neto := v_total; v_iva := 0; END IF;
        PERFORM 1 FROM sobres WHERE id = p_sobre_id FOR UPDATE;
        IF (SELECT saldo FROM sobres WHERE id = p_sobre_id) < v_total THEN
            RAISE EXCEPTION 'Saldo insuficiente en el sobre para la compra';
        END IF;
        INSERT INTO gastos(descripcion, monto, categoria, sobre_id, usuario_id, fecha,
                           comprobante, tiene_iva, monto_neto, monto_iva, local_id, created_by)
        VALUES ('Compra de mercadería', v_total, 'INSUMOS', p_sobre_id, p_usuario, now(),
                '', p_tiene_iva, v_neto, v_iva, p_local_id, p_usuario)
        RETURNING id INTO v_gasto_id;
        UPDATE sobres SET saldo = saldo - v_total WHERE id = p_sobre_id;
        INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
        VALUES (p_sobre_id, NULL, v_total, 'EGRESO', 'Compra de mercadería', p_usuario);
    END IF;

    INSERT INTO compras(proveedor_id, fecha, total, tiene_iva, nota, gasto_id, local_id, created_by)
    VALUES (p_proveedor_id, now(), v_total, p_tiene_iva, COALESCE(p_nota, ''), v_gasto_id, p_local_id, p_usuario)
    RETURNING id INTO v_compra_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        v_cant := (v_item->>'cantidad_base')::numeric;
        v_costo_base := (v_item->>'costo_por_base')::numeric;  -- costo por unidad base (bruto)
        v_subtotal := v_cant * v_costo_base;
        v_venc := NULLIF(v_item->>'vencimiento', '')::date;
        SELECT * INTO v_art FROM articulos WHERE id = (v_item->>'articulo_id')::int FOR UPDATE;
        IF v_art.id IS NOT NULL THEN
            -- ÚLTIMO precio: costo_base = costo_por_base / rendimiento
            v_nuevo_costo_base := v_costo_base / COALESCE(NULLIF(v_art.rendimiento,0), 1);
            IF v_nuevo_costo_base IS DISTINCT FROM v_art.costo_base THEN
                -- costo_base = costo por unidad base (ej: $9/g). costo_compra se
                -- guarda como precio del "pack" completo para que el formulario
                -- de edición lo muestre coherente: costo_compra = $/base × factor.
                UPDATE articulos SET stock_base = stock_base + v_cant,
                       costo_base = v_nuevo_costo_base,
                       costo_compra = v_costo_base * COALESCE(NULLIF(v_art.factor_compra, 0), 1)
                 WHERE id = v_art.id;
                PERFORM recalcular_recetas_articulo(v_art.id);   -- recalcula solo si cambió
            ELSE
                UPDATE articulos SET stock_base = stock_base + v_cant WHERE id = v_art.id;
            END IF;
        END IF;
        INSERT INTO compra_detalle(compra_id, articulo_id, cantidad_base, costo_por_base, subtotal, vencimiento, created_by)
        VALUES (v_compra_id, (v_item->>'articulo_id')::int, v_cant, v_costo_base, v_subtotal, v_venc, p_usuario);
    END LOOP;
    RETURN v_compra_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── 9) confirmar_cierre_semanal — snapshot congelado (Req 6) ────────────────
-- Inserta el snapshot de la semana. Si ya existe (mismo local + semana_inicio),
-- no lo re-cierra (ON CONFLICT DO NOTHING).
CREATE OR REPLACE FUNCTION confirmar_cierre_semanal(
    p_semana_inicio DATE, p_semana_fin DATE,
    p_ventas NUMERIC, p_variable NUMERIC, p_food_teorico NUMERIC,
    p_mano_obra NUMERIC, p_fijos NUMERIC, p_resultado NUMERIC,
    p_food_pct NUMERIC, p_labor_pct NUMERIC, p_break_even NUMERIC,
    p_margen NUMERIC, p_usuario UUID, p_local_id INTEGER DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE v_id INTEGER;
BEGIN
    INSERT INTO cierres_semanales(
        semana_inicio, semana_fin, ventas_cobradas, costo_variable, food_teorico,
        mano_obra_pagada, fijos_prorrateados, resultado, food_pct, labor_pct,
        break_even, margen_contribucion, estado, local_id, closed_at, created_by)
    VALUES (p_semana_inicio, p_semana_fin, p_ventas, p_variable, p_food_teorico,
        p_mano_obra, p_fijos, p_resultado, p_food_pct, p_labor_pct,
        p_break_even, p_margen, 'CERRADO', p_local_id, now(), p_usuario)
    ON CONFLICT (local_id, semana_inicio) DO NOTHING
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
