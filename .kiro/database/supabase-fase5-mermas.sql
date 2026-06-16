-- ============================================================================
-- ToppisERP - Fase 5: Mermas (waste log)
-- ============================================================================
-- Registro de pérdidas con motivo que descuentan stock.
-- Ejecutar en Supabase SQL Editor (después de los scripts de Fase 4).
-- ============================================================================

CREATE TABLE IF NOT EXISTS mermas (
    id SERIAL PRIMARY KEY,
    tipo_componente tipo_componente NOT NULL,    -- ARTICULO | PREPARACION
    componente_id INTEGER NOT NULL,
    cantidad_base NUMERIC(14,4) NOT NULL CHECK (cantidad_base > 0),
    motivo TEXT NOT NULL CHECK (motivo IN (
        'VENCIDO', 'ESTROPEADO', 'VINO_MALO', 'ERROR_COCINA', 'CORTESIA', 'ROBO', 'OTRO'
    )),
    costo NUMERIC(12,2) NOT NULL DEFAULT 0,
    nota TEXT NOT NULL DEFAULT '',
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_mermas_fecha ON mermas(fecha);
CREATE INDEX IF NOT EXISTS idx_mermas_motivo ON mermas(motivo);
CREATE INDEX IF NOT EXISTS idx_mermas_componente ON mermas(componente_id, tipo_componente);

DROP TRIGGER IF EXISTS update_mermas_updated_at ON mermas;
CREATE TRIGGER update_mermas_updated_at
    BEFORE UPDATE ON mermas FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE mermas ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven mermas" ON mermas;
CREATE POLICY "Todos ven mermas" ON mermas FOR SELECT USING (true);
DROP POLICY IF EXISTS "Autenticados registran mermas" ON mermas;
CREATE POLICY "Autenticados registran mermas" ON mermas FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
DROP POLICY IF EXISTS "Solo admins modifican mermas" ON mermas;
CREATE POLICY "Solo admins modifican mermas" ON mermas FOR UPDATE USING (get_user_rol() = 'ADMIN');
DROP POLICY IF EXISTS "Solo admins eliminan mermas" ON mermas;
CREATE POLICY "Solo admins eliminan mermas" ON mermas FOR DELETE USING (get_user_rol() = 'ADMIN');

-- ── RPC: registrar merma (atómico: inserta + descuenta stock) ─────────────────
CREATE OR REPLACE FUNCTION registrar_merma(
    p_tipo TEXT,
    p_componente_id INTEGER,
    p_cantidad NUMERIC,
    p_motivo TEXT,
    p_nota TEXT,
    p_usuario UUID
)
RETURNS INTEGER AS $$
DECLARE
    v_costo_base NUMERIC := 0;
    v_costo NUMERIC := 0;
    v_id INTEGER;
BEGIN
    IF p_tipo = 'ARTICULO' THEN
        SELECT costo_base INTO v_costo_base FROM articulos WHERE id = p_componente_id;
        UPDATE articulos SET stock_base = stock_base - p_cantidad WHERE id = p_componente_id;
    ELSIF p_tipo = 'PREPARACION' THEN
        SELECT costo_base INTO v_costo_base FROM preparaciones WHERE id = p_componente_id;
        UPDATE preparaciones SET stock_base = stock_base - p_cantidad WHERE id = p_componente_id;
    END IF;

    v_costo := COALESCE(v_costo_base, 0) * p_cantidad;

    INSERT INTO mermas(tipo_componente, componente_id, cantidad_base, motivo, costo, nota, created_by)
    VALUES (p_tipo::tipo_componente, p_componente_id, p_cantidad, p_motivo, v_costo, COALESCE(p_nota, ''), p_usuario)
    RETURNING id INTO v_id;

    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── Realtime ─────────────────────────────────────────────────────────────────
DO $$
BEGIN
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE mermas; EXCEPTION WHEN duplicate_object THEN NULL; END;
END $$;
