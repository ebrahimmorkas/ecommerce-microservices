CREATE TABLE orders
(
    id             BIGSERIAL PRIMARY KEY,
    order_number   UUID           NOT NULL UNIQUE,
    customer_email VARCHAR(255)   NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    total_amount   NUMERIC(12, 2) NOT NULL,
    failure_reason VARCHAR(500),
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_customer_email ON orders (lower(customer_email));

CREATE TABLE order_line_items
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku_code   VARCHAR(64)    NOT NULL,
    quantity   INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_order_line_items_order_id ON order_line_items (order_id);
