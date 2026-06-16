-- ============================================================================
-- ToppisERP - Fase 4: Realtime Configuration
-- Versión: 4.0
-- Fecha: 2026-06-15
-- ============================================================================
-- Ejecutar DESPUÉS de supabase-fase4-schema.sql y supabase-fase4-rls.sql
-- ============================================================================

-- Agregar tablas nuevas a la publicación realtime.
-- (Se envuelve en bloque para ignorar si ya estaban agregadas.)
DO $$
BEGIN
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE articulos; EXCEPTION WHEN duplicate_object THEN NULL; END;
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE preparaciones; EXCEPTION WHEN duplicate_object THEN NULL; END;
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE modificadores; EXCEPTION WHEN duplicate_object THEN NULL; END;
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE promociones; EXCEPTION WHEN duplicate_object THEN NULL; END;
END $$;

-- Verificación
SELECT tablename FROM pg_publication_tables
WHERE pubname = 'supabase_realtime'
ORDER BY tablename;

-- ============================================================================
-- FIN REALTIME FASE 4
-- ============================================================================
