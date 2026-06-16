-- ============================================================================
-- ToppisERP - Fase 5: Conteos de inventario (stock take)
-- ============================================================================
-- Conteo físico que ajusta el stock del sistema a la realidad.
-- Ejecutar en Supabase SQL Editor (después de Fase 4).
-- ============================================================================

CREATE TABLE IF NOT EXISTS conteos (
    id SERIAL PRIMARY KEY,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado TEXT NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO')),
    nota TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS conteo_detalle (
    id SERIAL PRIMARY KEY,
    conteo_id INTEGER NOT NULL REFERENCES conteos(id) ON DELETE CASCADE,
    articulo_id INTEGER NOT NULL REFERENCES articulos(id) ON DELETE CASCADE,
    stock_sistema NUMERIC(14,4) NOT NULL DEFAULT 0,
    stock_contado NUMERIC(14,4) NOT NULL DEFAULT 0,
    diferencia NUMERIC(14,4) NOT NULL DEFAULT 0,   -- contado - sistema
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_conteos_fecha ON conteos(fecha);
CREATE INDEX IF NOT EXISTS idx_conteo_detalle_conteo ON conteo_detalle(conteo_id);

DROP TRIGGER IF EXISTS update_conteos_updated_at ON conteos;
CREATE TRIGGER update_conteos_updated_at BEFORE UPDATE ON conteos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
DROP TRIGGER IF EXISTS update_conteo_detalle_updated_at ON conteo_detalle;
CREATE TRIGGER update_conteo_detalle_updated_at BEFORE UPDATE ON conteo_detalle
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE conteos ENABLE ROW LEVEL SECURITY;
ALTER TABLE conteo_detalle ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven conteos" ON conteos;
CREATE POLICY "Todos ven conteos" ON conteos FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan conteos" ON conteos;
CREATE POLICY "Solo admins gestionan conteos" ON conteos FOR ALL USING (get_user_rol() = 'ADMIN');
DROP POLICY IF EXISTS "Todos ven conteo_detalle" ON conteo_detalle;
CREATE POLICY "Todos ven conteo_detalle" ON conteo_detalle FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan conteo_detalle" ON conteo_detalle;
CREATE POLICY "Solo admins gestionan conteo_detalle" ON conteo_detalle FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── RPC: cerrar conteo (ajusta stock al valor contado) ────────────────────────
CREATE OR REPLACE FUNCTION cerrar_conteo(p_conteo_id INTEGER)
RETURNS VOID AS $$
DECLARE
    v_det RECORD;
BEGIN
    FOR v_det IN SELECT * FROM conteo_detalle WHERE conteo_id = p_conteo_id LOOP
        UPDATE articulos SET stock_base = v_det.stock_contado WHERE id = v_det.articulo_id;
    END LOOP;
    UPDATE conteos SET estado = 'CERRADO' WHERE id = p_conteo_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
