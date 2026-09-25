CREATE TABLE stock_reservations
(
    id           BIGSERIAL PRIMARY KEY,
    order_number UUID        NOT NULL UNIQUE,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE stock_reservation_items
(
    reservation_id BIGINT      NOT NULL REFERENCES stock_reservations (id) ON DELETE CASCADE,
    sku_code       VARCHAR(64) NOT NULL,
    quantity       INTEGER     NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_stock_reservation_items_reservation_id ON stock_reservation_items (reservation_id);
