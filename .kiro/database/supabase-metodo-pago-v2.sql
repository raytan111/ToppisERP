-- ============================================================================
-- ToppisERP - Limpieza de ventas de prueba + Método de pago v2
-- ============================================================================
-- 1) Las ventas de prueba tienen método de pago marcado (DEBITO/EFECTIVO);
--    las históricas importadas tienen metodo_pago NULL. Borramos las de prueba.
-- 2) Cambiamos el enum metodo_pago a: EFECTIVO | TARJETA | TRANSFERENCIA
--    (se renombra DEBITO -> TARJETA y se agrega TRANSFERENCIA).
-- Ejecutar en Supabase SQL Editor.
-- ============================================================================

-- ── PASO 1: borrar ventas de prueba (las que tienen método de pago) ──────────
-- Previsualizar primero (opcional):
-- SELECT id, fecha, total, metodo_pago, origen FROM ventas WHERE metodo_pago IS NOT NULL ORDER BY id;

DELETE FROM ventas WHERE metodo_pago IS NOT NULL;

-- ── PASO 2: actualizar el enum metodo_pago ───────────────────────────────────
-- Renombrar DEBITO -> TARJETA (convierte cualquier fila existente automáticamente).
ALTER TYPE metodo_pago RENAME VALUE 'DEBITO' TO 'TARJETA';

-- Agregar TRANSFERENCIA. (Si da error por transacción, ejecutá esta línea sola.)
ALTER TYPE metodo_pago ADD VALUE IF NOT EXISTS 'TRANSFERENCIA';

-- Verificación
SELECT unnest(enum_range(NULL::metodo_pago)) AS metodos_disponibles;
-- Esperado: EFECTIVO, TARJETA, TRANSFERENCIA
