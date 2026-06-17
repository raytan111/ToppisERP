-- ============================================================================
-- ToppisERP - Fase 9 (Capa 2): Sellado de transacciones por local
-- ============================================================================
-- Agrega p_local_id a las RPC y guarda local_id en cada transacción.
-- Ejecutar DESPUÉS de supabase-fase9-locales.sql
-- ============================================================================

-- ── 1. registrar_venta_menu ───────────────────────────────────────────────
DROP FUNCTION IF EXISTS registrar_venta_menu(TEXT, INTEGER, UUID, NUMERIC, BOOLEAN, JSONB, TEXT);
CREATE OR REPLACE FUNCTION registrar_venta_menu(
    p_metodo_pago TEXT,
    p_sobre_id INTEGER,
    p_usuario UUID,
    p_monto_envio NUMERIC,
    p_incluir_envio BOOLEAN,
    p_items JSONB,
    p_comanda_texto TEXT,
    p_local_id INTEGER DEFAULT NULL
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
    SELECT COALESCE(SUM((it->>'subtotal')::numeric), 0) INTO v_subtotal
        FROM jsonb_array_elements(p_items) it;
    v_total := v_subtotal + COALESCE(p_monto_envio, 0);

    INSERT INTO ventas(fecha, total, metodo_pago, sobre_id, usuario_id, estado,
                       incluir_envio, monto_envio, stickers_enviados, local_id, created_by)
    VALUES (now(), v_total, p_metodo_pago::metodo_pago, p_sobre_id, p_usuario, 'COMPLETADA',
            p_incluir_envio, COALESCE(p_monto_envio, 0), 0, p_local_id, p_usuario)
    RETURNING id INTO v_venta_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items) LOOP
        v_promo_id := NULLIF(v_item->>'promocion_id', '')::int;
        INSERT INTO items_venta_menu(venta_id, item_menu_id, cantidad, precio_unitario,
                                     subtotal, salsas_seleccionadas, costo_unitario,
                                     modificadores, promocion_id, created_by)
        VALUES (
            v_venta_id, (v_item->>'item_menu_id')::int, (v_item->>'cantidad')::int,
            (v_item->>'precio_unitario')::numeric, (v_item->>'subtotal')::numeric,
            COALESCE(v_item->>'salsas', ''), COALESCE((v_item->>'costo_unitario')::numeric, 0),
            COALESCE(v_item->>'modificadores', ''), v_promo_id, p_usuario
        );

        FOR v_receta IN SELECT * FROM recetas_menu WHERE item_menu_id = (v_item->>'item_menu_id')::int LOOP
            v_descontar := v_receta.cantidad_base * (v_item->>'cantidad')::int;
            IF v_receta.tipo_componente = 'ARTICULO' THEN
                UPDATE articulos SET stock_base = stock_base - v_descontar WHERE id = v_receta.componente_id;
            ELSIF v_receta.tipo_componente = 'PREPARACION' THEN
                UPDATE preparaciones SET stock_base = stock_base - v_descontar WHERE id = v_receta.componente_id;
            END IF;
        END LOOP;

        FOR v_mod IN SELECT * FROM jsonb_array_elements(COALESCE(v_item->'mods_comp', '[]'::jsonb)) LOOP
            v_descontar := (v_mod->>'cantidad')::numeric * (v_item->>'cantidad')::int;
            IF (v_mod->>'accion') = 'QUITAR' THEN v_descontar := -v_descontar; END IF;
            IF (v_mod->>'tipo') = 'ARTICULO' THEN
                UPDATE articulos SET stock_base = stock_base - v_descontar WHERE id = (v_mod->>'componente_id')::int;
            ELSIF (v_mod->>'tipo') = 'PREPARACION' THEN
                UPDATE preparaciones SET stock_base = stock_base - v_descontar WHERE id = (v_mod->>'componente_id')::int;
            END IF;
        END LOOP;
    END LOOP;

    UPDATE sobres SET saldo = saldo + v_total WHERE id = p_sobre_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Sobre id % no encontrado', p_sobre_id; END IF;

    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (NULL, p_sobre_id, v_total, 'INGRESO', 'Venta POS #' || v_venta_id || ' - Pago: ' || p_metodo_pago, p_usuario);

    INSERT INTO comandas(venta_id, fecha, detalle_texto, estado, created_by)
    VALUES (v_venta_id, now(), p_comanda_texto, 'PENDIENTE', p_usuario);

    RETURN v_venta_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── 2. registrar_gasto ─────────────────────────────────────────────────────
DROP FUNCTION IF EXISTS registrar_gasto(TEXT, NUMERIC, TEXT, INTEGER, UUID, TEXT, BOOLEAN);
CREATE OR REPLACE FUNCTION registrar_gasto(
    p_descripcion TEXT, p_monto NUMERIC, p_categoria TEXT, p_sobre_id INTEGER,
    p_usuario UUID, p_comprobante TEXT, p_tiene_iva BOOLEAN DEFAULT false,
    p_local_id INTEGER DEFAULT NULL
)
RETURNS BIGINT AS $$
DECLARE
    v_saldo NUMERIC; v_gasto_id BIGINT; v_neto NUMERIC; v_iva NUMERIC;
BEGIN
    IF p_monto <= 0 THEN RAISE EXCEPTION 'El monto debe ser mayor a 0'; END IF;
    SELECT saldo INTO v_saldo FROM sobres WHERE id = p_sobre_id FOR UPDATE;
    IF v_saldo IS NULL THEN RAISE EXCEPTION 'Sobre no encontrado'; END IF;
    IF v_saldo < p_monto THEN RAISE EXCEPTION 'Saldo insuficiente en el sobre'; END IF;

    IF p_tiene_iva THEN v_neto := round(p_monto / 1.19); v_iva := p_monto - v_neto;
    ELSE v_neto := p_monto; v_iva := 0; END IF;

    INSERT INTO gastos(descripcion, monto, categoria, sobre_id, usuario_id, fecha,
                       comprobante, tiene_iva, monto_neto, monto_iva, local_id, created_by)
    VALUES (p_descripcion, p_monto, p_categoria, p_sobre_id, p_usuario, now(),
            p_comprobante, p_tiene_iva, v_neto, v_iva, p_local_id, p_usuario)
    RETURNING id INTO v_gasto_id;

    UPDATE sobres SET saldo = saldo - p_monto WHERE id = p_sobre_id;
    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (p_sobre_id, NULL, p_monto, 'EGRESO', 'Gasto: ' || p_descripcion, p_usuario);
    RETURN v_gasto_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── 3. registrar_compra ────────────────────────────────────────────────────
DROP FUNCTION IF EXISTS registrar_compra(INTEGER, BOOLEAN, TEXT, JSONB, INTEGER, UUID);
CREATE OR REPLACE FUNCTION registrar_compra(
    p_proveedor_id INTEGER, p_tiene_iva BOOLEAN, p_nota TEXT, p_items JSONB,
    p_sobre_id INTEGER, p_usuario UUID, p_local_id INTEGER DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    v_total NUMERIC := 0; v_compra_id INTEGER; v_item JSONB; v_art RECORD;
    v_cant NUMERIC; v_costo_base NUMERIC; v_subtotal NUMERIC;
    v_bruto_actual NUMERIC; v_nuevo_bruto NUMERIC; v_venc DATE;
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
        v_costo_base := (v_item->>'costo_por_base')::numeric;
        v_subtotal := v_cant * v_costo_base;
        v_venc := NULLIF(v_item->>'vencimiento', '')::date;
        SELECT * INTO v_art FROM articulos WHERE id = (v_item->>'articulo_id')::int FOR UPDATE;
        IF v_art.id IS NOT NULL THEN
            v_bruto_actual := v_art.costo_base * COALESCE(NULLIF(v_art.rendimiento,0), 1);
            IF (v_art.stock_base + v_cant) > 0 THEN
                v_nuevo_bruto := (v_art.stock_base * v_bruto_actual + v_cant * v_costo_base) / (v_art.stock_base + v_cant);
            ELSE v_nuevo_bruto := v_costo_base; END IF;
            UPDATE articulos SET stock_base = stock_base + v_cant,
                   costo_base = v_nuevo_bruto / COALESCE(NULLIF(v_art.rendimiento,0), 1)
             WHERE id = v_art.id;
        END IF;
        INSERT INTO compra_detalle(compra_id, articulo_id, cantidad_base, costo_por_base, subtotal, vencimiento, created_by)
        VALUES (v_compra_id, (v_item->>'articulo_id')::int, v_cant, v_costo_base, v_subtotal, v_venc, p_usuario);
    END LOOP;
    RETURN v_compra_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── 4. registrar_merma ─────────────────────────────────────────────────────
DROP FUNCTION IF EXISTS registrar_merma(TEXT, INTEGER, NUMERIC, TEXT, TEXT, UUID);
CREATE OR REPLACE FUNCTION registrar_merma(
    p_tipo TEXT, p_componente_id INTEGER, p_cantidad NUMERIC, p_motivo TEXT,
    p_nota TEXT, p_usuario UUID, p_local_id INTEGER DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE v_costo_base NUMERIC := 0; v_costo NUMERIC := 0; v_id INTEGER;
BEGIN
    IF p_tipo = 'ARTICULO' THEN
        SELECT costo_base INTO v_costo_base FROM articulos WHERE id = p_componente_id;
        UPDATE articulos SET stock_base = stock_base - p_cantidad WHERE id = p_componente_id;
    ELSIF p_tipo = 'PREPARACION' THEN
        SELECT costo_base INTO v_costo_base FROM preparaciones WHERE id = p_componente_id;
        UPDATE preparaciones SET stock_base = stock_base - p_cantidad WHERE id = p_componente_id;
    END IF;
    v_costo := COALESCE(v_costo_base, 0) * p_cantidad;
    INSERT INTO mermas(tipo_componente, componente_id, cantidad_base, motivo, costo, nota, local_id, created_by)
    VALUES (p_tipo::tipo_componente, p_componente_id, p_cantidad, p_motivo, v_costo, COALESCE(p_nota, ''), p_local_id, p_usuario)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ── 5. registrar_arqueo ────────────────────────────────────────────────────
DROP FUNCTION IF EXISTS registrar_arqueo(INTEGER, NUMERIC, TEXT, BOOLEAN, UUID);
CREATE OR REPLACE FUNCTION registrar_arqueo(
    p_sobre_id INTEGER, p_contado NUMERIC, p_nota TEXT, p_ajustar BOOLEAN,
    p_usuario UUID, p_local_id INTEGER DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE v_saldo NUMERIC; v_dif NUMERIC; v_id INTEGER;
BEGIN
    SELECT saldo INTO v_saldo FROM sobres WHERE id = p_sobre_id FOR UPDATE;
    IF v_saldo IS NULL THEN RAISE EXCEPTION 'Sobre no encontrado'; END IF;
    v_dif := p_contado - v_saldo;
    INSERT INTO arqueos(sobre_id, saldo_sistema, saldo_contado, diferencia, ajustado, nota, local_id, created_by)
    VALUES (p_sobre_id, v_saldo, p_contado, v_dif, p_ajustar, COALESCE(p_nota, ''), p_local_id, p_usuario)
    RETURNING id INTO v_id;
    IF p_ajustar AND v_dif <> 0 THEN
        UPDATE sobres SET saldo = p_contado WHERE id = p_sobre_id;
        INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
        VALUES (
            CASE WHEN v_dif < 0 THEN p_sobre_id ELSE NULL END,
            CASE WHEN v_dif > 0 THEN p_sobre_id ELSE NULL END,
            abs(v_dif),
            (CASE WHEN v_dif < 0 THEN 'EGRESO' ELSE 'INGRESO' END)::tipo_movimiento,
            'Ajuste por arqueo de caja', p_usuario
        );
    END IF;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
