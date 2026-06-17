-- ============================================================================
-- ToppisERP - Fase 7: Dinero a nivel real (cuentas vs fondos + arqueo de caja)
-- ============================================================================
-- Ejecutar en Supabase SQL Editor (después de las fases previas).
-- ============================================================================

-- 1. Tipo de sobre: CUENTA (dinero real) vs FONDO (provisión)
ALTER TABLE sobres ADD COLUMN IF NOT EXISTS tipo TEXT NOT NULL DEFAULT 'CUENTA'
    CHECK (tipo IN ('CUENTA', 'FONDO'));

-- 2. Arqueos de caja
CREATE TABLE IF NOT EXISTS arqueos (
    id SERIAL PRIMARY KEY,
    sobre_id INTEGER NOT NULL REFERENCES sobres(id) ON DELETE CASCADE,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    saldo_sistema NUMERIC(12,2) NOT NULL,
    saldo_contado NUMERIC(12,2) NOT NULL,
    diferencia NUMERIC(12,2) NOT NULL,    -- contado - sistema
    ajustado BOOLEAN NOT NULL DEFAULT false,
    nota TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_arqueos_sobre ON arqueos(sobre_id);
CREATE INDEX IF NOT EXISTS idx_arqueos_fecha ON arqueos(fecha);

DROP TRIGGER IF EXISTS update_arqueos_updated_at ON arqueos;
CREATE TRIGGER update_arqueos_updated_at BEFORE UPDATE ON arqueos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE arqueos ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Todos ven arqueos" ON arqueos;
CREATE POLICY "Todos ven arqueos" ON arqueos FOR SELECT USING (true);
DROP POLICY IF EXISTS "Autenticados crean arqueos" ON arqueos;
CREATE POLICY "Autenticados crean arqueos" ON arqueos FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
DROP POLICY IF EXISTS "Solo admins modifican arqueos" ON arqueos;
CREATE POLICY "Solo admins modifican arqueos" ON arqueos FOR UPDATE USING (get_user_rol() = 'ADMIN');
DROP POLICY IF EXISTS "Solo admins eliminan arqueos" ON arqueos;
CREATE POLICY "Solo admins eliminan arqueos" ON arqueos FOR DELETE USING (get_user_rol() = 'ADMIN');

-- 3. RPC: registrar arqueo (y ajustar saldo si se pide)
CREATE OR REPLACE FUNCTION registrar_arqueo(
    p_sobre_id INTEGER,
    p_contado NUMERIC,
    p_nota TEXT,
    p_ajustar BOOLEAN,
    p_usuario UUID
)
RETURNS INTEGER AS $$
DECLARE
    v_saldo NUMERIC;
    v_dif NUMERIC;
    v_id INTEGER;
BEGIN
    SELECT saldo INTO v_saldo FROM sobres WHERE id = p_sobre_id FOR UPDATE;
    IF v_saldo IS NULL THEN
        RAISE EXCEPTION 'Sobre no encontrado';
    END IF;

    v_dif := p_contado - v_saldo;

    INSERT INTO arqueos(sobre_id, saldo_sistema, saldo_contado, diferencia, ajustado, nota, created_by)
    VALUES (p_sobre_id, v_saldo, p_contado, v_dif, p_ajustar, COALESCE(p_nota, ''), p_usuario)
    RETURNING id INTO v_id;

    IF p_ajustar AND v_dif <> 0 THEN
        UPDATE sobres SET saldo = p_contado WHERE id = p_sobre_id;
        INSERT INTO movimientos_sobre(origen_id, destino_id, monto, tipo, descripcion, usuario_id)
        VALUES (
            CASE WHEN v_dif < 0 THEN p_sobre_id ELSE NULL END,
            CASE WHEN v_dif > 0 THEN p_sobre_id ELSE NULL END,
            abs(v_dif),
            (CASE WHEN v_dif < 0 THEN 'EGRESO' ELSE 'INGRESO' END)::tipo_movimiento,
            'Ajuste por arqueo de caja',
            p_usuario
        );
    END IF;

    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
