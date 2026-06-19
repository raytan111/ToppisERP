# Edge Function: admin-usuarios

Operaciones de administración de usuarios que requieren `service_role`:
- **Eliminar** la cuenta de Auth por completo (`auth.users` + perfil por CASCADE).
- **Resetear la contraseña** de un usuario.

Solo un usuario con rol **ADMIN** puede invocarla (la función valida el JWT del llamante).

---

## Requisitos (una sola vez)

1. Instalar el CLI de Supabase: https://supabase.com/docs/guides/cli
   - macOS: `brew install supabase/tap/supabase`
2. Loguearte: `supabase login`
3. Enlazar el proyecto (ref del proyecto, lo ves en la URL del dashboard):
   ```
   supabase link --project-ref dkgqrbxizegjpxdsypzf
   ```

## Desplegar la función

Desde la raíz del repo:
```
supabase functions deploy admin-usuarios
```

No hace falta configurar secretos: `SUPABASE_URL`, `SUPABASE_ANON_KEY` y
`SUPABASE_SERVICE_ROLE_KEY` ya están disponibles como variables de entorno en
el runtime de Edge Functions de Supabase.

## Verificar

En el Dashboard → Edge Functions debería aparecer `admin-usuarios` como
desplegada. Desde la app, probá:
- Eliminar un usuario (que no seas vos) → debería borrarse por completo.
- Cambiar contraseña de un usuario → luego ese usuario entra con la nueva.

## Notas

- Mientras la función NO esté desplegada, los botones de **eliminar** y
  **cambiar contraseña** mostrarán: "La función admin-usuarios no está
  desplegada en Supabase". El resto de la app funciona igual.
- La función rechaza operar sobre tu propia cuenta (evita auto-bloqueo).
