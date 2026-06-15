-- ============================================================================
-- ToppisERP - Fase 3: Contabilidad (IVA en gastos + cierres mensuales)
-- ============================================================================

-- 1. IVA en gastos (IVA crédito)
ALTER TABLE gastos ADD COLUMN IF NOT EXISTS tiene_iva BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE gastos ADD COLUMN IF NOT EXISTS monto_neto NUMERIC(12, 2);
ALTER TABLE gastos ADD COLUMN IF NOT EXISTS monto_iva NUMERIC(12, 2);

-- 2. Actualizar función registrar_gasto para calcular neto/IVA
CREATE OR REPLACE FUNCTION registrar_gasto(
    p_descripcion TEXT,
    p_monto NUMERIC,
    p_categoria TEXT,
    p_sobre_id INTEGER,
    p_usuario UUID,
    p_comprobante TEXT,
    p_tiene_iva BOOLEAN DEFAULT false
)
RETURNS BIGINT AS $$
DECLARE
    v_saldo NUMERIC;
    v_gasto_id BIGINT;
    v_neto NUMERIC;
    v_iva NUMERIC;
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

    -- Cálculo de IVA (precios con IVA incluido)
    IF p_tiene_iva THEN
        v_neto := round(p_monto / 1.19);
        v_iva := p_monto - v_neto;
    ELSE
        v_neto := p_monto;
        v_iva := 0;
    END IF;

    INSERT INTO gastos(descripcion, monto, categoria, sobre_id, usuario_id, fecha,
                       comprobante, tiene_iva, monto_neto, monto_iva, created_by)
    VALUES (p_descripcion, p_monto, p_categoria, p_sobre_id, p_usuario, now(),
            p_comprobante, p_tiene_iva, v_neto, v_iva, p_usuario)
    RETURNING id INTO v_gasto_id;

    UPDATE sobres SET saldo = saldo - p_monto WHERE id = p_sobre_id;

    INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (p_sobre_id, NULL, p_monto, 'EGRESO', 'Gasto: ' || p_descripcion, p_usuario);

    RETURN v_gasto_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 3. Tabla de cierres mensuales (snapshot)
CREATE TABLE IF NOT EXISTS cierres_mensuales (
    id SERIAL PRIMARY KEY,
    mes INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    anio INTEGER NOT NULL,
    ventas_netas NUMERIC(12, 2) NOT NULL DEFAULT 0,
    iva_debito NUMERIC(12, 2) NOT NULL DEFAULT 0,
    compras_netas NUMERIC(12, 2) NOT NULL DEFAULT 0,
    iva_credito NUMERIC(12, 2) NOT NULL DEFAULT 0,
    iva_a_pagar NUMERIC(12, 2) NOT NULL DEFAULT 0,
    resultado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    fecha_cierre TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    UNIQUE(mes, anio)
);

ALTER TABLE cierres_mensuales ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Solo admins gestionan cierres"
ON cierres_mensuales FOR ALL USING (get_user_rol() = 'ADMIN');

GRANT SELECT, INSERT, UPDATE, DELETE ON cierres_mensuales TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE cierres_mensuales_id_seq TO anon, authenticated;
