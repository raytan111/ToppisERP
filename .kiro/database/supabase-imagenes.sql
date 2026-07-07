-- ============================================================================
-- ToppisERP - Imágenes de productos y promociones
-- ============================================================================
-- 1) Columna imagen_url en items_menu y promociones (URL pública en Storage).
-- 2) Bucket público "menu" para las fotos.
-- Ejecutar en Supabase SQL Editor.
-- ============================================================================

ALTER TABLE items_menu   ADD COLUMN IF NOT EXISTS imagen_url TEXT;
ALTER TABLE promociones  ADD COLUMN IF NOT EXISTS imagen_url TEXT;

-- ── Bucket de Storage "menu" (público para lectura) ──────────────────────────
INSERT INTO storage.buckets (id, name, public)
VALUES ('menu', 'menu', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Políticas de Storage para el bucket "menu":
--   - lectura pública (para mostrar las imágenes en la app)
--   - escritura/borrado solo usuarios autenticados (staff)
DROP POLICY IF EXISTS "menu lectura publica" ON storage.objects;
CREATE POLICY "menu lectura publica"
ON storage.objects FOR SELECT
USING (bucket_id = 'menu');

DROP POLICY IF EXISTS "menu escritura autenticados" ON storage.objects;
CREATE POLICY "menu escritura autenticados"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'menu');

DROP POLICY IF EXISTS "menu update autenticados" ON storage.objects;
CREATE POLICY "menu update autenticados"
ON storage.objects FOR UPDATE TO authenticated
USING (bucket_id = 'menu');

DROP POLICY IF EXISTS "menu delete autenticados" ON storage.objects;
CREATE POLICY "menu delete autenticados"
ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'menu');
