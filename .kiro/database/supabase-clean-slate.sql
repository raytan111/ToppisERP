-- ============================================================================
-- ToppisERP - CLEAN SLATE (puesta en marcha)
-- ============================================================================
-- Borra TODOS los datos transaccionales y deja la configuración intacta para
-- empezar a operar de cero (datos reales).
--
-- BORRA:   ventas, items_venta_menu, comandas, comprobantes, gastos,
--          movimientos_sobre, mermas, conteos (+detalle), compras (+detalle),
--          arqueos, jornadas, propinas, cierres_mensuales, papa_rendimientos,
--          presupuestos.
-- RESETEA: articulos.stock_base = 0, preparaciones.stock_base = 0,
--          sobres.saldo = 0.
-- CONSERVA: usuarios, locales, usuarios_locales, articulos (config),
--           preparaciones (config), preparacion_componentes, items_menu,
--           recetas_menu, modificadores, modificador_componentes, promociones,
--           promocion_items, proveedores, empleados.
--
-- ⚠️  ACCIÓN DESTRUCTIVA E IRREVERSIBLE. Hacé un backup/export antes.
--     Ejecutar en Supabase SQL Editor.
-- ============================================================================

DO $$
DECLARE
    t text;
    tablas text[] := ARRAY[
        'items_venta_menu', 'comandas', 'comprobantes', 'ventas',
        'gastos', 'movimientos_sobre', 'mermas',
        'conteo_detalle', 'conteos', 'compra_detalle', 'compras',
        'arqueos', 'jornadas', 'propinas', 'cierres_mensuales',
        'papa_rendimientos', 'presupuestos'
    ];
BEGIN
    FOREACH t IN ARRAY tablas LOOP
        IF to_regclass('public.' || t) IS NOT NULL THEN
            EXECUTE format('TRUNCATE TABLE %I RESTART IDENTITY CASCADE', t);
            RAISE NOTICE 'Truncada: %', t;
        END IF;
    END LOOP;
END $$;

-- ── Resetear stocks y saldos ───────────────────────────────────────────────
UPDATE articulos      SET stock_base = 0;
UPDATE preparaciones  SET stock_base = 0;
UPDATE sobres         SET saldo = 0;

-- ── Verificación ───────────────────────────────────────────────────────────
SELECT
    (SELECT count(*) FROM ventas)          AS ventas,
    (SELECT count(*) FROM gastos)          AS gastos,
    (SELECT count(*) FROM movimientos_sobre) AS movimientos,
    (SELECT count(*) FROM articulos WHERE stock_base <> 0) AS articulos_con_stock,
    (SELECT count(*) FROM sobres WHERE saldo <> 0)         AS sobres_con_saldo,
    (SELECT count(*) FROM usuarios)        AS usuarios_conservados,
    (SELECT count(*) FROM items_menu)      AS items_menu_conservados;
