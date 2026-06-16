-- ============================================================================
-- ToppisERP - Fase 4: Función RPC de venta atómica (POS) - modelo articulos
-- ============================================================================
-- Reemplaza registrar_venta_menu para el nuevo modelo de cocina:
--   recetas_menu usa tipo_componente ARTICULO|PREPARACION y cantidad_base.
--   Descuenta stock de articulos.stock_base / preparaciones.stock_base.
--   Guarda costo_unitario, modificadores y promocion_id en items_venta_menu.
-- NOTA Fase 4: NO bloquea la venta por stock insuficiente (permite negativo);
--   el control de variance/alertas llega en Fase 5.
-- Ejecutar DESPUÉS de supabase-fase4-schema.sql
-- ============================================================================

CREATE OR REPLACE FUNCTION registrar_venta_menu(
    p_metodo_pago TEXT,
    p_sobre_id INTEGER,
    p_usuario UUID,
    p_monto_envio NUMERIC,
    p_incluir_envio BOOLEAN,
    p_items JSONB,
    p_comanda_texto TEXT
)
RETURNS INTEGER AS $$
DECLARE
    v_subtotal NUMERIC := 0;
    v_total NUMERIC := 0;
    v_venta_id INTEGER;
    v_item JSONB;
    v_receta RECORD;
    v_mod JSONB;
    v_descontar NUMERIC;
    v_promo_id INTEGER;
BEGIN
    SELECT COALESCE(SUM((it->>'subtotal')::numeric), 0)
        INTO v_subtotal
        FROM jsonb_array_elements(p_items) it;

    v_total := v_subtotal + COALESCE(p_monto_envio, 0);

    INSERT INTO ventas(fecha, total, metodo_pago, sobre_id, usuario_id, estado,
                       incluir_envio, monto_envio, stickers_enviados, created_by)
    VALUES (now(), v_total, p_metodo_pago::metodo_pago, p_sobre_id, p_usuario, 'COMPLETADA',
            p_incluir_envio, COALESCE(p_monto_envio, 0), 0, p_usuario)
    RETURNING id INTO v_venta_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        v_promo_id := NULLIF(v_item->>'promocion_id', '')::int;

        INSERT INTO items_venta_menu(venta_id, item_menu_id, cantidad, precio_unitario,
                                     subtotal, salsas_seleccionadas, costo_unitario,
                                     modificadores, promocion_id, created_by)
        VALUES (
            v_venta_id,
            (v_item->>'item_menu_id')::int,
            (v_item->>'cantidad')::int,
            (v_item->>'precio_unitario')::numeric,
            (v_item->>'subtotal')::numeric,
            COALESCE(v_item->>'salsas', ''),
            COALESCE((v_item->>'costo_unitario')::numeric, 0),
            COALESCE(v_item->>'modificadores', ''),
            v_promo_id,
            p_usuario
        );

        -- Descuento de stock según receta base (no bloquea por faltante en Fase 4)
        FOR v_receta IN
            SELECT * FROM recetas_menu WHERE item_menu_id = (v_item->>'item_menu_id')::int
        LOOP
            v_descontar := v_receta.cantidad_base * (v_item->>'cantidad')::int;

            IF v_receta.tipo_componente = 'ARTICULO' THEN
                UPDATE articulos SET stock_base = stock_base - v_descontar
                    WHERE id = v_receta.componente_id;
            ELSIF v_receta.tipo_componente = 'PREPARACION' THEN
                UPDATE preparaciones SET stock_base = stock_base - v_descontar
                    WHERE id = v_receta.componente_id;
            END IF;
        END LOOP;

        -- Ajuste de stock por modificadores (AGREGAR descuenta, QUITAR devuelve)
        FOR v_mod IN SELECT * FROM jsonb_array_elements(COALESCE(v_item->'mods_comp', '[]'::jsonb)) LOOP
            v_descontar := (v_mod->>'cantidad')::numeric * (v_item->>'cantidad')::int;
            IF (v_mod->>'accion') = 'QUITAR' THEN
                v_descontar := -v_descontar;  -- devolver stock
            END IF;
            IF (v_mod->>'tipo') = 'ARTICULO' THEN
                UPDATE articulos SET stock_base = stock_base - v_descontar
                    WHERE id = (v_mod->>'componente_id')::int;
            ELSIF (v_mod->>'tipo') = 'PREPARACION' THEN
                UPDATE preparaciones SET stock_base = stock_base - v_descontar
                    WHERE id = (v_mod->>'componente_id')::int;
            END IF;
        END LOOP;
    END LOOP;

    UPDATE sobres SET saldo = saldo + v_total WHERE id = p_sobre_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sobre id % no encontrado', p_sobre_id;
    END IF;

    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (NULL, p_sobre_id, v_total, 'INGRESO',
            'Venta POS #' || v_venta_id || ' - Pago: ' || p_metodo_pago, p_usuario);

    INSERT INTO comandas(venta_id, fecha, detalle_texto, estado, created_by)
    VALUES (v_venta_id, now(), p_comanda_texto, 'PENDIENTE', p_usuario);

    RETURN v_venta_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
