-- =====================================================================
-- order-service :: V1 - skema pesanan
-- Database: shopflow_order
-- =====================================================================

CREATE TABLE orders
(
    id                  BIGSERIAL    PRIMARY KEY,
    order_number        VARCHAR(32)  NOT NULL,
    user_id             BIGINT       NOT NULL,
    user_email          VARCHAR(180),
    status              VARCHAR(32)  NOT NULL,
    total_cents         BIGINT       NOT NULL,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'IDR',

    -- Alamat di-embed (@Embeddable) ke tabel ini, bukan tabel terpisah:
    -- alamat selalu milik satu pesanan dan tidak pernah di-query lintas pesanan.
    recipient_name      VARCHAR(120) NOT NULL,
    phone               VARCHAR(32)  NOT NULL,
    line1               VARCHAR(200) NOT NULL,
    line2               VARCHAR(200),
    city                VARCHAR(120) NOT NULL,
    postal_code         VARCHAR(16)  NOT NULL,
    country             VARCHAR(64)  NOT NULL,

    notes               VARCHAR(500),
    payment_reference   VARCHAR(64),
    cancellation_reason VARCHAR(200),

    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT ck_orders_total_non_negative CHECK (total_cents >= 0)
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items
(
    id               BIGSERIAL    PRIMARY KEY,
    order_id         BIGINT       NOT NULL,
    product_id       BIGINT       NOT NULL,
    sku              VARCHAR(64)  NOT NULL,
    name             VARCHAR(200) NOT NULL,
    image_url        VARCHAR(500),
    unit_price_cents BIGINT       NOT NULL,
    quantity         INTEGER      NOT NULL,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price_non_negative CHECK (unit_price_cents >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

COMMENT ON COLUMN orders.status IS 'PENDING_PAYMENT | PAID | PAYMENT_FAILED | CANCELLED | FULFILLED';
COMMENT ON TABLE order_items IS 'Snapshot harga & nama produk pada saat dibeli';
