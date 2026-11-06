-- Dinheiro em centavos (INTEGER): o SQLite não tem DECIMAL, e REAL gera erro de arredondamento.
CREATE TABLE IF NOT EXISTS product (
    sku           TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    category      TEXT NOT NULL,
    price_cents   INTEGER NOT NULL CHECK (price_cents > 0),
    stock         INTEGER NOT NULL CHECK (stock >= 0),
    minimum_stock INTEGER NOT NULL CHECK (minimum_stock >= 0)
) STRICT;

-- Histórico: RESTRICT impede apagar um produto que já teve movimentação.
CREATE TABLE IF NOT EXISTS stock_movement (
    id          INTEGER PRIMARY KEY,
    sku         TEXT NOT NULL REFERENCES product (sku) ON DELETE RESTRICT,
    type        TEXT NOT NULL CHECK (type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT')),
    quantity    INTEGER NOT NULL CHECK (quantity >= 0),
    stock_after INTEGER NOT NULL CHECK (stock_after >= 0),
    reason      TEXT NOT NULL,
    occurred_at TEXT NOT NULL
) STRICT;

CREATE INDEX IF NOT EXISTS ix_stock_movement_sku ON stock_movement (sku);
