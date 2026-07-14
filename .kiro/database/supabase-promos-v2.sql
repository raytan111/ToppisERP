-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — Promociones v2 (Fase A): repetición por grupo
-- EJECUTAR en el SQL Editor de Supabase. Idempotente.
-- ════════════════════════════════════════════════════════════════════════

-- permite_repetir: si un grupo con cantidad > 1 permite elegir el mismo producto
-- más de una vez (ej. 2 Cheese iguales) o exige productos distintos.
ALTER TABLE promocion_espacios
    ADD COLUMN IF NOT EXISTS permite_repetir BOOLEAN NOT NULL DEFAULT TRUE;
