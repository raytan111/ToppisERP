# Fase 9 — Multi-Local / Franquicia — Spec Técnica

## Estado: en implementación (por capas)
## Base: todas las fases previas (single-local)

> Objetivo: escalar a N locales (propios y franquicias) con control central.
> Es un cambio fundacional; se implementa por capas para no romper lo existente.

---

## Estrategia por capas

### Capa 1 — Registro de Locales + Local activo (FOUNDATION) ← ahora
- Tabla `locales` (nombre, dirección, activo).
- CRUD de locales (Admin).
- **Local activo**: el usuario elige con qué local opera; se guarda en el dispositivo
  (SharedPreferences) y queda disponible vía `LocalSession`.
- Columnas `local_id` (nullable) agregadas a las tablas transaccionales para
  no romper nada y dejar el esquema listo.

### Capa 2 — Sellado de transacciones (stamping)
- Las RPC (registrar_venta_menu, registrar_gasto, registrar_compra) y los inserts
  (mermas, conteos, arqueos, jornadas, propinas) reciben el `local_id` activo.
- Cada transacción nueva queda asociada a su local.

### Capa 3 — Filtrado y reportes consolidados
- Reportes por local + consolidado (todos los locales).
- Stock/artículos: decisión — ¿catálogo compartido con stock por local, o
  artículos por local? (cada local tiene su cocina → stock por local).

### Capa 4 — Roles por local + franquicia
- Usuarios asignados a uno o más locales; encargado/franquiciado ve solo su local.
- (Opcional) royalties/fees por local.

---

## Notas de diseño
- Cada local tiene su propia cocina (D2) → el stock es por local. En la capa 3
  habrá que decidir si los artículos/recetas son catálogo compartido (con stock
  por local) o totalmente por local. Recomendación: **catálogo compartido**
  (artículos, recetas, menú, precios) + **stock y movimientos por local**.
- Multi-local idealmente listo **antes** de abrir el 2º local.

---
**Fecha**: 2026-06-16
**Estado**: Capa 1 en implementación.
