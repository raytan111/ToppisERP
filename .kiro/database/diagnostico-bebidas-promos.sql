-- ============================================================================
-- DIAGNÓSTICO: ¿por qué no aparecen las bebidas en las promos?
-- Corré cada bloque en el SQL Editor de Supabase y revisá los resultados.
-- El POS y las promos leen las bebidas desde items_menu (NO desde inventario).
-- Para que una bebida aparezca como opción de una promo por categoría debe:
--   1) existir en items_menu, 2) estar activo = true,
--   3) tener categoria EXACTAMENTE 'Bebida lata' o 'Bebida mediana'.
-- ============================================================================

-- 1) ¿Qué ítems de menú hay y con qué categoría? (mirá las bebidas)
--    Fijate el texto EXACTO de la columna categoria y si activo = true.
SELECT id, nombre, categoria, activo, precio
FROM items_menu
ORDER BY categoria, nombre;

-- 2) ¿Cuántos ítems de menú activos hay por categoría?
--    Debe aparecer 'Bebida lata' y/o 'Bebida mediana' con conteo > 0.
SELECT categoria, count(*) FILTER (WHERE activo) AS activos, count(*) AS total
FROM items_menu
GROUP BY categoria
ORDER BY categoria;

-- 3) ¿Cómo quedaron configurados los grupos (espacios) de las promos?
--    Para bebidas por categoría: modo = 'CATEGORIA' y categoria = 'Bebida lata'/'Bebida mediana'.
SELECT e.id, p.nombre AS promo, e.nombre AS grupo, e.modo, e.categoria,
       e.cantidad, e.permite_repetir, e.orden
FROM promocion_espacios e
JOIN promociones p ON p.id = e.promocion_id
ORDER BY p.nombre, e.orden;

-- 4) Cruce directo: por cada grupo de categoría, cuántas bebidas activas calzan.
--    Si "elegibles" = 0, el texto de categoria del grupo no coincide con el de items_menu.
SELECT e.id AS espacio_id, p.nombre AS promo, e.nombre AS grupo, e.categoria AS categoria_grupo,
       count(m.id) FILTER (WHERE m.activo) AS elegibles
FROM promocion_espacios e
JOIN promociones p ON p.id = e.promocion_id
LEFT JOIN items_menu m
       ON lower(btrim(m.categoria)) = lower(btrim(e.categoria))
WHERE e.modo = 'CATEGORIA'
GROUP BY e.id, p.nombre, e.nombre, e.categoria
ORDER BY p.nombre, e.nombre;
