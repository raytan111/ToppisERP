-- ============================================================================
-- ToppisERP - Corregir política de inserción de usuarios
-- ============================================================================
-- Al crear un usuario con signUp, Supabase cambia la sesión al usuario nuevo,
-- por lo que el INSERT del perfil corre como ese usuario (aún sin rol ADMIN).
-- Permitimos que un usuario inserte SU PROPIO perfil (id = auth.uid()),
-- además de los admins.
-- ============================================================================

DROP POLICY IF EXISTS "Solo admins crean usuarios" ON usuarios;

CREATE POLICY "Crear perfil propio o admin"
ON usuarios FOR INSERT
WITH CHECK (
    id = auth.uid()              -- el propio usuario inserta su perfil (flujo signUp)
    OR get_user_rol() = 'ADMIN'  -- o un admin crea usuarios
);
