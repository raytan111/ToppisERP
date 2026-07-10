-- ════════════════════════════════════════════════════════════════════════
-- ToppisERP — Rediseño del POS (Fase A: modelo de datos)
-- EJECUTAR MANUALMENTE en el SQL Editor de Supabase.
-- CLP; montos con IVA incluido. Idempotente (IF NOT EXISTS / CREATE OR REPLACE).
-- ════════════════════════════════════════════════════════════════════════

-- ── 1) Clientes y cuponera ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS clientes (
    id                 SERIAL PRIMARY KEY,
    telefono3          TEXT NOT NULL,                 -- 3 últimos dígitos del WhatsApp
    nombre             TEXT,                          -- opcional (se completa después)
    sellos_hamburguesa INT NOT NULL DEFAULT 0,        -- avance de la cuponera
    local_id           INT REFERENCES locales(id),
    created_at         TIMESTAMPTZ DEFAULT now(),
    updated_at         TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_clientes_telefono3 ON clientes(telefono3);

-- ── 2) Pedidos (carritos) ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pedidos (
    id            SERIAL PRIMARY KEY,
    cliente_id    INT REFERENCES clientes(id),
    estado        TEXT NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','CERRADO')),
    pagado        BOOLEAN NOT NULL DEFAULT FALSE,
    entregado     BOOLEAN NOT NULL DEFAULT FALSE,
    metodo_pago   TEXT,                               -- al pagar
    sobre_id      INT REFERENCES sobres(id),          -- al pagar
    venta_id      INT REFERENCES ventas(id),          -- venta materializada al pagar
    zona_envio    TEXT NOT NULL DEFAULT 'SIN_ENVIO',
    monto_envio   NUMERIC NOT NULL DEFAULT 0,
    total         NUMERIC NOT NULL DEFAULT 0,
    comanda_texto TEXT,                               -- se llena al cerrar
    local_id      INT REFERENCES locales(id),
    created_by    UUID,
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now(),
    closed_at     TIMESTAMPTZ,
    paid_at       TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_pedidos_cliente ON pedidos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_estado  ON pedidos(estado);
CREATE INDEX IF NOT EXISTS idx_pedidos_local   ON pedidos(local_id);

-- ── 3) Líneas de cobro del pedido ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pedido_items (
    id              SERIAL PRIMARY KEY,
    pedido_id       INT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    tipo            TEXT NOT NULL CHECK (tipo IN ('PRODUCTO','PROMO')),
    item_menu_id    INT REFERENCES items_menu(id),    -- si PRODUCTO
    promocion_id    INT REFERENCES promociones(id),   -- si PROMO
    cantidad        INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unitario NUMERIC NOT NULL DEFAULT 0,
    subtotal        NUMERIC NOT NULL DEFAULT 0,
    es_regalo       BOOLEAN NOT NULL DEFAULT FALSE,   -- cupón: no se cobra
    created_at      TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_pedido_items_pedido ON pedido_items(pedido_id);

-- ── 4) Unidades a preparar (comanda + descuento de stock) ───────────────────
CREATE TABLE IF NOT EXISTS pedido_unidades (
    id             SERIAL PRIMARY KEY,
    pedido_item_id INT NOT NULL REFERENCES pedido_items(id) ON DELETE CASCADE,
    item_menu_id   INT NOT NULL REFERENCES items_menu(id),
    comentario     TEXT,                              -- "sin tomate" (no descuenta stock)
    created_at     TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_pedido_unidades_item ON pedido_unidades(pedido_item_id);

-- ── 5) Modificadores (agregados) de una unidad ──────────────────────────────
CREATE TABLE IF NOT EXISTS pedido_unidad_mods (
    id               SERIAL PRIMARY KEY,
    pedido_unidad_id INT NOT NULL REFERENCES pedido_unidades(id) ON DELETE CASCADE,
    modificador_id   INT NOT NULL REFERENCES modificadores(id),
    created_at       TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_pedido_unidad_mods_unidad ON pedido_unidad_mods(pedido_unidad_id);

-- ── 6) Promociones configurables (espacios) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS promocion_espacios (
    id           SERIAL PRIMARY KEY,
    promocion_id INT NOT NULL REFERENCES promociones(id) ON DELETE CASCADE,
    nombre       TEXT NOT NULL,                       -- ej "Hamburguesa", "Bebida"
    cantidad     INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    modo         TEXT NOT NULL CHECK (modo IN ('LISTA','CATEGORIA')),
    categoria    TEXT,                                -- si modo = CATEGORIA
    orden        INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_promo_espacios_promo ON promocion_espacios(promocion_id);

CREATE TABLE IF NOT EXISTS promocion_espacio_opciones (
    id           SERIAL PRIMARY KEY,
    espacio_id   INT NOT NULL REFERENCES promocion_espacios(id) ON DELETE CASCADE,
    item_menu_id INT NOT NULL REFERENCES items_menu(id),
    created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_promo_esp_opciones_espacio ON promocion_espacio_opciones(espacio_id);

-- ── 7) Modificadores por categoría del menú ─────────────────────────────────
ALTER TABLE modificadores ADD COLUMN IF NOT EXISTS categoria TEXT;

-- ── 8) Categorías de bebida en el inventario ────────────────────────────────
ALTER TABLE articulos DROP CONSTRAINT IF EXISTS chk_articulo_categoria;
ALTER TABLE articulos ADD CONSTRAINT chk_articulo_categoria
    CHECK (categoria IN ('INGREDIENTES','PACKAGING','INSUMOS','BEBIDA_LATA','BEBIDA_MEDIANA'));

-- ── 9) RLS (usuarios autenticados; app single-org) ──────────────────────────
ALTER TABLE clientes                   ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedidos                    ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedido_items               ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedido_unidades            ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedido_unidad_mods         ENABLE ROW LEVEL SECURITY;
ALTER TABLE promocion_espacios         ENABLE ROW LEVEL SECURITY;
ALTER TABLE promocion_espacio_opciones ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE t TEXT;
BEGIN
    FOR t IN SELECT unnest(ARRAY['clientes','pedidos','pedido_items','pedido_unidades',
                                 'pedido_unidad_mods','promocion_espacios','promocion_espacio_opciones'])
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS "auth all %1$s" ON %1$s;', t);
        EXECUTE format(
          'CREATE POLICY "auth all %1$s" ON %1$s FOR ALL USING (auth.role() = ''authenticated'') WITH CHECK (auth.role() = ''authenticated'');', t);
    END LOOP;
END $$;

-- ── 10) Realtime (carritos y su detalle) ────────────────────────────────────
DO $$
BEGIN
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE pedidos;         EXCEPTION WHEN OTHERS THEN NULL; END;
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE pedido_items;    EXCEPTION WHEN OTHERS THEN NULL; END;
    BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE pedido_unidades; EXCEPTION WHEN OTHERS THEN NULL; END;
END $$;
