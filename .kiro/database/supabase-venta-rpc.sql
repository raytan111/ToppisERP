-- ============================================================================
-- ToppisERP - Función RPC de venta atómica (POS)
-- ============================================================================
-- Registra una venta completa en una sola transacción:
--   venta + items + descuento de stock (receta) + saldo sobre + movimiento + comanda
-- Si falta stock, lanza excepción y revierte TODO.
-- ============================================================================

-- 1. Agregar columna faltante a items_venta_menu
ALTER TABLE items_venta_menu
    ADD COLUMN IF NOT EXISTS salsas_seleccionadas TEXT NOT NULL DEFAULT '';

-- 2. Función de venta atómica
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
    v_descontar NUMERIC;
    v_stock NUMERIC;
BEGIN
    -- Subtotal de los items
    SELECT COALESCE(SUM((it->>'subtotal')::numeric), 0)
        INTO v_subtotal
        FROM jsonb_array_elements(p_items) it;

    v_total := v_subtotal + COALESCE(p_monto_envio, 0);

    -- Insertar venta
    INSERT INTO ventas(fecha, total, metodo_pago, sobre_id, usuario_id, estado,
                       incluir_envio, monto_envio, stickers_enviados, created_by)
    VALUES (now(), v_total, p_metodo_pago::metodo_pago, p_sobre_id, p_usuario, 'COMPLETADA',
            p_incluir_envio, COALESCE(p_monto_envio, 0), 0, p_usuario)
    RETURNING id INTO v_venta_id;

    -- Items + descuento de stock según receta
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        INSERT INTO items_venta_menu(venta_id, item_menu_id, cantidad, precio_unitario,
                                     subtotal, salsas_seleccionadas, created_by)
        VALUES (
            v_venta_id,
            (v_item->>'item_menu_id')::int,
            (v_item->>'cantidad')::int,
            (v_item->>'precio_unitario')::numeric,
            (v_item->>'subtotal')::numeric,
            COALESCE(v_item->>'salsas', ''),
            p_usuario
        );

        FOR v_receta IN
            SELECT * FROM recetas_menu WHERE item_menu_id = (v_item->>'item_menu_id')::int
        LOOP
            v_descontar := v_receta.cantidad * (v_item->>'cantidad')::int;

            IF v_receta.tipo_componente = 'INGREDIENTE' THEN
                SELECT stock_actual INTO v_stock FROM ingredientes
                    WHERE id = v_receta.componente_id FOR UPDATE;
                IF v_stock IS NOT NULL THEN
                    IF v_stock < v_descontar THEN
                        RAISE EXCEPTION 'Stock insuficiente en ingrediente id % (disp: %, req: %)',
                            v_receta.componente_id, v_stock, v_descontar;
                    END IF;
                    UPDATE ingredientes SET stock_actual = stock_actual - v_descontar
                        WHERE id = v_receta.componente_id;
                END IF;

            ELSIF v_receta.tipo_componente = 'INSUMO' THEN
                SELECT stock INTO v_stock FROM insumos
                    WHERE id = v_receta.componente_id FOR UPDATE;
                IF v_stock IS NOT NULL THEN
                    IF v_stock < v_descontar THEN
                        RAISE EXCEPTION 'Stock insuficiente en insumo id % (disp: %, req: %)',
                            v_receta.componente_id, v_stock, v_descontar;
                    END IF;
                    UPDATE insumos SET stock = stock - v_descontar::int
                        WHERE id = v_receta.componente_id;
                END IF;
            END IF;
            -- SALSA: no descuenta stock
        END LOOP;
    END LOOP;

    -- Actualizar saldo del sobre
    UPDATE sobres SET saldo = saldo + v_total WHERE id = p_sobre_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sobre id % no encontrado', p_sobre_id;
    END IF;

    -- Movimiento de ingreso
    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (NULL, p_sobre_id, v_total, 'INGRESO',
            'Venta POS #' || v_venta_id || ' - Pago: ' || p_metodo_pago, p_usuario);

    -- Comanda
    INSERT INTO comandas(venta_id, fecha, detalle_texto, estado, created_by)
    VALUES (v_venta_id, now(), p_comanda_texto, 'PENDIENTE', p_usuario);

    RETURN v_venta_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
