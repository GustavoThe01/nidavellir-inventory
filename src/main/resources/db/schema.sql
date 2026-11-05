-- Dinheiro em centavos (INTEGER): o SQLite não tem DECIMAL, e REAL gera erro de arredondamento.
CREATE TABLE IF NOT EXISTS product (
    sku           TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    category      TEXT NOT NULL,
    price_cents   INTEGER NOT NULL CHECK (price_cents > 0),
    stock         INTEGER NOT NULL CHECK (stock >= 0),
    minimum_stock INTEGER NOT NULL CHECK (minimum_stock >= 0)
) STRICT;
