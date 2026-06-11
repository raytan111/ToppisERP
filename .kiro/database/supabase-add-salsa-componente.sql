-- ============================================================================
-- ToppisERP - Permitir SALSA como componente de receta
-- ============================================================================
-- Algunas recetas incluyen salsas. Agregamos 'SALSA' al enum tipo_componente.
-- ============================================================================

ALTER TYPE tipo_componente ADD VALUE IF NOT EXISTS 'SALSA';
