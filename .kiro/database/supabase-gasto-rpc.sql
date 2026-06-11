-- ============================================================================
-- ToppisERP - Función RPC de gasto atómico
-- ============================================================================
-- Registra un gasto en una sola transacción:
--   valida saldo + inserta gasto + descuenta saldo del sobre + movimiento EGRESO
-- ============================================================================

CREATE OR REPLACE FUNCTION registrar_gasto(
    p_descripcion TEXT,
    p_monto NUMERIC,
    p_categoria TEXT,
    p_sobre_id INTEGER,
    p_usuario UUID,
    p_comprobante TEXT
)
RETURNS BIGINT AS $$
DECLARE
    v_saldo NUMERIC;
    v_gasto_id BIGINT;
BEGIN
    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto debe ser mayor a 0';
    END IF;

    SELECT saldo INTO v_saldo FROM sobres WHERE id = p_sobre_id FOR UPDATE;
    IF v_saldo IS NULL THEN
        RAISE EXCEPTION 'Sobre no encontrado';
    END IF;
    IF v_saldo < p_monto THEN
        RAISE EXCEPTION 'Saldo insuficiente en el sobre';
    END IF;

    INSERT INTO gastos(descripcion, monto, categoria, sobre_id, usuario_id, fecha, comprobante, created_by)
    VALUES (p_descripcion, p_monto, p_categoria, p_sobre_id, p_usuario, now(), p_comprobante, p_usuario)
    RETURNING id INTO v_gasto_id;

    UPDATE sobres SET saldo = saldo - p_monto WHERE id = p_sobre_id;

    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (p_sobre_id, NULL, p_monto, 'EGRESO', 'Gasto: ' || p_descripcion, p_usuario);

    RETURN v_gasto_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Habilitar Realtime para gastos (respeta RLS: cada cajero ve solo los suyos)
ALTER PUBLICATION supabase_realtime ADD TABLE gastos;
