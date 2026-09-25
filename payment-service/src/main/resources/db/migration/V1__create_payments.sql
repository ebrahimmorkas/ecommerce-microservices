CREATE TABLE payments
(
    id                 UUID PRIMARY KEY,
    order_number       UUID           NOT NULL UNIQUE,
    amount             NUMERIC(12, 2) NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    provider_reference VARCHAR(100),
    failure_reason     VARCHAR(500),
    created_at         TIMESTAMPTZ    NOT NULL
);
