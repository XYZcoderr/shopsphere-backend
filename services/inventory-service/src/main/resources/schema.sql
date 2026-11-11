CREATE TABLE IF NOT EXISTS inventory_stock (
  sku VARCHAR(100) PRIMARY KEY,
  available INTEGER NOT NULL CHECK (available >= 0)
);
CREATE TABLE IF NOT EXISTS inventory_reservations (
  order_id VARCHAR(36) PRIMARY KEY,
  sku VARCHAR(100) NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(50)
);
INSERT INTO inventory_stock(sku, available) VALUES ('BK-001', 25) ON CONFLICT (sku) DO NOTHING;
INSERT INTO inventory_stock(sku, available) VALUES ('BK-002', 10) ON CONFLICT (sku) DO NOTHING;
