# Import de ventas históricas (Mar–Jun 2026)

Cargar las ventas reales del Excel (canal WhatsApp) como historial, en texto
libre, sin mapear a ítems del menú ni descontar stock. Local único:
**Toppis Burgers**.

## Decisiones tomadas
- Una fila del Excel = una venta histórica (sin ítems de menú).
- Local único "Toppis Burgers".
- `total` = "Total cobrado" (incluye delivery, igual que la app); `monto_envio` = Delivery.
- Clean slate ANTES del import.

## Cambios en la app
- Tabla `ventas`: columnas nuevas `descripcion`, `canal`, `modo_entrega`, `origen`
  (default 'APP'); `sobre_id` y `metodo_pago` pasan a NULL-ables (las históricas
  no los tienen; la app los sigue enviando en ventas normales).
- Modelo `Venta`: `metodoPago`/`sobreId` nullables + campos nuevos.
- `VentasHistorialScreen`: muestra `descripcion`, delivery y modo de entrega
  cuando la venta no tiene ítems; método nulo se muestra como canal o "—".
- `ReportesScreen` y `ExportacionUtil`: manejan `metodoPago` nulo.

## Orden de ejecución (en Supabase SQL Editor)
1. `supabase-import-01-columnas-ventas.sql` — columnas nuevas + nullables.
2. `supabase-clean-slate.sql` — dejar la app limpia (conserva usuarios/locales/menú/configs).
3. `supabase-import-02-ventas-historicas.sql` — crea local "Toppis Burgers" + 99 ventas.

## Verificación esperada
- 99 ventas con `origen='IMPORT_HISTORICO'`.
- Suma `total` = **1.003.545** (coincide con el TOTAL del detalle del Excel).
- Suma delivery = **68.500**. Rango 2026-03-18 → 2026-06-17.
- (Las hojas de resumen del Excel mostraban 1.011.535/96 pedidos por una
  inconsistencia interna; el detalle real son 99 líneas = 1.003.545.)

## Notas
- Las históricas no tienen food cost (sin ítems/receta) → en KPIs el food cost %
  de esos meses sale bajo/0; los ingresos sí se reflejan. Es esperado.
- Las ventas que cargues operando desde la app quedan con `origen='APP'` y su
  método de pago / sobre normales.
