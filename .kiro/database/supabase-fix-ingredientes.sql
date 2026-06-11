-- ============================================================================
-- ToppisERP - Corrección tabla ingredientes
-- ============================================================================
-- El modelo real de la app tiene campos de costo y merma que el schema inicial
-- no contemplaba. Este script recrea la tabla con el modelo correcto.
-- IMPORTANTE: La tabla ingredientes está vacía, es seguro recrearla.
-- ============================================================================

DROP TABLE IF EXISTS ingredientes CASCADE;

CREATE TABLE ingredientes (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    unidad_medida TEXT NOT NULL,
    stock_actual NUMERIC(12, 2) NOT NULL DEFAULT 0,
    costo_unitario NUMERIC(12, 4) NOT NULL DEFAULT 0,
    costo_compra NUMERIC(12, 2) NOT NULL DEFAULT 0,
    porcentaje_merma NUMERIC(6, 2) NOT NULL DEFAULT 0,
    unidad_compra TEXT NOT NULL DEFAULT '',
    cantidad_aprovechable NUMERIC(12, 2) NOT NULL DEFAULT 0,
    costo_gramo NUMERIC(12, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_ingredientes_nombre ON ingredientes(nombre);

CREATE TRIGGER update_ingredientes_updated_at
    BEFORE UPDATE ON ingredientes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- RLS: todos ven, solo admins modifican
ALTER TABLE ingredientes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Todos ven ingredientes"
ON ingredientes FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican ingredientes"
ON ingredientes FOR ALL
USING (get_user_rol() = 'ADMIN');

-- Grants para los roles del Data API
GRANT SELECT, INSERT, UPDATE, DELETE ON ingredientes TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE ingredientes_id_seq TO anon, authenticated;

-- Re-agregar a la publicación Realtime
ALTER PUBLICATION supabase_realtime ADD TABLE ingredientes;
