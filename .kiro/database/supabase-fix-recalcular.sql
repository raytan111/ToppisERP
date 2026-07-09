-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — FIX compras (2026-07-09)
-- ════════════════════════════════════════════════════════════════════════
-- Dos correcciones a las funciones de compra por "último precio":
--
-- 1) recalcular_recetas_articulo: agrega WHERE a los UPDATE globales
--    (el proyecto tiene el guard "safeupdate" que rechaza UPDATE sin WHERE).
--
-- 2) registrar_compra: el campo costo_compra del artículo guardaba el costo
--    por unidad base (ej: $9/g) en vez del precio del "pack" completo (ej:
--    $9.000 por 1.000 g). Ahora guarda costo_compra = ($/base) × factor_compra,
--    para que el formulario de edición lo muestre coherente. OJO: costo_base
--    (lo que usan las recetas) ya estaba correcto; esto solo arregla el
--    valor que ves en "Total pagado" al reabrir el artículo.
--
-- Ejecutar en el SQL Editor de Supabase. Idempotente (CREATE OR REPLACE).
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


DROP FUNCTION IF EXISTS registrar_compra(INTEGER, BOOLEAN, TEXT, JSONB, INTEGER, UUID);
CREATE OR REPLACE FUNCTION registrar_compra(
    p_proveedor_id INTEGER, p_tiene_iva BOOLEAN, p_nota TEXT, p_items JSONB,
    p_sobre_id INTEGER, p_usuario UUID, p_local_id INTEGER DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    v_total NUMERIC := 0; v_compra_id INTEGER; v_item JSONB; v_art RECORD;
    v_cant NUMERIC; v_costo_base NUMERIC; v_subtotal NUMERIC;
    v_nuevo_costo_base NUMERIC; v_venc DATE;
    v_gasto_id BIGINT := NULL; v_neto NUMERIC; v_iva NUMERIC;
BEGIN
    SELECT COALESCE(SUM((it->>'cantidad_base')::numeric * (it->>'costo_por_base')::numeric), 0)
        INTO v_total FROM jsonb_array_elements(p_items) it;

    IF p_sobre_id IS NOT NULL AND v_total > 0 THEN
        IF p_tiene_iva THEN v_neto := round(v_total / 1.19); v_iva := v_total - v_neto;
        ELSE v_neto := v_total; v_iva := 0; END IF;
        PERFORM 1 FROM sobres WHERE id = p_sobre_id FOR UPDATE;
        IF (SELECT saldo FROM sobres WHERE id = p_sobre_id) < v_total THEN
            RAISE EXCEPTION 'Saldo insuficiente en el sobre para la compra';
        END IF;
        INSERT INTO gastos(descripcion, monto, categoria, sobre_id, usuario_id, fecha,
                           comprobante, tiene_iva, monto_neto, monto_iva, local_id, created_by)
        VALUES ('Compra de mercadería', v_total, 'INSUMOS', p_sobre_id, p_usuario, now(),
                '', p_tiene_iva, v_neto, v_iva, p_local_id, p_usuario)
        RETURNING id INTO v_gasto_id;
        UPDATE sobres SET saldo = saldo - v_total WHERE id = p_sobre_id;
        INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
        VALUES (p_sobre_id, NULL, v_total, 'EGRESO', 'Compra de mercadería', p_usuario);
    END IF;

    INSERT INTO compras(proveedor_id, fecha, total, tiene_iva, nota, gasto_id, local_id, created_by)
    VALUES (p_proveedor_id, now(), v_total, p_tiene_iva, COALESCE(p_nota, ''), v_gasto_id, p_local_id, p_usuario)
    RETURNING id INTO v_compra_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        v_cant := (v_item->>'cantidad_base')::numeric;
        v_costo_base := (v_item->>'costo_por_base')::numeric;  -- costo por unidad base (bruto)
        v_subtotal := v_cant * v_costo_base;
        v_venc := NULLIF(v_item->>'vencimiento', '')::date;
        SELECT * INTO v_art FROM articulos WHERE id = (v_item->>'articulo_id')::int FOR UPDATE;
        IF v_art.id IS NOT NULL THEN
            v_nuevo_costo_base := v_costo_base / COALESCE(NULLIF(v_art.rendimiento,0), 1);
            IF v_nuevo_costo_base IS DISTINCT FROM v_art.costo_base THEN
                -- costo_compra = precio del "pack" completo = ($/base) × factor_compra
                UPDATE articulos SET stock_base = stock_base + v_cant,
                       costo_base = v_nuevo_costo_base,
                       costo_compra = v_costo_base * COALESCE(NULLIF(v_art.factor_compra, 0), 1)
                 WHERE id = v_art.id;
                PERFORM recalcular_recetas_articulo(v_art.id);
            ELSE
                UPDATE articulos SET stock_base = stock_base + v_cant WHERE id = v_art.id;
            END IF;
        END IF;
        INSERT INTO compra_detalle(compra_id, articulo_id, cantidad_base, costo_por_base, subtotal, vencimiento, created_by)
        VALUES (v_compra_id, (v_item->>'articulo_id')::int, v_cant, v_costo_base, v_subtotal, v_venc, p_usuario);
    END LOOP;
    RETURN v_compra_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
