-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — LIMPIEZA de overloads (auditoría 2026-07-09)
-- ════════════════════════════════════════════════════════════════════════
-- Ejecutar en el SQL Editor de Supabase.
--
-- Resultado de la auditoría: la ÚNICA función duplicada es `registrar_gasto`.
-- Conviven dos versiones:
--   • 6 args (VIEJA): sin p_tiene_iva ni p_local_id  → NO calcula IVA ni sella local.
--   • 8 args (VIGENTE, de fase9-stamping): con p_tiene_iva DEFAULT false
--                                          y p_local_id DEFAULT NULL.
--
-- La app siempre envía p_tiene_iva, por lo que usa la de 8 args. La de 6 es
-- un residuo (fase9-stamping intentó borrar una firma de 7 args, no la de 6).
-- Este script elimina SOLO la versión vieja de 6 argumentos.
-- La versión de 8 args (con sus DEFAULT) queda intacta y cubre todos los casos.
-- ════════════════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS registrar_gasto(TEXT, NUMERIC, TEXT, INTEGER, UUID, TEXT);

-- Verificación: debe quedar UNA sola fila (la de 8 argumentos).
SELECT p.proname                                   AS funcion,
       pg_get_function_identity_arguments(p.oid)   AS argumentos
FROM   pg_proc p
JOIN   pg_namespace n ON n.oid = p.pronamespace
WHERE  n.nspname = 'public'
  AND  p.proname = 'registrar_gasto';
