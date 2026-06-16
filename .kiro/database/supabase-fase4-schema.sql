-- ============================================================================
-- ToppisERP - Fase 4: Costos y Rentabilidad (Food Cost) - SCHEMA
-- Versión: 4.0
-- Fecha: 2026-06-15
-- Autor: Kiro + andreslh
-- Proyecto: https://dkgqrbxizegipxdsypzf.supabase.co
-- ============================================================================
-- IMPORTANTE: CLEAN SLATE de las tablas de cocina (no hay datos reales).
-- Ejecutar en Supabase SQL Editor en este orden:
--   1) supabase-fase4-schema.sql   (este archivo)
--   2) supabase-fase4-rls.sql
--   3) supabase-fase4-realtime.sql
-- Reemplaza: ingredientes, insumos, salsas, recetas_menu (modelo viejo).
-- Mantiene: usuarios, sobres, movimientos_sobre, ventas, items_venta_menu,
--           comandas, gastos, presupuestos, comprobantes, contabilidad.
-- ============================================================================

-- ============================================================================
-- 0. LIMPIEZA (clean slate de cocina)
-- ============================================================================
-- Quitar de la publicación realtime las tablas viejas (ignorar si no existen)
DO $$
BEGIN
    BEGIN ALTER PUBLICATION supabase_realtime DROP TABLE ingredientes; EXCEPTION WHEN OTHERS THEN NULL; END;
    BEGIN ALTER PUBLICATION supabase_realtime DROP TABLE insumos; EXCEPTION WHEN OTHERS THEN NULL; END;
END $$;

DROP TABLE IF EXISTS recetas_menu CASCADE;
DROP TABLE IF EXISTS salsas CASCADE;
DROP TABLE IF EXISTS ingredientes CASCADE;
DROP TABLE IF EXISTS insumos CASCADE;

-- Tablas nuevas que pudieran existir de una corrida previa
DROP TABLE IF EXISTS papa_rendimientos CASCADE;
DROP TABLE IF EXISTS promocion_items CASCADE;
DROP TABLE IF EXISTS promociones CASCADE;
DROP TABLE IF EXISTS modificador_componentes CASCADE;
DROP TABLE IF EXISTS modificadores CASCADE;
DROP TABLE IF EXISTS preparacion_componentes CASCADE;
DROP TABLE IF EXISTS preparaciones CASCADE;
DROP TABLE IF EXISTS articulos CASCADE;

-- ============================================================================
-- 1. ENUMS NUEVOS
-- ============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'dimension_unidad') THEN
        CREATE TYPE dimension_unidad AS ENUM ('MASA', 'VOLUMEN', 'UNIDAD');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_articulo') THEN
        CREATE TYPE tipo_articulo AS ENUM ('INGREDIENTE', 'PREPARACION');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_modificador') THEN
        CREATE TYPE tipo_modificador AS ENUM ('DOBLE', 'QUITAR', 'REEMPLAZAR', 'EXTRA');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_promocion') THEN
        CREATE TYPE tipo_promocion AS ENUM ('COMBO', 'DESCUENTO_PORCENTAJE');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'accion_modificador') THEN
        CREATE TYPE accion_modificador AS ENUM ('AGREGAR', 'QUITAR');
    END IF;
END $$;

-- tipo_componente: el viejo enum era ('INGREDIENTE','INSUMO'[,'SALSA']).
-- El nuevo modelo usa ('ARTICULO','PREPARACION'). Recreamos el tipo.
DO $$
BEGIN
    -- Como recetas_menu (que lo usaba) ya fue dropeada, podemos recrear el tipo.
    DROP TYPE IF EXISTS tipo_componente CASCADE;
    CREATE TYPE tipo_componente AS ENUM ('ARTICULO', 'PREPARACION');
END $$;

-- ============================================================================
-- 2. TRIGGER updated_at (re-usa la función existente si ya está)
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. TABLA: articulos  (reemplaza ingredientes + insumos)
-- ============================================================================
CREATE TABLE articulos (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    dimension dimension_unidad NOT NULL,
    unidad_base TEXT NOT NULL,                 -- 'g' | 'ml' | 'un'
    unidad_compra TEXT NOT NULL DEFAULT '',    -- 'saco 25kg', 'pack 6', 'bidón 5L'
    factor_compra NUMERIC(14,4) NOT NULL DEFAULT 1 CHECK (factor_compra > 0),
    costo_compra NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_compra >= 0),
    costo_base NUMERIC(14,6) NOT NULL DEFAULT 0 CHECK (costo_base >= 0),
    rendimiento NUMERIC(6,4) NOT NULL DEFAULT 1 CHECK (rendimiento > 0 AND rendimiento <= 1),
    stock_base NUMERIC(14,4) NOT NULL DEFAULT 0,
    par_level NUMERIC(14,4) NOT NULL DEFAULT 0,
    perecible BOOLEAN NOT NULL DEFAULT false,
    vida_util_dias INTEGER NOT NULL DEFAULT 0 CHECK (vida_util_dias >= 0),
    es_vendible BOOLEAN NOT NULL DEFAULT false,
    seleccionable_en_pos BOOLEAN NOT NULL DEFAULT false,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_articulos_nombre ON articulos(nombre);
CREATE INDEX idx_articulos_activo ON articulos(activo);
CREATE INDEX idx_articulos_dimension ON articulos(dimension);
CREATE TRIGGER update_articulos_updated_at
    BEFORE UPDATE ON articulos FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 4. TABLA: preparaciones  (sub-recetas que generan stock)
-- ============================================================================
CREATE TABLE preparaciones (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    dimension dimension_unidad NOT NULL,
    unidad_base TEXT NOT NULL,                 -- 'g' | 'ml' | 'un'
    rendimiento_lote NUMERIC(14,4) NOT NULL DEFAULT 1 CHECK (rendimiento_lote > 0),
    costo_lote NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_lote >= 0),
    costo_base NUMERIC(14,6) NOT NULL DEFAULT 0 CHECK (costo_base >= 0),
    stock_base NUMERIC(14,4) NOT NULL DEFAULT 0,
    seleccionable_en_pos BOOLEAN NOT NULL DEFAULT false,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_preparaciones_nombre ON preparaciones(nombre);
CREATE INDEX idx_preparaciones_activo ON preparaciones(activo);
CREATE TRIGGER update_preparaciones_updated_at
    BEFORE UPDATE ON preparaciones FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 5. TABLA: preparacion_componentes  (receta de una preparación)
-- ============================================================================
CREATE TABLE preparacion_componentes (
    id SERIAL PRIMARY KEY,
    preparacion_id INTEGER NOT NULL REFERENCES preparaciones(id) ON DELETE CASCADE,
    tipo_componente tipo_componente NOT NULL,  -- ARTICULO | PREPARACION (anidación)
    componente_id INTEGER NOT NULL,
    cantidad_base NUMERIC(14,4) NOT NULL CHECK (cantidad_base > 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_prep_comp_prep_id ON preparacion_componentes(preparacion_id);
CREATE INDEX idx_prep_comp_componente ON preparacion_componentes(componente_id, tipo_componente);
CREATE TRIGGER update_prep_comp_updated_at
    BEFORE UPDATE ON preparacion_componentes FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 6. TABLA: recetas_menu  (rehecha: artículos/preparaciones en unidad base)
-- ============================================================================
CREATE TABLE recetas_menu (
    id SERIAL PRIMARY KEY,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    tipo_componente tipo_componente NOT NULL,  -- ARTICULO | PREPARACION
    componente_id INTEGER NOT NULL,
    cantidad_base NUMERIC(14,4) NOT NULL CHECK (cantidad_base > 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_recetas_menu_item ON recetas_menu(item_menu_id);
CREATE INDEX idx_recetas_menu_componente ON recetas_menu(componente_id, tipo_componente);
CREATE TRIGGER update_recetas_menu_updated_at
    BEFORE UPDATE ON recetas_menu FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 7. items_menu: agregar costo_teorico + categoria
-- ============================================================================
ALTER TABLE items_menu ADD COLUMN IF NOT EXISTS costo_teorico NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE items_menu ADD COLUMN IF NOT EXISTS categoria TEXT NOT NULL DEFAULT '';

-- ============================================================================
-- 8. TABLA: modificadores  (doble, quitar, reemplazar, extra)
-- ============================================================================
CREATE TABLE modificadores (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    tipo tipo_modificador NOT NULL,
    item_menu_id INTEGER REFERENCES items_menu(id) ON DELETE CASCADE, -- NULL = aplica a todos
    delta_precio NUMERIC(12,2) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_modificadores_item ON modificadores(item_menu_id);
CREATE INDEX idx_modificadores_activo ON modificadores(activo);
CREATE TRIGGER update_modificadores_updated_at
    BEFORE UPDATE ON modificadores FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 9. TABLA: modificador_componentes  (delta de receta del modificador)
-- ============================================================================
CREATE TABLE modificador_componentes (
    id SERIAL PRIMARY KEY,
    modificador_id INTEGER NOT NULL REFERENCES modificadores(id) ON DELETE CASCADE,
    accion accion_modificador NOT NULL,        -- AGREGAR | QUITAR
    tipo_componente tipo_componente NOT NULL,  -- ARTICULO | PREPARACION
    componente_id INTEGER NOT NULL,
    cantidad_base NUMERIC(14,4) NOT NULL CHECK (cantidad_base > 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_mod_comp_mod_id ON modificador_componentes(modificador_id);
CREATE TRIGGER update_mod_comp_updated_at
    BEFORE UPDATE ON modificador_componentes FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 10. TABLA: promociones  (creador manual)
-- ============================================================================
CREATE TABLE promociones (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    tipo tipo_promocion NOT NULL,
    precio NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio >= 0),   -- precio final (COMBO)
    descuento_pct NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (descuento_pct >= 0 AND descuento_pct <= 100),
    fecha_inicio DATE,
    fecha_fin DATE,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_promociones_activo ON promociones(activo);
CREATE TRIGGER update_promociones_updated_at
    BEFORE UPDATE ON promociones FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 11. TABLA: promocion_items
-- ============================================================================
CREATE TABLE promocion_items (
    id SERIAL PRIMARY KEY,
    promocion_id INTEGER NOT NULL REFERENCES promociones(id) ON DELETE CASCADE,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_promocion_items_promo ON promocion_items(promocion_id);
CREATE TRIGGER update_promocion_items_updated_at
    BEFORE UPDATE ON promocion_items FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 12. TABLA: papa_rendimientos  (formulario para cargar pesos - QF1)
-- ============================================================================
CREATE TABLE papa_rendimientos (
    id SERIAL PRIMARY KEY,
    articulo_id INTEGER NOT NULL REFERENCES articulos(id) ON DELETE CASCADE,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    peso_crudo NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (peso_crudo >= 0),
    peso_pelado NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (peso_pelado >= 0),
    peso_prefrito NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (peso_prefrito >= 0),
    peso_frito NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (peso_frito >= 0),
    rendimiento NUMERIC(6,4) NOT NULL DEFAULT 0,   -- peso_frito / peso_crudo
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_papa_rend_articulo ON papa_rendimientos(articulo_id);
CREATE INDEX idx_papa_rend_fecha ON papa_rendimientos(fecha);
CREATE TRIGGER update_papa_rend_updated_at
    BEFORE UPDATE ON papa_rendimientos FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 13. items_venta_menu: trazabilidad de costo + modificadores + promo
-- ============================================================================
ALTER TABLE items_venta_menu ADD COLUMN IF NOT EXISTS costo_unitario NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE items_venta_menu ADD COLUMN IF NOT EXISTS modificadores TEXT NOT NULL DEFAULT '';
ALTER TABLE items_venta_menu ADD COLUMN IF NOT EXISTS promocion_id INTEGER REFERENCES promociones(id) ON DELETE SET NULL;

-- ============================================================================
-- FIN DEL SCHEMA FASE 4
-- ============================================================================
