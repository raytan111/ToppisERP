-- ============================================================================
-- ToppisERP - Fase 4: RLS Policies
-- Versión: 4.0
-- Fecha: 2026-06-15
-- ============================================================================
-- Ejecutar DESPUÉS de supabase-fase4-schema.sql
-- Usa la función get_user_rol() ya creada en supabase-rls.sql
-- ============================================================================

-- Habilitar RLS
ALTER TABLE articulos ENABLE ROW LEVEL SECURITY;
ALTER TABLE preparaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE preparacion_componentes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recetas_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE modificadores ENABLE ROW LEVEL SECURITY;
ALTER TABLE modificador_componentes ENABLE ROW LEVEL SECURITY;
ALTER TABLE promociones ENABLE ROW LEVEL SECURITY;
ALTER TABLE promocion_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE papa_rendimientos ENABLE ROW LEVEL SECURITY;

-- ── articulos ────────────────────────────────────────────────────────────
CREATE POLICY "Todos ven articulos" ON articulos FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican articulos" ON articulos FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── preparaciones ──────────────────────────────────────────────────────────
CREATE POLICY "Todos ven preparaciones" ON preparaciones FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican preparaciones" ON preparaciones FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── preparacion_componentes ────────────────────────────────────────────────
CREATE POLICY "Todos ven prep_componentes" ON preparacion_componentes FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican prep_componentes" ON preparacion_componentes FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── recetas_menu ─────────────────────────────────────────────────────────
CREATE POLICY "Todos ven recetas" ON recetas_menu FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican recetas" ON recetas_menu FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── modificadores ────────────────────────────────────────────────────────
CREATE POLICY "Todos ven modificadores" ON modificadores FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican modificadores" ON modificadores FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── modificador_componentes ──────────────────────────────────────────────
CREATE POLICY "Todos ven mod_componentes" ON modificador_componentes FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican mod_componentes" ON modificador_componentes FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── promociones ──────────────────────────────────────────────────────────
CREATE POLICY "Todos ven promociones" ON promociones FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican promociones" ON promociones FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── promocion_items ──────────────────────────────────────────────────────
CREATE POLICY "Todos ven promocion_items" ON promocion_items FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican promocion_items" ON promocion_items FOR ALL USING (get_user_rol() = 'ADMIN');

-- ── papa_rendimientos ────────────────────────────────────────────────────
CREATE POLICY "Todos ven papa_rendimientos" ON papa_rendimientos FOR SELECT USING (true);
CREATE POLICY "Solo admins modifican papa_rendimientos" ON papa_rendimientos FOR ALL USING (get_user_rol() = 'ADMIN');

-- ============================================================================
-- FIN RLS FASE 4
-- ============================================================================
