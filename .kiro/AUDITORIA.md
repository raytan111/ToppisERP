# Auditoría ToppisERP — 2026-06-20

Estado general: **compila sin errores**; 9 warnings de deprecación (menores).
Arquitectura consistente (MVVM + Repository + Supabase + RPC).

## Hallazgos por prioridad

### Alta (correctitud)
1. **Código muerto**: `data/repository/DashboardRepository.kt` (+ `DashboardKpi`) sin uso (se eliminó la pantalla Dashboard). → borrar.
2. **RLS de `gastos` desalineada con roles nuevos**: solo contempla ADMIN/CAJERO; un ADMIN_LOCAL no vería gastos. → correr `supabase-fix-rls-gastos-roles.sql`.
3. **Cache viejo tras cambios externos (SQL)**: solo Gastos y Comprobantes recargan al abrir. Otras pantallas pueden mostrar datos viejos si Realtime está caído. → agregar "recargar al abrir" a las listas clave (Reportes, Sobres, Inventario, y config).

### Media (seguridad)
4. **Autorización por rol mayormente client-side**; RLS con `USING(true)` amplio (aceptable para staff, endurecer si se escala).
5. **Lectura anon abierta** en algunas tablas (para el agente): esos datos quedan públicos con la anon key. Recomendación: usar `service_role` en el agente y **revertir el acceso anon** (o dejar documentadas qué tablas quedaron abiertas).
6. **Edge Function `admin-usuarios`**: confirmar que está desplegada (borrar/reset password dependen de ella).

### Baja (pulido)
7. **9 warnings**: `menuAnchor()` deprecado (6), `Locale(String)` deprecado (3), `Icons.Filled.ShowChart` (1). → fixes triviales.
8. **Diseño pendiente**: splash, tipografía de marca, animaciones, estados vacíos/carga, revisión de modo oscuro.
9. **Logo real** (reemplazar placeholder `toppis_logo.xml`).
10. **KPIs**: food cost 0 en meses históricos (esperado; agregar nota aclaratoria).

### Operativo (datos)
11. **Local único** "Toppis Burgers" + reasignar las 99 ventas.

## Plan de aplicación
- **Fase A (código, seguro):** borrar DashboardRepository + arreglar los 9 warnings + nota food-cost histórico en KPIs. (build + push)
- **Fase B (recarga al abrir):** extender a Reportes/Sobres/Inventario/config.
- **Fase C (SQL, lo corre el usuario):** fix RLS gastos; revisar acceso anon.
- **Fase D (operativo):** local único + reasignar ventas.
- **Fase E (diseño):** splash + tipografía + logo + estados vacíos + animaciones + revisión dark.
