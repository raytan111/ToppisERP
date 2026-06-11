-- ============================================================================
-- ToppisERP - Corrección tablas salsas y recetas_menu (módulo Menú)
-- ============================================================================
-- salsas: faltaban 'descripcion' y el flag correcto es 'activa'
-- recetas_menu: el modelo real usa 'cantidad' (decimal), no 'cantidad_gramos'
-- Ambas tablas están vacías, es seguro recrearlas.
-- ============================================================================

-- ── SALSAS ──────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS salsas CASCADE;

CREATE TABLE salsas (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    activa BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_salsas_nombre ON salsas(nombre);

CREATE TRIGGER update_salsas_updated_at
    BEFORE UPDATE ON salsas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE salsas ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Todos ven salsas"
ON salsas FOR SELECT USING (true);

CREATE POLICY "Solo admins modifican salsas"
ON salsas FOR ALL USING (get_user_rol() = 'ADMIN');

GRANT SELECT, INSERT, UPDATE, DELETE ON salsas TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE salsas_id_seq TO anon, authenticated;

ALTER PUBLICATION supabase_realtime ADD TABLE salsas;

-- ── RECETAS_MENU ──────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS recetas_menu CASCADE;

CREATE TABLE recetas_menu (
    id SERIAL PRIMARY KEY,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    tipo_componente tipo_componente NOT NULL,
    componente_id INTEGER NOT NULL,
    cantidad NUMERIC(12, 2) NOT NULL CHECK (cantidad > 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_recetas_menu_item_menu_id ON recetas_menu(item_menu_id);
CREATE INDEX idx_recetas_menu_componente ON recetas_menu(componente_id, tipo_componente);

CREATE TRIGGER update_recetas_menu_updated_at
    BEFORE UPDATE ON recetas_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE recetas_menu ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Todos ven recetas"
ON recetas_menu FOR SELECT USING (true);

CREATE POLICY "Solo admins modifican recetas"
ON recetas_menu FOR ALL USING (get_user_rol() = 'ADMIN');

GRANT SELECT, INSERT, UPDATE, DELETE ON recetas_menu TO anon, authenticated;
GRANT USAGE, SELECT ON SEQUENCE recetas_menu_id_seq TO anon, authenticated;
