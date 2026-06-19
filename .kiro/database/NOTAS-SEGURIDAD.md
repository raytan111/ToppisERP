# Revisión de seguridad — ToppisERP

Fecha: 2026-06-19. Alcance: manejo de secretos, autenticación, RLS, Edge
Function, validación de inputs.

## ✅ Lo que está bien

- **Secretos**: `local.properties` y `.env*` están en `.gitignore` y NO se
  commitean. La app solo usa la **anon key** (pública por diseño). La
  **service_role key** NO está en la app ni en el repo: vive solo en el entorno
  de la Edge Function de Supabase.
- **Edge Function `admin-usuarios`**: valida el JWT del llamante y exige rol
  ADMIN antes de borrar cuentas / cambiar contraseñas. Rechaza operar sobre la
  propia cuenta.
- **Inyección SQL**: no hay SQL crudo con input del usuario. Todo pasa por
  Postgrest (parametrizado) o RPCs con parámetros tipados.
- **Contraseñas**: mínimo 6 (regla de Supabase); la app valida en el formulario.
- **Validación de inputs numéricos**: los parseos usan `toDoubleOrNull()` /
  `toIntOrNull()` con guardas, evitando crashes por texto inválido.
- **RLS activada** en las tablas y rol resuelto server-side con `get_user_rol()`.

## ⚠️ Hallazgos / recomendaciones

### 1. La autorización por rol es mayormente client-side (MEDIA)
La navegación y los permisos (crear/editar/borrar) se aplican en la app
(`Permisos.kt`). En la base, muchas tablas permiten a **cualquier usuario
autenticado** leer (`USING true`) y escribir (`auth.uid() IS NOT NULL`). Un
usuario con su token podría, vía API, modificar datos que la UI no le muestra.

- Riesgo real: BAJO en la práctica (todos los usuarios son staff con cuentas que
  crea el admin), pero no es defensa en profundidad.
- Recomendación: si más adelante hay franquiciados u operadores menos confiables,
  endurecer RLS para validar rol y `local_id` en INSERT/UPDATE/DELETE de las
  tablas sensibles (ventas, gastos, sobres, inventario, etc.).

### 2. RLS de `gastos` desactualizada para los roles nuevos (FUNCIONAL)
Las políticas de `gastos` solo contemplan `ADMIN` y `CAJERO`. Con los roles
nuevos, un **ADMIN_LOCAL no vería gastos** y un SUPERVISOR tampoco. Hay un script
para alinearlo: `supabase-fix-rls-gastos-roles.sql`.

### 3. Confirmación de email (OPERATIVO)
Mantener **"Confirm email" desactivado** en Auth para staff interno (ver
`NOTAS-SUPABASE-AUTH.md`), o confirmar manualmente. Si se reactiva, los usuarios
nuevos no podrán entrar.

### 4. `scope por local` server-side (BAJA)
El `local_id` se sella desde la app (`LocalSession`). La base no fuerza que un
usuario solo escriba en su local. Igual que el punto 1: aceptable para staff,
endurecer si se abre a terceros.

## Checklist para producción (si se escala)
- [ ] RLS por rol + local en tablas transaccionales (no solo client-side).
- [ ] Rate limiting / monitoreo de la Edge Function.
- [ ] Rotar claves si alguna vez se filtraron.
- [ ] Backups automáticos de la base (Supabase los provee; verificar plan).
