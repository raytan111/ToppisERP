-- ============================================================================
-- ToppisERP - Agregar columna cantidad_comprada a ingredientes
-- ============================================================================
-- La "cantidad comprada" se usaba solo para derivar cantidad_aprovechable y
-- costo_gramo, pero no se guardaba. Ahora se persiste para edición fiel.
-- Usamos ALTER (no DROP) para conservar los ingredientes ya creados.
-- ============================================================================

ALTER TABLE ingredientes
    ADD COLUMN IF NOT EXISTS cantidad_comprada NUMERIC(12, 2) NOT NULL DEFAULT 0;
