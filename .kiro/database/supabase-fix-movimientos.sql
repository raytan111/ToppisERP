-- ============================================================================
-- ToppisERP - Corrección tabla movimientos_sobre + función de transferencia
-- ============================================================================
-- El modelo real de la app usa origen_id / destino_id (no sobre_id).
-- Este script recrea la tabla y agrega la función atómica de transferencia.
-- IMPORTANTE: La tabla movimientos_sobre está vacía, es seguro recrearla.
-- ============================================================================

-- 1. Eliminar tabla anterior (estaba vacía)
DROP TABLE IF EXISTS movimientos_sobre CASCADE;

-- 2. Recrear con el modelo correcto
CREATE TABLE movimientos_sobre (
    id SERIAL PRIMARY KEY,
    origen_id INTEGER REFERENCES sobres(id) ON DELETE SET NULL,
    destino_id INTEGER REFERENCES sobres(id) ON DELETE SET NULL,
    monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    tipo tipo_movimiento NOT NULL,
    descripcion TEXT NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_movimientos_sobre_origen ON movimientos_sobre(origen_id);
CREATE INDEX idx_movimientos_sobre_destino ON movimientos_sobre(destino_id);
CREATE INDEX idx_movimientos_sobre_usuario ON movimientos_sobre(usuario_id);
CREATE INDEX idx_movimientos_sobre_fecha ON movimientos_sobre(fecha);

CREATE TRIGGER update_movimientos_sobre_updated_at
    BEFORE UPDATE ON movimientos_sobre
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3. Habilitar RLS + políticas
ALTER TABLE movimientos_sobre ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Todos ven movimientos"
ON movimientos_sobre FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean movimientos"
ON movimientos_sobre FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- 4. Función atómica de transferencia entre sobres
--    Garantiza que el descuento, abono y registro del movimiento ocurran
--    como una sola transacción. SECURITY DEFINER para operar de forma segura.
CREATE OR REPLACE FUNCTION transferir_entre_sobres(
    p_origen INTEGER,
    p_destino INTEGER,
    p_monto NUMERIC,
    p_descripcion TEXT,
    p_usuario UUID DEFAULT NULL
)
RETURNS void AS $$
DECLARE
    v_saldo_origen NUMERIC;
BEGIN
    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto debe ser mayor a 0';
    END IF;

    -- Bloquear y leer saldo del origen
    SELECT saldo INTO v_saldo_origen FROM sobres WHERE id = p_origen FOR UPDATE;
    IF v_saldo_origen IS NULL THEN
        RAISE EXCEPTION 'Sobre origen no encontrado';
    END IF;
    IF v_saldo_origen < p_monto THEN
        RAISE EXCEPTION 'Saldo insuficiente en el sobre origen';
    END IF;

    -- Verificar destino
    PERFORM 1 FROM sobres WHERE id = p_destino FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sobre destino no encontrado';
    END IF;

    -- Mover el dinero
    UPDATE sobres SET saldo = saldo - p_monto WHERE id = p_origen;
    UPDATE sobres SET saldo = saldo + p_monto WHERE id = p_destino;

    -- Registrar el movimiento
    INSERT INTO movimientos_sobre (origen_id, destino_id, monto, tipo, descripcion, usuario_id)
    VALUES (p_origen, p_destino, p_monto, 'TRANSFERENCIA', p_descripcion, p_usuario);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
