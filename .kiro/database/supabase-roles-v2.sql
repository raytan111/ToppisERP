-- ============================================================================
-- ToppisERP - Roles v2: ADMIN | ADMIN_LOCAL | SUPERVISOR | CAJERO
-- ============================================================================
-- Amplía el enum `rol` con dos roles nuevos. Los usuarios existentes
-- (ADMIN / CAJERO) no se ven afectados.
--
-- ⚠️  IMPORTANTE: un valor nuevo de enum debe estar COMMITEADO antes de poder
--     usarse. Por eso este PASO 1 contiene SOLO los ALTER TYPE (sin SELECT que
--     use los valores nuevos). Ejecutá el PASO 1 solo; luego, si querés
--     verificar, ejecutá el PASO 2 en una corrida aparte.
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────
-- PASO 1 — Ejecutar esto SOLO (seleccioná estas dos líneas y corré):
-- ─────────────────────────────────────────────────────────────────────────
ALTER TYPE rol ADD VALUE IF NOT EXISTS 'ADMIN_LOCAL';
ALTER TYPE rol ADD VALUE IF NOT EXISTS 'SUPERVISOR';

-- ─────────────────────────────────────────────────────────────────────────
-- PASO 2 — Verificación (ejecutar en una corrida SEPARADA, después del PASO 1):
-- ─────────────────────────────────────────────────────────────────────────
-- SELECT unnest(enum_range(NULL::rol)) AS roles_disponibles;
-- Debería listar: ADMIN, CAJERO, ADMIN_LOCAL, SUPERVISOR
