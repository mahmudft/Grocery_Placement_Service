CREATE TABLE product (
    sku         VARCHAR(64)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    price_cents BIGINT       NOT NULL CHECK (price_cents >= 0),
    stock       INT          NOT NULL CHECK (stock >= 0)          -- last line of defence against overselling
);

CREATE TABLE delivery_slot (
    id        BIGSERIAL   PRIMARY KEY,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at   TIMESTAMPTZ NOT NULL,
    capacity  INT         NOT NULL CHECK (capacity >= 0),
    booked    INT         NOT NULL DEFAULT 0,
    CONSTRAINT slot_not_overbooked CHECK (booked <= capacity)      -- last line of defence against overbooking
);
CREATE INDEX idx_delivery_slot_starts_at ON delivery_slot (starts_at);

CREATE TABLE customer_order (
    id              UUID         PRIMARY KEY,
    customer_id     VARCHAR(64)  NOT NULL,
    slot_id         BIGINT       NOT NULL REFERENCES delivery_slot (id),
    status          VARCHAR(32)  NOT NULL,
    total_cents     BIGINT       NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE order_line (
    id               BIGSERIAL   PRIMARY KEY,
    order_id         UUID        NOT NULL REFERENCES customer_order (id),
    sku              VARCHAR(64) NOT NULL REFERENCES product (sku),
    quantity         INT         NOT NULL CHECK (quantity > 0),
    unit_price_cents BIGINT      NOT NULL
);
CREATE INDEX idx_order_line_order_id ON order_line (order_id);

CREATE TABLE outbox_event (
    id           UUID        PRIMARY KEY,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type   VARCHAR(64) NOT NULL,
    payload      TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
