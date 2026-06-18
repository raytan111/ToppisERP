-- ============================================================================
-- ToppisERP - Fase 9 (Capa 4): Roles por local
-- ============================================================================
-- Asigna usuarios a locales con un rol local (ENCARGADO, FRANQUICIADO, ADMIN_LOCAL).
-- Un ADMIN global ve todos los locales; un encargado/franquiciado solo su(s) local(es).
-- Ejecutar DESPUÉS de supabase-fase9-locales.sql
-- ============================================================================

-- Tabla de asignación usuario-local
CREATE TABLE IF NOT EXISTS usuarios_locales (
    id SERIAL PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    local_id INTEGER NOT NULL REFERENCES locales(id) ON DELETE CASCADE,
    rol_local TEXT NOT NULL DEFAULT 'ENCARGADO' CHECK (rol_local IN ('ADMIN_LOCAL', 'ENCARGADO', 'FRANQUICIADO')),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    UNIQUE(usuario_id, local_id)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_locales_usuario ON usuarios_locales(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_locales_local ON usuarios_locales(local_id);

DROP TRIGGER IF EXISTS update_usuarios_locales_updated_at ON usuarios_locales;
CREATE TRIGGER update_usuarios_locales_updated_at BEFORE UPDATE ON usuarios_locales
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE usuarios_locales ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven asignaciones" ON usuarios_locales;
CREATE POLICY "Todos ven asignaciones" ON usuarios_locales FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan asignaciones" ON usuarios_locales;
CREATE POLICY "Solo admins gestionan asignaciones" ON usuarios_locales FOR ALL USING (get_user_rol() = 'ADMIN');

-- Helper: locales asignados a un usuario (para filtrar en la app)
CREATE OR REPLACE FUNCTION locales_del_usuario(p_usuario UUID)
RETURNS TABLE(local_id INTEGER, rol_local TEXT) AS $$
    SELECT ul.local_id, ul.rol_local
    FROM usuarios_locales ul
    WHERE ul.usuario_id = p_usuario AND ul.activo = true;
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;
