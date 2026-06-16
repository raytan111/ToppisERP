-- ============================================================================
-- ToppisERP - Fase 4: cantidad_pos para salsas/agregados seleccionables
-- ============================================================================
-- Agrega la cantidad (en unidad base) que se consume cada vez que se elige
-- el artículo/preparación como salsa/agregado en el POS.
-- Ejecutar en Supabase SQL Editor. Es ADD COLUMN IF NOT EXISTS (no borra datos).
-- ============================================================================

ALTER TABLE articulos     ADD COLUMN IF NOT EXISTS cantidad_pos NUMERIC(14,4) NOT NULL DEFAULT 0;
ALTER TABLE preparaciones ADD COLUMN IF NOT EXISTS cantidad_pos NUMERIC(14,4) NOT NULL DEFAULT 0;
