# Notas de Supabase Auth — Reglas para crear/iniciar usuarios

Resumen de las reglas y ajustes que afectan a la creación y el login de usuarios en ToppisERP. Tenelo a mano para no equivocarte.

---

## 1. Contraseña

- **Mínimo 6 caracteres** (regla por defecto de Supabase). Si es más corta, el signUp falla con `weak_password` ("Password should be at least 6 characters").
- La app valida 6 en el formulario y muestra "Faltan caracteres: mínimo 6".
- Se puede **subir** el mínimo (no bajar de 6) en: **Dashboard → Authentication → Sign In / Providers → Password → Minimum password length**.
- También se pueden exigir requisitos (mayúsculas, números, símbolos) en esa misma sección. Si los activás, hay que reflejarlos en el formulario de la app.

## 2. Email

- Debe tener **formato válido** (`algo@dominio.com`). La app valida que tenga `@` y `.`.
- Supabase rechaza emails con formato inválido con `email_address_invalid`.
- **No hay regla de "largo mínimo" de email** como tal; si viste un error de "corto", casi seguro era el formato o la confirmación (ver punto 3).

## 3. ⚠️ Confirmación de email (la causa más común de "no puedo entrar")

- Por defecto Supabase exige **confirmar el email por correo** antes de poder iniciar sesión.
- Como el staff no recibe ese correo, el login falla con `email_not_confirmed`.
- **SOLUCIÓN (hacelo una vez)**: Dashboard → **Authentication → Providers → Email → desactivar "Confirm email"**.
  - Así los usuarios nuevos quedan habilitados de inmediato.
- Para los usuarios ya creados que quedaron sin confirmar, corré `supabase-confirmar-emails.sql`.

## 4. Cómo crea usuarios la app (importante)

- La pantalla **Administración → Usuarios** usa `signUp` (no un panel admin con service_role).
- `signUp` **cambia temporalmente la sesión** al usuario recién creado; la app restaura la sesión del admin después (vía `importSession`). Es aceptable para uso interno.
- Tras crear en Auth, se inserta el perfil en la tabla `usuarios` (nombre, email, rol, activo).

## 5. Eliminar usuarios

- Solo **ADMIN** puede eliminar (regla RLS en `usuarios`: DELETE solo si `get_user_rol() = 'ADMIN'`).
- La app pide **confirmación** antes de borrar y **no permite eliminarte a vos mismo**.
- El borrado quita el **perfil** (`usuarios`); la cuenta de `auth.users` queda en Supabase.
  - Efecto: el usuario ya no aparece ni puede operar; el login le falla por no tener perfil.
  - Para borrar también la cuenta de Auth se necesita service_role / Edge Function (pendiente si se requiere).

## 6. Roles (enum `rol`)

- Valores válidos: `ADMIN`, `ADMIN_LOCAL`, `SUPERVISOR`, `CAJERO`.
- Agregados con `supabase-roles-v2.sql`. Recordá: un valor nuevo de enum debe estar **commiteado** antes de usarse (correr los `ALTER TYPE` solos, sin el `SELECT` en la misma corrida).

## 7. Reglas RLS clave (tabla usuarios)

- **SELECT**: cada uno ve su propio perfil; ADMIN ve todos.
- **INSERT / UPDATE / DELETE**: solo ADMIN.
- (No se modificó la RLS para ADMIN_LOCAL: la gestión de usuarios queda solo en ADMIN.)

---

## Checklist rápido si "no puedo crear / entrar"

1. ¿Contraseña de 6+ caracteres? → sí.
2. ¿Email con formato válido? → sí.
3. ¿"Confirm email" desactivado en el Dashboard? → si no, desactivalo o confirmá con `supabase-confirmar-emails.sql`.
4. ¿El rol existe en el enum? → corré `supabase-roles-v2.sql` (paso 1) si falta.
5. ¿Eliminar no funciona? → tiene que ser un usuario **ADMIN** quien lo hace.
