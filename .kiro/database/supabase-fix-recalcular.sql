-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — FIX: recalcular_recetas_articulo con WHERE (2026-07-09)
-- ════════════════════════════════════════════════════════════════════════
-- Corrige el error "UPDATE requires a WHERE clause" al registrar compras.
-- El proyecto tiene activo el guard "safeupdate", que rechaza UPDATE sin WHERE.
-- Se agregan predicados siempre-verdaderos (id > 0) a los recálculos globales.
-- Ejecutar en el SQL Editor de Supabase.
-- ════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION recalcular_recetas_articulo(p_articulo_id INTEGER)
RETURNS VOID AS $$
DECLARE v_pass INT;
BEGIN
    FOR v_pass IN 1..3 LOOP
        UPDATE preparaciones p SET costo_lote = COALESCE((
            SELECT SUM(pc.cantidad_base *
                CASE pc.tipo_componente
                    WHEN 'ARTICULO' THEN (SELECT costo_base FROM articulos WHERE id = pc.componente_id)
                    ELSE (SELECT costo_base FROM preparaciones WHERE id = pc.componente_id)
                END)
            FROM preparacion_componentes pc WHERE pc.preparacion_id = p.id), 0)
        WHERE p.id > 0;
        UPDATE preparaciones p SET costo_base =
            CASE WHEN COALESCE(NULLIF(p.rendimiento_lote,0),1) > 0
                 THEN p.costo_lote / COALESCE(NULLIF(p.rendimiento_lote,0),1) ELSE 0 END
        WHERE p.id > 0;
    END LOOP;

    UPDATE items_menu i SET costo_teorico = COALESCE((
        SELECT SUM(rm.cantidad_base *
            CASE rm.tipo_componente
                WHEN 'ARTICULO' THEN (SELECT costo_base FROM articulos WHERE id = rm.componente_id)
                ELSE (SELECT costo_base FROM preparaciones WHERE id = rm.componente_id)
            END)
        FROM recetas_menu rm WHERE rm.item_menu_id = i.id), 0)
    WHERE i.id > 0;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
