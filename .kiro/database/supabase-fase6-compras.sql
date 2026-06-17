-- ============================================================================
-- ToppisERP - Fase 6: Compras / Recepción de mercadería
-- ============================================================================
-- Suma stock, recalcula costo promedio ponderado y guarda caducidad por lote.
-- Opcionalmente registra un gasto (con IVA) y descuenta de un sobre.
-- Ejecutar después de supabase-fase6-proveedores.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS compras (
    id SERIAL PRIMARY KEY,
    proveedor_id INTEGER REFERENCES proveedores(id) ON DELETE SET NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    tiene_iva BOOLEAN NOT NULL DEFAULT false,
    nota TEXT NOT NULL DEFAULT '',
    gasto_id BIGINT REFERENCES gastos(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS compra_detalle (
    id SERIAL PRIMARY KEY,
    compra_id INTEGER NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    articulo_id INTEGER NOT NULL REFERENCES articulos(id) ON DELETE RESTRICT,
    cantidad_base NUMERIC(14,4) NOT NULL,        -- en unidad base
    costo_por_base NUMERIC(14,6) NOT NULL,       -- costo por unidad base (bruto, sin rendimiento)
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
    vencimiento DATE,                            -- caducidad del lote (opcional)
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_compras_fecha ON compras(fecha);
CREATE INDEX IF NOT EXISTS idx_compra_detalle_compra ON compra_detalle(compra_id);
CREATE INDEX IF NOT EXISTS idx_compra_detalle_venc ON compra_detalle(vencimiento);

DROP TRIGGER IF EXISTS update_compras_updated_at ON compras;
CREATE TRIGGER update_compras_updated_at BEFORE UPDATE ON compras
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
DROP TRIGGER IF EXISTS update_compra_detalle_updated_at ON compra_detalle;
CREATE TRIGGER update_compra_detalle_updated_at BEFORE UPDATE ON compra_detalle
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE compras ENABLE ROW LEVEL SECURITY;
ALTER TABLE compra_detalle ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven compras" ON compras;
CREATE POLICY "Todos ven compras" ON compras FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan compras" ON compras;
CREATE POLICY "Solo admins gestionan compras" ON compras FOR ALL USING (get_user_rol() = 'ADMIN');
DROP POLICY IF EXISTS "Todos ven compra_detalle" ON compra_detalle;
CREATE POLICY "Todos ven compra_detalle" ON compra_detalle FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan compra_detalle" ON compra_detalle;
CREATE POLICY "Solo admins gestionan compra_detalle" ON compra_detalle FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── RPC: registrar compra (stock + costo promedio + lote + gasto opcional) ────
CREATE OR REPLACE FUNCTION registrar_compra(
    p_proveedor_id INTEGER,
    p_tiene_iva BOOLEAN,
    p_nota TEXT,
    p_items JSONB,           -- [{articulo_id, cantidad_base, costo_por_base, vencimiento}]
    p_sobre_id INTEGER,      -- NULL = no descuenta dinero
    p_usuario UUID
)
RETURNS INTEGER AS $$
DECLARE
    v_total NUMERIC := 0;
    v_compra_id INTEGER;
    v_item JSONB;
    v_art RECORD;
    v_cant NUMERIC;
    v_costo_base NUMERIC;
    v_subtotal NUMERIC;
    v_bruto_actual NUMERIC;
    v_nuevo_bruto NUMERIC;
    v_venc DATE;
    v_gasto_id BIGINT := NULL;
    v_neto NUMERIC;
    v_iva NUMERIC;
BEGIN
    -- Total de la compra
    SELECT COALESCE(SUM((it->>'cantidad_base')::numeric * (it->>'costo_por_base')::numeric), 0)
        INTO v_total FROM jsonb_array_elements(p_items) it;

    -- Si descuenta de un sobre, registrar gasto primero (valida saldo)
    IF p_sobre_id IS NOT NULL AND v_total > 0 THEN
        IF p_tiene_iva THEN
            v_neto := round(v_total / 1.19);
            v_iva := v_total - v_neto;
        ELSE
            v_neto := v_total; v_iva := 0;
        END IF;
        PERFORM 1 FROM sobres WHERE id = p_sobre_id FOR UPDATE;
        IF (SELECT saldo FROM sobres WHERE id = p_sobre_id) < v_total THEN
            RAISE EXCEPTION 'Saldo insuficiente en el sobre para la compra';
        END IF;
        INSERT INTO gastos(descripcion, monto, categoria, sobre_id, usuario_id, fecha,
                           comprobante, tiene_iva, monto_neto, monto_iva, created_by)
        VALUES ('Compra de mercadería', v_total, 'INSUMOS', p_sobre_id, p_usuario, now(),
                '', p_tiene_iva, v_neto, v_iva, p_usuario)
        RETURNING id INTO v_gasto_id;
        UPDATE sobres SET saldo = saldo - v_total WHERE id = p_sobre_id;
        INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
        VALUES (p_sobre_id, NULL, v_total, 'EGRESO', 'Compra de mercadería', p_usuario);
    END IF;

    INSERT INTO compras(proveedor_id, fecha, total, tiene_iva, nota, gasto_id, created_by)
    VALUES (p_proveedor_id, now(), v_total, p_tiene_iva, COALESCE(p_nota, ''), v_gasto_id, p_usuario)
    RETURNING id INTO v_compra_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        v_cant := (v_item->>'cantidad_base')::numeric;
        v_costo_base := (v_item->>'costo_por_base')::numeric;  -- bruto
        v_subtotal := v_cant * v_costo_base;
        v_venc := NULLIF(v_item->>'vencimiento', '')::date;

        SELECT * INTO v_art FROM articulos WHERE id = (v_item->>'articulo_id')::int FOR UPDATE;
        IF v_art.id IS NOT NULL THEN
            -- costo promedio ponderado (sobre costo bruto, sin rendimiento)
            v_bruto_actual := v_art.costo_base * COALESCE(NULLIF(v_art.rendimiento,0), 1);
            IF (v_art.stock_base + v_cant) > 0 THEN
                v_nuevo_bruto := (v_art.stock_base * v_bruto_actual + v_cant * v_costo_base)
                                 / (v_art.stock_base + v_cant);
            ELSE
                v_nuevo_bruto := v_costo_base;
            END IF;

            UPDATE articulos
               SET stock_base = stock_base + v_cant,
                   costo_base = v_nuevo_bruto / COALESCE(NULLIF(v_art.rendimiento,0), 1)
             WHERE id = v_art.id;
        END IF;

        INSERT INTO compra_detalle(compra_id, articulo_id, cantidad_base, costo_por_base, subtotal, vencimiento, created_by)
        VALUES (v_compra_id, (v_item->>'articulo_id')::int, v_cant, v_costo_base, v_subtotal, v_venc, p_usuario);
    END LOOP;

    RETURN v_compra_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── Realtime ─────────────────────────────────────────────────────────────────
DO $$
BEGIN
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE compras; EXCEPTION WHEN duplicate_object THEN NULL; END;
END $$;
