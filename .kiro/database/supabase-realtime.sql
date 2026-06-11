-- ============================================================================
-- ToppisERP - Realtime Configuration
-- Versión: 1.0
-- Fecha: 2026-06-08
-- Proyecto: https://dkgqrbxizegipxdsypzf.supabase.co
-- ============================================================================
-- IMPORTANTE: Ejecutar DESPUÉS de supabase-schema.sql y supabase-rls.sql
-- ============================================================================

-- ============================================================================
-- HABILITAR REALTIME EN TABLAS CRÍTICAS
-- ============================================================================

-- Inventario: actualizaciones de stock en tiempo real
ALTER PUBLICATION supabase_realtime ADD TABLE insumos;
ALTER PUBLICATION supabase_realtime ADD TABLE ingredientes;

-- Menú: cambios de precios/items visibles instantáneamente
ALTER PUBLICATION supabase_realtime ADD TABLE items_menu;

-- Ventas: ver ventas de otros cajeros al instante
ALTER PUBLICATION supabase_realtime ADD TABLE ventas;

-- Sobres: saldos actualizados en vivo
ALTER PUBLICATION supabase_realtime ADD TABLE sobres;

-- Comandas: cocina ve órdenes nuevas sin refresh
ALTER PUBLICATION supabase_realtime ADD TABLE comandas;

-- ============================================================================
-- VERIFICACIÓN: Listar tablas con Realtime habilitado
-- ============================================================================

SELECT * FROM pg_publication_tables 
WHERE pubname = 'supabase_realtime'
ORDER BY tablename;

-- Deberías ver 6 tablas:
-- - comandas
-- - insumos
-- - ingredientes
-- - items_menu
-- - sobres
-- - ventas

-- ============================================================================
-- FIN DE CONFIGURACIÓN REALTIME
-- ============================================================================
