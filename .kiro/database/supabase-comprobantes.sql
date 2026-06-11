-- ============================================================================
-- ToppisERP - Fase 2A: Comprobantes internos + provisión de IVA
-- ============================================================================
-- Documento de control interno (NO tributario) por cada venta.
-- Calcula neto/IVA (19%) y asigna folio correlativo interno.
-- Tabla preparada para emisión real ante el SII en Fase 2B.
-- ============================================================================

CREATE TABLE IF NOT EXISTS comprobantes (
    id SERIAL PRIMARY KEY,
    venta_id INTEGER REFERENCES ventas(id) ON DELETE SET NULL,
    folio INTEGER NOT NULL,
    tipo TEXT NOT NULL DEFAULT 'COMPROBANTE_INTERNO',
    neto NUMERIC(12, 2) NOT NULL,
    iva NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    estado TEXT NOT NULL DEFAULT 'INTERNO',   -- INTERNO, EMITIDO, RECHAZADO, ANULADO

    -- Campos preparados para emisión real ante el SII (Fase 2B)
    tipo_dte INTEGER,                 -- 39 = boleta, 33 = factura
    folio_sii INTEGER,
    rut_receptor TEXT,
    track_id TEXT,
    pdf_url TEXT,
    xml_url TEXT,
    respuesta_proveedor JSONB,

    fecha_emision TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,

    UNIQUE(venta_id),   -- un comprobante por venta
    UNIQUE(folio)       -- folio correlativo único
);

CREATE INDEX IF NOT EXISTS idx_comprobantes_venta ON comprobantes(venta_id);
CREATE INDEX IF NOT EXISTS idx_comprobantes_fecha ON comprobantes(fecha_emision);
CREATE INDEX IF NOT EXISTS idx_comprobantes_estado ON comprobantes(estado);

CREATE TRIGGER update_comprobantes_updated_at
    BEFORE UPDATE ON comprobantes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- RLS
ALTER TABLE comprobantes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Todos ven comprobantes"
ON comprobantes FOR SELECT USING (true);

CREATE POLICY "Usuarios autenticados crean comprobantes"
ON comprobantes FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Solo admins modifican comprobantes"
ON comprobantes FOR UPDATE USING (get_user_rol() = 'ADMIN');

-- Grants
GRANT SELECT, INSERT, UPDATE ON comprobantes TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE comprobantes_id_seq TO anon, authenticated;

-- Realtime
ALTER PUBLICATION supabase_realtime ADD TABLE comprobantes;

-- ============================================================================
-- Función: emitir comprobante interno para una venta
-- Calcula neto/IVA (precios IVA incluido) y asigna folio correlativo.
-- ============================================================================
CREATE OR REPLACE FUNCTION emitir_comprobante(
    p_venta_id INTEGER,
    p_usuario UUID
)
RETURNS JSONB AS $$
DECLARE
    v_total NUMERIC;
    v_neto NUMERIC;
    v_iva NUMERIC;
    v_folio INTEGER;
    v_id INTEGER;
    v_existente INTEGER;
BEGIN
    -- Evitar duplicado
    SELECT id INTO v_existente FROM comprobantes WHERE venta_id = p_venta_id;
    IF v_existente IS NOT NULL THEN
        RAISE EXCEPTION 'La venta % ya tiene comprobante', p_venta_id;
    END IF;

    SELECT total INTO v_total FROM ventas WHERE id = p_venta_id;
    IF v_total IS NULL THEN
        RAISE EXCEPTION 'Venta no encontrada';
    END IF;

    -- Precios con IVA incluido: neto = total / 1.19
    v_neto := round(v_total / 1.19);
    v_iva := v_total - v_neto;

    -- Folio correlativo interno
    SELECT COALESCE(MAX(folio), 0) + 1 INTO v_folio FROM comprobantes;

    INSERT INTO comprobantes(venta_id, folio, tipo, neto, iva, total, estado, fecha_emision, created_by)
    VALUES (p_venta_id, v_folio, 'COMPROBANTE_INTERNO', v_neto, v_iva, v_total, 'INTERNO', now(), p_usuario)
    RETURNING id INTO v_id;

    RETURN jsonb_build_object(
        'id', v_id,
        'folio', v_folio,
        'neto', v_neto,
        'iva', v_iva,
        'total', v_total
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
