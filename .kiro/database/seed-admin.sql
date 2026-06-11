-- ============================================================================
-- ToppisERP - Crear perfil del Administrador inicial
-- ============================================================================
-- IMPORTANTE: Ejecutar DESPUÉS de crear el usuario en Authentication.
-- El UUID debe coincidir con el User UID de Supabase Auth.
-- ============================================================================

INSERT INTO usuarios (id, nombre, email, rol, activo)
VALUES (
    '89c06255-0538-4259-9151-f9ebe5eae92b',  -- User UID de Supabase Auth
    'Administrador',
    'admin@toppis.com',
    'ADMIN',
    true
);

-- Verificar que se creó correctamente
SELECT id, nombre, email, rol, activo, created_at
FROM usuarios
WHERE email = 'admin@toppis.com';
