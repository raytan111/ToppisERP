-- ============================================================================
-- ToppisERP - Import histórico (PASO 1): columnas nuevas en "ventas"
-- ============================================================================
-- Prepara la tabla ventas para guardar ventas históricas en texto libre
-- (sin ítems de menú, sin descuento de stock) y para futuras ventas con más
-- contexto. Cambios aditivos, no rompen las ventas que crea la app.
--
-- Ejecutar en Supabase SQL Editor ANTES del clean slate y del import.
-- ============================================================================

ALTER TABLE ventas ADD COLUMN IF NOT EXISTS descripcion  TEXT;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS canal        TEXT;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS modo_entrega TEXT;   -- RETIRO | DELIVERY
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS origen       TEXT NOT NULL DEFAULT 'APP';
                                                                  -- APP | IMPORT_HISTORICO

-- Las ventas históricas no tienen sobre ni método de pago conocido:
-- aflojamos esas restricciones (la app sigue enviándolos en ventas normales).
ALTER TABLE ventas ALTER COLUMN sobre_id    DROP NOT NULL;
ALTER TABLE ventas ALTER COLUMN metodo_pago DROP NOT NULL;

-- Verificación
SELECT column_name, is_nullable, data_type
FROM information_schema.columns
WHERE table_name = 'ventas'
ORDER BY ordinal_position;
