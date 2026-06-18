-- ============================================================================
-- ToppisERP - Roles v2: ADMIN | ADMIN_LOCAL | SUPERVISOR | CAJERO
-- ============================================================================
-- Amplía el enum `rol` con dos roles nuevos. Los usuarios existentes
-- (ADMIN / CAJERO) no se ven afectados.
-- Ejecutar en Supabase SQL Editor.
-- NOTA: ALTER TYPE ... ADD VALUE debe ejecutarse fuera de un bloque de
--       transacción (el SQL Editor lo hace automáticamente).
-- ============================================================================

ALTER TYPE rol ADD VALUE IF NOT EXISTS 'ADMIN_LOCAL';
ALTER TYPE rol ADD VALUE IF NOT EXISTS 'SUPERVISOR';

-- Verificación
SELECT unnest(enum_range(NULL::rol)) AS roles_disponibles;
