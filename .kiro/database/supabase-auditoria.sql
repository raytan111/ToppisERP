-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — AUDITORÍA de la base de datos (SOLO LECTURA)
-- ════════════════════════════════════════════════════════════════════════
-- No modifica nada: son puros SELECT sobre el catálogo de Postgres.
-- Corré cada BLOQUE por separado en el SQL Editor de Supabase
-- (seleccioná el bloque y Cmd/Ctrl+Enter) y copiame el resultado.
-- El editor muestra solo el resultado del último SELECT, por eso van separados.
-- ════════════════════════════════════════════════════════════════════════


-- ── BLOQUE 1 · FUNCIONES DUPLICADAS (overloads) ─────────────────────────────
-- El riesgo #1: misma función con varias firmas. Si aparece algo acá,
-- PostgREST puede fallar con "could not choose best candidate function".
SELECT p.proname                                              AS funcion,
       count(*)                                               AS versiones,
       string_agg(pg_get_function_identity_arguments(p.oid),
                  E'\n   ---> ' ORDER BY p.oid)                AS firmas
FROM   pg_proc p
JOIN   pg_namespace n ON n.oid = p.pronamespace
WHERE  n.nspname = 'public'
GROUP  BY p.proname
HAVING count(*) > 1
ORDER  BY p.proname;


-- ── BLOQUE 2 · INVENTARIO DE TABLAS (filas, RLS, políticas, índices) ─────────
-- Para ver qué tablas hay, cuáles están vacías (posible legacy) y si tienen RLS.
SELECT c.relname                                              AS tabla,
       c.reltuples::bigint                                    AS filas_aprox,
       c.relrowsecurity                                       AS rls_activo,
       (SELECT count(*) FROM pg_policies pol
         WHERE pol.schemaname = 'public'
           AND pol.tablename = c.relname)                     AS politicas,
       (SELECT count(*) FROM pg_index i
         WHERE i.indrelid = c.oid)                            AS indices
FROM   pg_class c
JOIN   pg_namespace n ON n.oid = c.relnamespace
WHERE  n.nspname = 'public'
  AND  c.relkind = 'r'
ORDER  BY c.relname;


-- ── BLOQUE 3 · TABLAS SIN RLS o SIN POLÍTICAS ───────────────────────────────
-- RLS desactivado o sin políticas = agujero de seguridad (o tabla olvidada).
SELECT c.relname                                              AS tabla,
       c.relrowsecurity                                       AS rls_activo,
       (SELECT count(*) FROM pg_policies pol
         WHERE pol.schemaname = 'public'
           AND pol.tablename = c.relname)                     AS politicas,
       CASE
         WHEN NOT c.relrowsecurity THEN 'RLS DESACTIVADO'
         WHEN NOT EXISTS (SELECT 1 FROM pg_policies pol
                           WHERE pol.schemaname='public'
                             AND pol.tablename=c.relname) THEN 'RLS SIN POLITICAS'
       END                                                    AS problema
FROM   pg_class c
JOIN   pg_namespace n ON n.oid = c.relnamespace
WHERE  n.nspname = 'public'
  AND  c.relkind = 'r'
  AND (NOT c.relrowsecurity
       OR NOT EXISTS (SELECT 1 FROM pg_policies pol
                       WHERE pol.schemaname='public'
                         AND pol.tablename=c.relname))
ORDER  BY c.relname;


-- ── BLOQUE 4 · CLAVES FORÁNEAS SIN ÍNDICE ───────────────────────────────────
-- Una FK sin índice hace lentos los JOIN y los borrados en cascada.
SELECT conrelid::regclass::text                               AS tabla,
       con.conname                                            AS fk,
       a.attname                                              AS columna
FROM   pg_constraint con
JOIN   pg_attribute a
       ON a.attrelid = con.conrelid
      AND a.attnum = ANY (con.conkey)
WHERE  con.contype = 'f'
  AND  con.connamespace = 'public'::regnamespace
  AND  array_length(con.conkey, 1) = 1          -- FKs de una sola columna
  AND  NOT EXISTS (
         SELECT 1 FROM pg_index i
         WHERE i.indrelid = con.conrelid
           AND a.attnum = i.indkey[0]
       )
ORDER  BY tabla, columna;


-- ── BLOQUE 5 · INVENTARIO DE FUNCIONES (para revisar a ojo) ─────────────────
SELECT p.proname                                              AS funcion,
       pg_get_function_identity_arguments(p.oid)              AS argumentos,
       pg_get_function_result(p.oid)                          AS retorna
FROM   pg_proc p
JOIN   pg_namespace n ON n.oid = p.pronamespace
WHERE  n.nspname = 'public'
ORDER  BY p.proname, p.oid;


-- ── BLOQUE 6 · TABLAS LEGACY DEL MODELO VIEJO (deberían dar 0 filas) ────────
-- Si alguna de estas EXISTE, quedó de antes de la Fase 4 y se puede borrar.
SELECT c.relname AS tabla_legacy_presente
FROM   pg_class c
JOIN   pg_namespace n ON n.oid = c.relnamespace
WHERE  n.nspname = 'public'
  AND  c.relkind = 'r'
  AND  c.relname IN ('ingredientes','insumos','salsas');
