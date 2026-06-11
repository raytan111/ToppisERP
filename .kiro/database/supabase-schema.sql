-- ============================================================================
-- ToppisERP - Supabase PostgreSQL Schema
-- Versión: 1.0
-- Fecha: 2026-06-08
-- Autor: Kiro + andreslh
-- Proyecto: https://dkgqrbxizegipxdsypzf.supabase.co
-- ============================================================================
-- IMPORTANTE: Ejecutar este script en Supabase SQL Editor
-- Orden: Enums → Trigger → Tablas
-- ============================================================================

-- ============================================================================
-- 1. ENUMERACIONES
-- ============================================================================

CREATE TYPE rol AS ENUM ('ADMIN', 'CAJERO');
CREATE TYPE tipo_movimiento AS ENUM ('INGRESO', 'EGRESO', 'TRANSFERENCIA');
CREATE TYPE metodo_pago AS ENUM ('EFECTIVO', 'DEBITO');
CREATE TYPE estado_venta AS ENUM ('COMPLETADA', 'ANULADA');
CREATE TYPE tipo_componente AS ENUM ('INGREDIENTE', 'INSUMO');
CREATE TYPE estado_comanda AS ENUM ('PENDIENTE', 'ENTREGADA');

-- ============================================================================
-- 2. TRIGGER PARA updated_at AUTOMÁTICO
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. TABLAS CON AUDITORÍA
-- ============================================================================

-- 3.1 TABLA: usuarios
-- ============================================================================

CREATE TABLE usuarios (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    nombre TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    rol rol NOT NULL DEFAULT 'CAJERO',
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);

CREATE TRIGGER update_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.2 TABLA: sobres
-- ============================================================================

CREATE TABLE sobres (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    saldo NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_sobres_nombre ON sobres(nombre);

CREATE TRIGGER update_sobres_updated_at
    BEFORE UPDATE ON sobres
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.3 TABLA: movimientos_sobre
-- ============================================================================

CREATE TABLE movimientos_sobre (
    id SERIAL PRIMARY KEY,
    sobre_id INTEGER NOT NULL REFERENCES sobres(id) ON DELETE RESTRICT,
    tipo tipo_movimiento NOT NULL,
    monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    concepto TEXT NOT NULL,
    sobre_destino_id INTEGER REFERENCES sobres(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT chk_transferencia_destino CHECK (
        tipo != 'TRANSFERENCIA' OR sobre_destino_id IS NOT NULL
    )
);

CREATE INDEX idx_movimientos_sobre_sobre_id ON movimientos_sobre(sobre_id);
CREATE INDEX idx_movimientos_sobre_fecha ON movimientos_sobre(fecha);
CREATE INDEX idx_movimientos_sobre_tipo ON movimientos_sobre(tipo);

CREATE TRIGGER update_movimientos_sobre_updated_at
    BEFORE UPDATE ON movimientos_sobre
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.4 TABLA: insumos
-- ============================================================================

CREATE TABLE insumos (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    unidad_medida TEXT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_insumos_nombre ON insumos(nombre);
CREATE INDEX idx_insumos_activo ON insumos(activo);

CREATE TRIGGER update_insumos_updated_at
    BEFORE UPDATE ON insumos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.5 TABLA: ingredientes
-- ============================================================================

CREATE TABLE ingredientes (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    stock_gramos INTEGER NOT NULL DEFAULT 0 CHECK (stock_gramos >= 0),
    precio_gramo NUMERIC(12, 4) NOT NULL CHECK (precio_gramo >= 0),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_ingredientes_nombre ON ingredientes(nombre);
CREATE INDEX idx_ingredientes_activo ON ingredientes(activo);

CREATE TRIGGER update_ingredientes_updated_at
    BEFORE UPDATE ON ingredientes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.6 TABLA: items_menu
-- ============================================================================

CREATE TABLE items_menu (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_items_menu_nombre ON items_menu(nombre);
CREATE INDEX idx_items_menu_activo ON items_menu(activo);

CREATE TRIGGER update_items_menu_updated_at
    BEFORE UPDATE ON items_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.7 TABLA: recetas_menu
-- ============================================================================

CREATE TABLE recetas_menu (
    id SERIAL PRIMARY KEY,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    componente_id INTEGER NOT NULL,
    tipo_componente tipo_componente NOT NULL,
    cantidad_gramos INTEGER NOT NULL CHECK (cantidad_gramos > 0),
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

-- 3.8 TABLA: salsas
-- ============================================================================

CREATE TABLE salsas (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_salsas_nombre ON salsas(nombre);
CREATE INDEX idx_salsas_activo ON salsas(activo);

CREATE TRIGGER update_salsas_updated_at
    BEFORE UPDATE ON salsas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.9 TABLA: ventas
-- ============================================================================

CREATE TABLE ventas (
    id SERIAL PRIMARY KEY,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    metodo_pago metodo_pago NOT NULL,
    sobre_id INTEGER NOT NULL REFERENCES sobres(id) ON DELETE RESTRICT,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    estado estado_venta NOT NULL DEFAULT 'COMPLETADA',
    incluir_envio BOOLEAN NOT NULL DEFAULT false,
    monto_envio NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (monto_envio >= 0),
    stickers_enviados INTEGER NOT NULL DEFAULT 0 CHECK (stickers_enviados >= 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_ventas_fecha ON ventas(fecha);
CREATE INDEX idx_ventas_sobre_id ON ventas(sobre_id);
CREATE INDEX idx_ventas_usuario_id ON ventas(usuario_id);
CREATE INDEX idx_ventas_estado ON ventas(estado);

CREATE TRIGGER update_ventas_updated_at
    BEFORE UPDATE ON ventas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.10 TABLA: items_venta_menu
-- ============================================================================

CREATE TABLE items_venta_menu (
    id SERIAL PRIMARY KEY,
    venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12, 2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_items_venta_menu_venta_id ON items_venta_menu(venta_id);
CREATE INDEX idx_items_venta_menu_item_menu_id ON items_venta_menu(item_menu_id);

CREATE TRIGGER update_items_venta_menu_updated_at
    BEFORE UPDATE ON items_venta_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.11 TABLA: comandas
-- ============================================================================

CREATE TABLE comandas (
    id SERIAL PRIMARY KEY,
    venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    detalle_texto TEXT NOT NULL,
    estado estado_comanda NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_comandas_venta_id ON comandas(venta_id);
CREATE INDEX idx_comandas_fecha ON comandas(fecha);
CREATE INDEX idx_comandas_estado ON comandas(estado);

CREATE TRIGGER update_comandas_updated_at
    BEFORE UPDATE ON comandas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.12 TABLA: gastos
-- ============================================================================

CREATE TABLE gastos (
    id BIGSERIAL PRIMARY KEY,
    descripcion TEXT NOT NULL,
    monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    categoria TEXT NOT NULL CHECK (categoria IN (
        'INSUMOS', 'SUELDOS', 'SERVICIOS', 'ARRIENDO', 
        'TRANSPORTE', 'ENVIOS', 'PACKAGING', 'OTROS'
    )),
    sobre_id INTEGER REFERENCES sobres(id) ON DELETE SET NULL,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    comprobante TEXT,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_gastos_sobre_id ON gastos(sobre_id);
CREATE INDEX idx_gastos_usuario_id ON gastos(usuario_id);
CREATE INDEX idx_gastos_fecha ON gastos(fecha);
CREATE INDEX idx_gastos_categoria ON gastos(categoria);

CREATE TRIGGER update_gastos_updated_at
    BEFORE UPDATE ON gastos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 3.13 TABLA: presupuestos
-- ============================================================================

CREATE TABLE presupuestos (
    id SERIAL PRIMARY KEY,
    mes INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    anio INTEGER NOT NULL CHECK (anio >= 2020),
    categoria_gasto TEXT NOT NULL CHECK (categoria_gasto IN (
        'INSUMOS', 'SUELDOS', 'SERVICIOS', 'ARRIENDO', 
        'TRANSPORTE', 'ENVIOS', 'PACKAGING', 'OTROS'
    )),
    monto_presupuestado NUMERIC(12, 2) NOT NULL CHECK (monto_presupuestado >= 0),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    UNIQUE(mes, anio, categoria_gasto)
);

CREATE INDEX idx_presupuestos_periodo ON presupuestos(anio, mes);
CREATE INDEX idx_presupuestos_categoria ON presupuestos(categoria_gasto);

CREATE TRIGGER update_presupuestos_updated_at
    BEFORE UPDATE ON presupuestos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- FIN DEL SCHEMA - 13 tablas creadas con auditoría
-- ============================================================================
