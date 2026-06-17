-- ============================================================================
-- ToppisERP - Fase 5: Variance / Consumo teórico
-- ============================================================================
-- Consumo teórico por artículo/preparación según las ventas de un período
-- (lo que las recetas dicen que se debió consumir). Se compara contra la
-- merma registrada y los ajustes de conteo para detectar fugas.
-- Ejecutar en Supabase SQL Editor (después de Fase 4 y mermas/conteos).
-- ============================================================================

CREATE OR REPLACE FUNCTION consumo_teorico_periodo(
    p_desde TIMESTAMPTZ,
    p_hasta TIMESTAMPTZ
)
RETURNS TABLE(tipo TEXT, componente_id INTEGER, cantidad NUMERIC, costo NUMERIC) AS $$
    SELECT
        r.tipo_componente::text AS tipo,
        r.componente_id,
        SUM(r.cantidad_base * ivm.cantidad) AS cantidad,
        SUM(
            r.cantidad_base * ivm.cantidad *
            CASE WHEN r.tipo_componente = 'ARTICULO'
                 THEN COALESCE(a.costo_base, 0)
                 ELSE COALESCE(p.costo_base, 0) END
        ) AS costo
    FROM items_venta_menu ivm
    JOIN ventas v
        ON v.id = ivm.venta_id
        AND v.estado = 'COMPLETADA'
        AND v.fecha >= p_desde
        AND v.fecha < p_hasta
    JOIN recetas_menu r
        ON r.item_menu_id = ivm.item_menu_id
    LEFT JOIN articulos a
        ON r.tipo_componente = 'ARTICULO' AND a.id = r.componente_id
    LEFT JOIN preparaciones p
        ON r.tipo_componente = 'PREPARACION' AND p.id = r.componente_id
    GROUP BY r.tipo_componente, r.componente_id;
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public;
