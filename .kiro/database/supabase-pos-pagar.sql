-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — Rediseño del POS (Fase G): pagar_pedido (atómico)
-- EJECUTAR en el SQL Editor de Supabase.
-- Materializa una venta desde un pedido: crea la venta + su detalle, descuenta
-- stock (recetas + modificadores), suma al sobre y marca el pedido como pagado.
-- Idempotente por venta_id (si el pedido ya tiene venta, la devuelve y no duplica).
-- Todos los UPDATE llevan WHERE (guard safeupdate).
-- ════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION pagar_pedido(
    p_pedido_id INTEGER,
    p_metodo    TEXT,
    p_sobre_id  INTEGER,
    p_usuario   UUID
) RETURNS INTEGER AS $$
DECLARE
    v_ped     pedidos%ROWTYPE;
    v_venta_id INTEGER;
    v_item    RECORD;
    v_unidad  RECORD;
    v_receta  RECORD;
    v_mc      RECORD;
    v_cant    INTEGER;
    v_desc    NUMERIC;
    v_costo   NUMERIC;
    v_precio  NUMERIC;
    v_sub     NUMERIC;
BEGIN
    SELECT * INTO v_ped FROM pedidos WHERE id = p_pedido_id FOR UPDATE;
    IF v_ped.id IS NULL THEN RAISE EXCEPTION 'Pedido no encontrado'; END IF;
    -- Idempotencia: si ya se pagó (tiene venta), devolverla sin duplicar.
    IF v_ped.venta_id IS NOT NULL THEN RETURN v_ped.venta_id; END IF;

    PERFORM 1 FROM sobres WHERE id = p_sobre_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Sobre no encontrado'; END IF;

    -- 1) Cabecera de la venta (el total del pedido es la fuente de verdad)
    INSERT INTO ventas(fecha, total, metodo_pago, sobre_id, usuario_id, estado,
                       incluir_envio, monto_envio, stickers_enviados, local_id, created_by)
    VALUES (now(), v_ped.total, p_metodo::metodo_pago, p_sobre_id, p_usuario, 'COMPLETADA',
            COALESCE(v_ped.monto_envio,0) > 0, COALESCE(v_ped.monto_envio,0), 0, v_ped.local_id, p_usuario)
    RETURNING id INTO v_venta_id;

    -- 2) Por cada línea del pedido y cada unidad a preparar
    FOR v_item IN SELECT * FROM pedido_items WHERE pedido_id = p_pedido_id LOOP
        v_cant := GREATEST(v_item.cantidad, 1);
        FOR v_unidad IN SELECT * FROM pedido_unidades WHERE pedido_item_id = v_item.id LOOP

            -- 2a) Descontar la receta del producto
            FOR v_receta IN SELECT * FROM recetas_menu WHERE item_menu_id = v_unidad.item_menu_id LOOP
                v_desc := v_receta.cantidad_base * v_cant;
                IF v_receta.tipo_componente = 'ARTICULO' THEN
                    UPDATE articulos SET stock_base = stock_base - v_desc WHERE id = v_receta.componente_id;
                ELSE
                    UPDATE preparaciones SET stock_base = stock_base - v_desc WHERE id = v_receta.componente_id;
                END IF;
            END LOOP;

            -- 2b) Descontar/devolver por los modificadores de la unidad
            FOR v_mc IN
                SELECT mc.* FROM pedido_unidad_mods pum
                JOIN modificador_componentes mc ON mc.modificador_id = pum.modificador_id
                WHERE pum.pedido_unidad_id = v_unidad.id
            LOOP
                v_desc := v_mc.cantidad_base * v_cant;
                IF v_mc.accion = 'QUITAR' THEN v_desc := -v_desc; END IF;
                IF v_mc.tipo_componente = 'ARTICULO' THEN
                    UPDATE articulos SET stock_base = stock_base - v_desc WHERE id = v_mc.componente_id;
                ELSE
                    UPDATE preparaciones SET stock_base = stock_base - v_desc WHERE id = v_mc.componente_id;
                END IF;
            END LOOP;

            -- 2c) Detalle de venta (una fila por unidad). Para PROMO el ingreso va en
            --     el total de la venta, por eso el precio por unidad de promo es 0.
            SELECT COALESCE(costo_teorico,0) INTO v_costo FROM items_menu WHERE id = v_unidad.item_menu_id;
            IF v_item.tipo = 'PRODUCTO' THEN
                v_precio := v_item.precio_unitario;
                v_sub := v_item.subtotal;
            ELSE
                v_precio := 0;
                v_sub := 0;
            END IF;

            INSERT INTO items_venta_menu(venta_id, item_menu_id, cantidad, precio_unitario,
                                         subtotal, salsas_seleccionadas, costo_unitario,
                                         modificadores, promocion_id, created_by)
            VALUES (v_venta_id, v_unidad.item_menu_id, v_cant, v_precio, v_sub, '',
                    v_costo, COALESCE(v_unidad.comentario,''),
                    CASE WHEN v_item.tipo = 'PROMO' THEN v_item.promocion_id ELSE NULL END, p_usuario);
        END LOOP;
    END LOOP;

    -- 3) Ingreso al sobre + movimiento
    UPDATE sobres SET saldo = saldo + v_ped.total WHERE id = p_sobre_id;
    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (NULL, p_sobre_id, v_ped.total, 'INGRESO', 'Pago pedido #' || p_pedido_id || ' - ' || p_metodo, p_usuario);

    -- 4) Marcar el pedido como pagado
    UPDATE pedidos SET pagado = TRUE, paid_at = now(), metodo_pago = p_metodo,
           sobre_id = p_sobre_id, venta_id = v_venta_id
     WHERE id = p_pedido_id;

    RETURN v_venta_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
