-- ============================================================================
-- ToppisERP - Confirmar emails (staff interno, sin verificación por correo)
-- ============================================================================
-- La app crea usuarios desde la pantalla "Usuarios" (signUp). Por defecto
-- Supabase exige confirmar el email por correo, y como el staff no recibe ese
-- correo, no puede iniciar sesión (error "email_not_confirmed").
--
-- SOLUCIÓN RECOMENDADA (una sola vez, en el Dashboard):
--   Authentication → Providers → Email → desactivar "Confirm email".
--   Así los usuarios nuevos quedan habilitados de inmediato.
--
-- Para los usuarios YA creados que quedaron sin confirmar, ejecutá esto en el
-- SQL Editor para confirmarlos manualmente:
-- ============================================================================

-- Confirmar TODOS los usuarios pendientes:
UPDATE auth.users
SET email_confirmed_at = COALESCE(email_confirmed_at, now())
WHERE email_confirmed_at IS NULL;

-- (Opcional) Confirmar uno específico por email:
-- UPDATE auth.users
-- SET email_confirmed_at = now()
-- WHERE email = 'persona@toppis.com' AND email_confirmed_at IS NULL;

-- Verificación:
SELECT email, email_confirmed_at
FROM auth.users
ORDER BY created_at DESC;
