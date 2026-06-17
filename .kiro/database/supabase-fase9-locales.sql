-- ============================================================================
-- ToppisERP - Fase 9 (Capa 1): Locales + columnas local_id
-- ============================================================================
-- Registro de locales y columnas local_id (nullable) en tablas transaccionales.
-- Nullable para no romper datos existentes. Ejecutar en Supabase SQL Editor.
-- ============================================================================

CREATE TABLE IF NOT EXISTS locales (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    direccion TEXT NOT NULL DEFAULT '',
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS update_locales_updated_at ON locales;
CREATE TRIGGER update_locales_updated_at BEFORE UPDATE ON locales
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE locales ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven locales" ON locales;
CREATE POLICY "Todos ven locales" ON locales FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan locales" ON locales;
CREATE POLICY "Solo admins gestionan locales" ON locales FOR ALL USING (get_user_rol() = 'ADMIN');

-- Columnas local_id (nullable) en tablas transaccionales
ALTER TABLE ventas       ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE gastos       ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE compras      ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE mermas       ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE conteos      ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE arqueos      ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE jornadas     ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;
ALTER TABLE propinas     ADD COLUMN IF NOT EXISTS local_id INTEGER REFERENCES locales(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ventas_local ON ventas(local_id);
CREATE INDEX IF NOT EXISTS idx_gastos_local ON gastos(local_id);
CREATE INDEX IF NOT EXISTS idx_compras_local ON compras(local_id);
