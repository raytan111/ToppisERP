-- ============================================================================
-- ToppisERP - Fase 6: Proveedores
-- ============================================================================
-- Ejecutar en Supabase SQL Editor (después de Fase 4/5).
-- ============================================================================

CREATE TABLE IF NOT EXISTS proveedores (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    contacto TEXT NOT NULL DEFAULT '',
    telefono TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    nota TEXT NOT NULL DEFAULT '',
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_proveedores_nombre ON proveedores(nombre);

DROP TRIGGER IF EXISTS update_proveedores_updated_at ON proveedores;
CREATE TRIGGER update_proveedores_updated_at BEFORE UPDATE ON proveedores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE proveedores ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven proveedores" ON proveedores;
CREATE POLICY "Todos ven proveedores" ON proveedores FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan proveedores" ON proveedores;
CREATE POLICY "Solo admins gestionan proveedores" ON proveedores FOR ALL USING (get_user_rol() = 'ADMIN');
