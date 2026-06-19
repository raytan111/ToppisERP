-- ============================================================================
-- ToppisERP - Import histórico (PASO 3): ventas reales Mar–Jun 2026
-- ============================================================================
-- Carga 99 ventas reales (canal WhatsApp) como registros históricos en texto
-- libre: sin ítems de menú y sin descontar stock. Para reportes/KPIs/historial.
--
-- ORDEN DE EJECUCIÓN:
--   1) supabase-import-01-columnas-ventas.sql   (columnas nuevas)
--   2) supabase-clean-slate.sql                 (dejar la app limpia)
--   3) este script
--
-- total        = "Total cobrado" del Excel (incluye delivery, igual que la app)
-- monto_envio  = columna Delivery
-- modo_entrega = RETIRO | DELIVERY
-- origen       = IMPORT_HISTORICO
-- ============================================================================

-- 1) Asegurar el local único "Toppis Burgers"
INSERT INTO locales (nombre, direccion, activo)
SELECT 'Toppis Burgers', '', true
WHERE NOT EXISTS (SELECT 1 FROM locales WHERE nombre = 'Toppis Burgers');

-- 2) Insertar las ventas históricas
INSERT INTO ventas
    (fecha, total, metodo_pago, sobre_id, estado, incluir_envio,
     monto_envio, modo_entrega, canal, descripcion, origen, local_id)
SELECT
    (d.fecha || ' 12:00:00')::timestamptz,
    d.total,
    NULL,                       -- método de pago desconocido
    NULL,                       -- sobre desconocido (histórico)
    'COMPLETADA',
    d.envio > 0,
    d.envio,
    d.modo,
    'WhatsApp',
    d.descripcion,
    'IMPORT_HISTORICO',
    (SELECT id FROM locales WHERE nombre = 'Toppis Burgers' LIMIT 1)
FROM (VALUES
    ('2026-03-18', 14000, 0,    'RETIRO',   'Completo XL italiano + Completo completo x2 + Bebida sprite + Papa chica'),
    ('2026-03-19', 7500,  1000, 'DELIVERY', 'Promo XL'),
    ('2026-03-19', 8500,  1000, 'DELIVERY', 'Papa Cheddar + Cup cheddar'),
    ('2026-03-19', 1500,  0,    'RETIRO',   'Completo chucrut'),
    ('2026-03-19', 2000,  0,    'RETIRO',   'Completo italiano (Andres)'),
    ('2026-03-20', 11990, 1000, 'DELIVERY', 'Promo precisa'),
    ('2026-03-20', 11490, 500,  'DELIVERY', 'Promo precisa'),
    ('2026-03-20', 4000,  0,    'RETIRO',   'Completo XL italiano'),
    ('2026-03-20', 2500,  0,    'RETIRO',   'Completo italiano'),
    ('2026-03-21', 3000,  0,    'RETIRO',   'Promo 2 completo aleman'),
    ('2026-03-21', 11490, 500,  'DELIVERY', 'Promo precisa'),
    ('2026-03-21', 11490, 500,  'DELIVERY', 'Promo precisa'),
    ('2026-03-21', 5500,  1000, 'DELIVERY', 'Promo la solitaria'),
    ('2026-03-21', 5500,  0,    'RETIRO',   'Salchipapa'),
    ('2026-03-21', 5500,  1000, 'DELIVERY', 'Promo individual'),
    ('2026-03-25', 24500, 500,  'DELIVERY', 'Cheddar mediana + Salchipapa + Promo XL italiano + Promo chica italiana'),
    ('2026-03-26', 9000,  1500, 'DELIVERY', 'Promo XL'),
    ('2026-03-26', 4500,  0,    'RETIRO',   'Completo aleman + Papas fritas chicas x2'),
    ('2026-03-27', 8000,  500,  'DELIVERY', 'Promo XL'),
    ('2026-03-27', 7500,  0,    'RETIRO',   'Completo normal x5'),
    ('2026-03-28', 11490, 500,  'DELIVERY', 'Promo precisa'),
    ('2026-03-28', 1500,  0,    'RETIRO',   'Completo aleman'),
    ('2026-03-28', 4500,  500,  'DELIVERY', 'Completo XL'),
    ('2026-03-28', 15000, 1500, 'DELIVERY', '3 Promos individuales'),
    ('2026-04-01', 20000, 0,    'RETIRO',   '2 Promos XL + 2 Papas fritas chicas + Bebida 1L'),
    ('2026-04-01', 13490, 1000, 'DELIVERY', 'Promo suprema'),
    ('2026-04-01', 9000,  1500, 'DELIVERY', 'Promo XL'),
    ('2026-04-02', 12000, 0,    'RETIRO',   'Promo XL + Promo normal'),
    ('2026-04-02', 7500,  1000, 'DELIVERY', 'Cheddar mediana'),
    ('2026-04-02', 5000,  500,  'DELIVERY', 'Promo individual'),
    ('2026-04-02', 6000,  500,  'DELIVERY', 'Salchipapa'),
    ('2026-04-02', 9500,  500,  'DELIVERY', '2 Promos normales'),
    ('2026-04-02', 8000,  500,  'DELIVERY', 'Promo XL'),
    ('2026-04-04', 13000, 0,    'RETIRO',   'Cheddar mediana + Bechamel mediana'),
    ('2026-04-10', 7500,  1000, 'DELIVERY', 'Papas cheddar'),
    ('2026-04-10', 4500,  0,    'RETIRO',   'Promo normal italiano'),
    ('2026-04-11', 7500,  1000, 'DELIVERY', 'Papas cheddar'),
    ('2026-04-11', 9500,  500,  'DELIVERY', '2 Promos individuales'),
    ('2026-04-11', 3500,  0,    'RETIRO',   'Completo normal italiano + Papas chicas'),
    ('2026-04-16', 9500,  0,    'RETIRO',   'Completo normal x4 + Completo normal papa + Completo italiano mama'),
    ('2026-04-18', 5500,  0,    'RETIRO',   'Salchipapa'),
    ('2026-04-18', 10000, 500,  'DELIVERY', 'Promo precisa'),
    ('2026-04-18', 1500,  0,    'RETIRO',   'Completo normal'),
    ('2026-04-22', 6000,  1500, 'DELIVERY', 'Promo normales'),
    ('2026-04-24', 5000,  500,  'DELIVERY', 'Promo individual'),
    ('2026-05-06', 9500,  2000, 'DELIVERY', 'Cheddar mediana'),
    ('2026-05-07', 7500,  1000, 'DELIVERY', 'Cheddar mediana'),
    ('2026-05-07', 3000,  0,    'RETIRO',   'Completo normal x2'),
    ('2026-05-08', 38500, 500,  'DELIVERY', '4 Promos precisas'),
    ('2026-05-08', 11400, 500,  'DELIVERY', 'Salchipapa Grande'),
    ('2026-05-09', 5500,  1000, 'DELIVERY', 'Promo individual'),
    ('2026-05-16', 8000,  0,    'RETIRO',   'Smokehouse'),
    ('2026-05-17', 7400,  1000, 'DELIVERY', 'Smokehouse'),
    ('2026-05-17', 5500,  1000, 'DELIVERY', 'Promo individual'),
    ('2026-05-17', 5000,  500,  'DELIVERY', 'Promo individual'),
    ('2026-05-17', 13375, 500,  'DELIVERY', 'Smokehouse + Bechamel mediana'),
    ('2026-05-17', 7300,  0,    'RETIRO',   'Smokehouse (mami) + Classic burger (papi)'),
    ('2026-05-20', 8490,  500,  'DELIVERY', 'Combo SMK'),
    ('2026-05-20', 8990,  1000, 'DELIVERY', 'Combo SMK'),
    ('2026-05-21', 15980, 0,    'RETIRO',   'Combo SMK + Combo Toppi'),
    ('2026-05-22', 7500,  1000, 'DELIVERY', 'Cheddar mediana'),
    ('2026-05-22', 7480,  500,  'DELIVERY', 'Promo aleman + Promo italiano'),
    ('2026-05-22', 9500,  2000, 'DELIVERY', 'Cheddar mediana + Cup cheddar'),
    ('2026-05-23', 25480, 0,    'RETIRO',   'Duo smash + Combo classic + Promo aleman'),
    ('2026-05-23', 12990, 1000, 'DELIVERY', 'Bechamel grande'),
    ('2026-05-28', 6500,  1500, 'DELIVERY', 'Dos completos italiano'),
    ('2026-05-28', 5990,  1000, 'DELIVERY', 'Smokehouse'),
    ('2026-05-28', 17470, 1500, 'DELIVERY', 'Toppi brgr x2 + Smk brgr + Cup cheddar'),
    ('2026-05-28', 10500, 500,  'DELIVERY', 'Toppi burger + Classic'),
    ('2026-05-29', 13000, 500,  'DELIVERY', 'Promo suprema'),
    ('2026-05-29', 12000, 0,    'RETIRO',   'Bechamel grande'),
    ('2026-05-30', 9500,  500,  'DELIVERY', 'Cheddar mediana + Completo italiano'),
    ('2026-05-30', 13000, 500,  'DELIVERY', 'Promo suprema'),
    ('2026-05-30', 10000, 500,  'DELIVERY', 'Promo precisa'),
    ('2026-06-03', 5500,  1000, 'DELIVERY', 'Combo individual'),
    ('2026-06-03', 28480, 1500, 'DELIVERY', 'Duo smash + Duo cheese'),
    ('2026-06-03', 8500,  1000, 'DELIVERY', 'Combo Classic'),
    ('2026-06-04', 12000, 500,  'DELIVERY', '4 completos + Papa chica'),
    ('2026-06-04', 8800,  1000, 'DELIVERY', 'Salchipapa mediana + Completo aleman'),
    ('2026-06-04', 2000,  0,    'RETIRO',   'Completo italiano'),
    ('2026-06-05', 19980, 500,  'DELIVERY', '2 burgers + Salchipapa'),
    ('2026-06-05', 9000,  1500, 'DELIVERY', 'Cheddar mediana + Cup de cheddar'),
    ('2026-06-05', 6500,  1000, 'DELIVERY', 'Salchipapa'),
    ('2026-06-10', 14980, 0,    'RETIRO',   'Classic Burger + Italiana Brgr'),
    ('2026-06-11', 13490, 1500, 'DELIVERY', 'Promo cheese'),
    ('2026-06-11', 25490, 1000, 'DELIVERY', 'Promo duo smash + Smk doble'),
    ('2026-06-11', 13500, 1500, 'DELIVERY', 'Duo cheese'),
    ('2026-06-11', 5000,  0,    'RETIRO',   '2 Completos'),
    ('2026-06-12', 12000, 500,  'DELIVERY', '2 completos + Cheddar mediana'),
    ('2026-06-12', 28580, 1500, 'DELIVERY', 'Dos completo completo + Duo Smash + Combo Cheese'),
    ('2026-06-13', 13490, 1500, 'DELIVERY', 'Duo cheese'),
    ('2026-06-13', 7990,  1500, 'DELIVERY', 'Combo cheese'),
    ('2026-06-13', 6500,  2000, 'DELIVERY', 'Combo individual'),
    ('2026-06-13', 22990, 1500, 'DELIVERY', 'Duo smash + Bechamel mediana'),
    ('2026-06-13', 15500, 500,  'DELIVERY', 'Duo smash'),
    ('2026-06-13', 6500,  1000, 'DELIVERY', 'Salchipapa mediana'),
    ('2026-06-13', 15990, 1000, 'DELIVERY', 'Duo smash'),
    ('2026-06-17', 9500,  1000, 'DELIVERY', 'Combo Cheese brgr + 2ble brgr'),
    ('2026-06-17', 9500,  2000, 'DELIVERY', 'Smokehouse')
) AS d(fecha, total, envio, modo, descripcion);

-- 3) Verificación
SELECT
    count(*)            AS ventas_importadas,
    sum(total)          AS total_cobrado,
    sum(monto_envio)    AS total_delivery,
    min(fecha)::date    AS desde,
    max(fecha)::date    AS hasta
FROM ventas
WHERE origen = 'IMPORT_HISTORICO';
-- Esperado: 99 ventas, total_cobrado = 1.003.545, delivery = 68.500,
--           desde 2026-03-18, hasta 2026-06-17.
-- (Nota: el detalle real son 99 líneas que suman 1.003.545. Las hojas de
--  resumen del Excel mostraban 1.011.535 con 96 pedidos por una pequeña
--  inconsistencia interna; acá usamos el detalle, que es la fuente real.)
