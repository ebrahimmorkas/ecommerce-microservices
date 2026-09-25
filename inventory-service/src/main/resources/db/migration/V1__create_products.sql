CREATE TABLE products
(
    id                 BIGSERIAL PRIMARY KEY,
    sku_code           VARCHAR(64)    NOT NULL UNIQUE,
    name               VARCHAR(255)   NOT NULL,
    description        VARCHAR(1000),
    price              NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    quantity_available INTEGER        NOT NULL CHECK (quantity_available >= 0),
    version            BIGINT         NOT NULL DEFAULT 0
);
