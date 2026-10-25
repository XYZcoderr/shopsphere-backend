CREATE TABLE IF NOT EXISTS orders (
  id VARCHAR(36) PRIMARY KEY,
  customer_id VARCHAR(200) NOT NULL,
  sku VARCHAR(100) NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  status VARCHAR(40) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  idempotency_key VARCHAR(200)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_idempotency_key
ON orders(idempotency_key) WHERE idempotency_key IS NOT NULL;
