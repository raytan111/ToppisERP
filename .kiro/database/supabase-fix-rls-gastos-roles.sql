-- ============================================================================
-- ToppisERP - Fix RLS de "gastos" para roles nuevos (ADMIN_LOCAL/SUPERVISOR)
-- ============================================================================
-- Las políticas originales solo contemplaban ADMIN y CAJERO. Esto alinea la
-- visibilidad de gastos con los roles actuales:
--   - ADMIN y ADMIN_LOCAL: ven todos los gastos.
--   - CAJERO y SUPERVISOR: ven solo los gastos que ellos registraron.
--   - Crear: cualquier autenticado. Editar/eliminar: ADMIN y ADMIN_LOCAL.
-- Ejecutar en Supabase SQL Editor.
-- ============================================================================

DROP POLICY IF EXISTS "Admins ven todos los gastos" ON gastos;
DROP POLICY IF EXISTS "Cajeros ven sus gastos" ON gastos;
DROP POLICY IF EXISTS "Solo admins modifican gastos" ON gastos;
DROP POLICY IF EXISTS "Solo admins eliminan gastos" ON gastos;

CREATE POLICY "Admins ven todos los gastos"
ON gastos FOR SELECT
USING (get_user_rol() IN ('ADMIN', 'ADMIN_LOCAL'));

CREATE POLICY "No-admins ven sus gastos"
ON gastos FOR SELECT
USING (get_user_rol() IN ('CAJERO', 'SUPERVISOR') AND usuario_id = auth.uid());

CREATE POLICY "Admins modifican gastos"
ON gastos FOR UPDATE
USING (get_user_rol() IN ('ADMIN', 'ADMIN_LOCAL'));

CREATE POLICY "Admins eliminan gastos"
ON gastos FOR DELETE
USING (get_user_rol() IN ('ADMIN', 'ADMIN_LOCAL'));

-- La política de INSERT "Usuarios autenticados crean gastos" se conserva.
