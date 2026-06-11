-- ============================================================================
-- ToppisERP - Row Level Security (RLS) Policies
-- Versión: 1.0
-- Fecha: 2026-06-08
-- Proyecto: https://dkgqrbxizegipxdsypzf.supabase.co
-- ============================================================================
-- IMPORTANTE: Ejecutar DESPUÉS de supabase-schema.sql
-- ============================================================================

-- ============================================================================
-- 1. HELPER FUNCTION: Obtener rol del usuario actual
-- ============================================================================

CREATE OR REPLACE FUNCTION get_user_rol()
RETURNS rol AS $$
BEGIN
    RETURN (SELECT rol FROM usuarios WHERE id = auth.uid());
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 2. HABILITAR RLS EN TODAS LAS TABLAS
-- ============================================================================

ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE sobres ENABLE ROW LEVEL SECURITY;
ALTER TABLE movimientos_sobre ENABLE ROW LEVEL SECURITY;
ALTER TABLE insumos ENABLE ROW LEVEL SECURITY;
ALTER TABLE ingredientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE items_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE recetas_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE salsas ENABLE ROW LEVEL SECURITY;
ALTER TABLE ventas ENABLE ROW LEVEL SECURITY;
ALTER TABLE items_venta_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE comandas ENABLE ROW LEVEL SECURITY;
ALTER TABLE gastos ENABLE ROW LEVEL SECURITY;
ALTER TABLE presupuestos ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- 3. POLÍTICAS: usuarios
-- ============================================================================

CREATE POLICY "Usuarios ven su propio perfil"
ON usuarios FOR SELECT
USING (id = auth.uid());

CREATE POLICY "Admins ven todos los usuarios"
ON usuarios FOR SELECT
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins crean usuarios"
ON usuarios FOR INSERT
WITH CHECK (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins actualizan usuarios"
ON usuarios FOR UPDATE
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins eliminan usuarios"
ON usuarios FOR DELETE
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 4. POLÍTICAS: sobres
-- ============================================================================

CREATE POLICY "Todos ven sobres"
ON sobres FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean sobres"
ON sobres FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Usuarios autenticados actualizan sobres"
ON sobres FOR UPDATE
USING (auth.uid() IS NOT NULL);

CREATE POLICY "Solo admins eliminan sobres"
ON sobres FOR DELETE
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 5. POLÍTICAS: movimientos_sobre
-- ============================================================================

CREATE POLICY "Todos ven movimientos"
ON movimientos_sobre FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean movimientos"
ON movimientos_sobre FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- ============================================================================
-- 6. POLÍTICAS: insumos
-- ============================================================================

CREATE POLICY "Todos ven insumos"
ON insumos FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican insumos"
ON insumos FOR ALL
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 7. POLÍTICAS: ingredientes
-- ============================================================================

CREATE POLICY "Todos ven ingredientes"
ON ingredientes FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican ingredientes"
ON ingredientes FOR ALL
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 8. POLÍTICAS: items_menu
-- ============================================================================

CREATE POLICY "Todos ven items menú"
ON items_menu FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican items menú"
ON items_menu FOR ALL
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 9. POLÍTICAS: recetas_menu
-- ============================================================================

CREATE POLICY "Todos ven recetas"
ON recetas_menu FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican recetas"
ON recetas_menu FOR ALL
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 10. POLÍTICAS: salsas
-- ============================================================================

CREATE POLICY "Todos ven salsas"
ON salsas FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican salsas"
ON salsas FOR ALL
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 11. POLÍTICAS: ventas
-- ============================================================================

CREATE POLICY "Todos ven ventas"
ON ventas FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean ventas"
ON ventas FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Solo admins actualizan ventas"
ON ventas FOR UPDATE
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins eliminan ventas"
ON ventas FOR DELETE
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 12. POLÍTICAS: items_venta_menu
-- ============================================================================

CREATE POLICY "Todos ven items venta"
ON items_venta_menu FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean items venta"
ON items_venta_menu FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- ============================================================================
-- 13. POLÍTICAS: comandas
-- ============================================================================

CREATE POLICY "Todos ven comandas"
ON comandas FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados gestionan comandas"
ON comandas FOR ALL
USING (auth.uid() IS NOT NULL);

-- ============================================================================
-- 14. POLÍTICAS: gastos (RLS más restrictivo)
-- ============================================================================

CREATE POLICY "Admins ven todos los gastos"
ON gastos FOR SELECT
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Cajeros ven sus gastos"
ON gastos FOR SELECT
USING (get_user_rol() = 'CAJERO' AND usuario_id = auth.uid());

CREATE POLICY "Usuarios autenticados crean gastos"
ON gastos FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Solo admins modifican gastos"
ON gastos FOR UPDATE
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins eliminan gastos"
ON gastos FOR DELETE
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- 15. POLÍTICAS: presupuestos
-- ============================================================================

CREATE POLICY "Solo admins gestionan presupuestos"
ON presupuestos FOR ALL
USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- FIN DE POLÍTICAS RLS
-- ============================================================================
