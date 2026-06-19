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

### Opción A (recomendada, sin CLI): desde el Dashboard
1. Dashboard de Supabase → **Edge Functions**.
2. **Deploy a new function → Via Editor**.
3. Nombre exacto: `admin-usuarios`.
4. Pegá todo el contenido de `index.ts` (este directorio) y **Deploy**.

No hace falta configurar secretos: `SUPABASE_URL`, `SUPABASE_ANON_KEY` y
`SUPABASE_SERVICE_ROLE_KEY` ya están disponibles en el runtime.

### Opción B: CLI con npx (si tenés Node, no compila nada)
Desde la raíz del repo:
```
npx supabase login
npx supabase link --project-ref dkgqrbxizegjpxdsypzf
npx supabase functions deploy admin-usuarios
```

### Opción C: CLI por Homebrew
```
supabase functions deploy admin-usuarios
```
⚠️ `brew install supabase/tap/supabase` puede fallar si intenta compilar desde
código con Xcode/Command Line Tools viejos. En ese caso usá la Opción A o B, o
bajá el binario de https://github.com/supabase/cli/releases.

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
