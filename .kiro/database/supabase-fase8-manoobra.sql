-- ============================================================================
-- ToppisERP - Fase 8: Mano de obra / Prime Cost
-- ============================================================================
-- Empleados (sueldo fijo / por turno / por hora), jornadas trabajadas y
-- propinas (total por día). Base para el Prime Cost (food + labor).
-- Ejecutar en Supabase SQL Editor.
-- ============================================================================

-- Tipo de pago del empleado
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_pago') THEN
        CREATE TYPE tipo_pago AS ENUM ('SUELDO_FIJO', 'POR_TURNO', 'POR_HORA');
    END IF;
END $$;

-- Empleados
CREATE TABLE IF NOT EXISTS empleados (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    cargo TEXT NOT NULL DEFAULT '',
    tipo_pago tipo_pago NOT NULL DEFAULT 'POR_TURNO',
    monto NUMERIC(12,2) NOT NULL DEFAULT 0,   -- sueldo mensual / valor turno / valor hora
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

-- Jornadas (turnos / horas trabajadas) — para POR_TURNO y POR_HORA
CREATE TABLE IF NOT EXISTS jornadas (
    id SERIAL PRIMARY KEY,
    empleado_id INTEGER NOT NULL REFERENCES empleados(id) ON DELETE CASCADE,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    cantidad NUMERIC(8,2) NOT NULL DEFAULT 1,  -- nº de turnos o nº de horas
    costo NUMERIC(12,2) NOT NULL DEFAULT 0,    -- cantidad × valor (snapshot)
    nota TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

-- Propinas (total por día)
CREATE TABLE IF NOT EXISTS propinas (
    id SERIAL PRIMARY KEY,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    monto NUMERIC(12,2) NOT NULL DEFAULT 0,
    nota TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_jornadas_empleado ON jornadas(empleado_id);
CREATE INDEX IF NOT EXISTS idx_jornadas_fecha ON jornadas(fecha);
CREATE INDEX IF NOT EXISTS idx_propinas_fecha ON propinas(fecha);

DROP TRIGGER IF EXISTS update_empleados_updated_at ON empleados;
CREATE TRIGGER update_empleados_updated_at BEFORE UPDATE ON empleados
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
DROP TRIGGER IF EXISTS update_jornadas_updated_at ON jornadas;
CREATE TRIGGER update_jornadas_updated_at BEFORE UPDATE ON jornadas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
DROP TRIGGER IF EXISTS update_propinas_updated_at ON propinas;
CREATE TRIGGER update_propinas_updated_at BEFORE UPDATE ON propinas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- RLS (solo admins gestionan; todos ven)
ALTER TABLE empleados ENABLE ROW LEVEL SECURITY;
ALTER TABLE jornadas ENABLE ROW LEVEL SECURITY;
ALTER TABLE propinas ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Todos ven empleados" ON empleados;
CREATE POLICY "Todos ven empleados" ON empleados FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan empleados" ON empleados;
CREATE POLICY "Solo admins gestionan empleados" ON empleados FOR ALL USING (get_user_rol() = 'ADMIN');

DROP POLICY IF EXISTS "Todos ven jornadas" ON jornadas;
CREATE POLICY "Todos ven jornadas" ON jornadas FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan jornadas" ON jornadas;
CREATE POLICY "Solo admins gestionan jornadas" ON jornadas FOR ALL USING (get_user_rol() = 'ADMIN');

DROP POLICY IF EXISTS "Todos ven propinas" ON propinas;
CREATE POLICY "Todos ven propinas" ON propinas FOR SELECT USING (true);
DROP POLICY IF EXISTS "Solo admins gestionan propinas" ON propinas;
CREATE POLICY "Solo admins gestionan propinas" ON propinas FOR ALL USING (get_user_rol() = 'ADMIN');
