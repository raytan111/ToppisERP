-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — ÍNDICES recomendados (auditoría 2026-07-09)
-- ════════════════════════════════════════════════════════════════════════
-- Ejecutar en el SQL Editor de Supabase. Es seguro y opcional.
-- Añade índices en las claves foráneas que participan en JOINs / filtros /
-- borrados en cascada. Con tu volumen actual el impacto es chico, pero deja
-- la base preparada para crecer.
--
-- NOTA: NO indexamos las columnas `created_by` (son de auditoría, casi nunca
-- se filtran) para no penalizar las escrituras con índices que no se usan.
-- ════════════════════════════════════════════════════════════════════════

-- ── FKs de JOIN frecuente (recetas, compras, conteos, promos) ───────────────
CREATE INDEX IF NOT EXISTS idx_compra_detalle_articulo   ON compra_detalle(articulo_id);
CREATE INDEX IF NOT EXISTS idx_conteo_detalle_articulo   ON conteo_detalle(articulo_id);
CREATE INDEX IF NOT EXISTS idx_compras_proveedor         ON compras(proveedor_id);
CREATE INDEX IF NOT EXISTS idx_compras_gasto             ON compras(gasto_id);
CREATE INDEX IF NOT EXISTS idx_promocion_items_item_menu ON promocion_items(item_menu_id);

-- ── FKs por local (filtrado multi-local; barato y a futuro) ─────────────────
CREATE INDEX IF NOT EXISTS idx_arqueos_local      ON arqueos(local_id);
CREATE INDEX IF NOT EXISTS idx_conteos_local      ON conteos(local_id);
CREATE INDEX IF NOT EXISTS idx_jornadas_local     ON jornadas(local_id);
CREATE INDEX IF NOT EXISTS idx_mermas_local       ON mermas(local_id);
CREATE INDEX IF NOT EXISTS idx_propinas_local     ON propinas(local_id);
CREATE INDEX IF NOT EXISTS idx_costos_fijos_local ON costos_fijos(local_id);

-- ── Refrescar estadísticas del planificador (arregla el "filas_aprox = -1") ─
ANALYZE;
