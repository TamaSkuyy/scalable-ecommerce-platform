-- =====================================================================
-- product-service :: V1 - skema katalog
-- Database: shopflow_product
-- =====================================================================

CREATE TABLE categories
(
    id            BIGSERIAL    PRIMARY KEY,
    slug          VARCHAR(64)  NOT NULL,
    name          VARCHAR(120) NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE products
(
    id             BIGSERIAL    PRIMARY KEY,
    sku            VARCHAR(64)  NOT NULL,
    slug           VARCHAR(160) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    price_cents    BIGINT       NOT NULL,
    currency       VARCHAR(3)   NOT NULL DEFAULT 'IDR',
    category_id    BIGINT       NOT NULL,
    image_url      VARCHAR(500),
    stock_quantity INTEGER      NOT NULL DEFAULT 0,
    version        BIGINT       NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),

    -- ------------------------------------------------------------------
    -- Jaring pengaman TERAKHIR anti-oversell.
    -- Aplikasi sudah memvalidasi stok, tapi bug/race condition bisa lolos.
    -- Constraint ini membuat database menolak stok negatif, apa pun yang terjadi.
    -- Prinsip: "validasi di aplikasi untuk pesan error yang ramah, constraint di
    -- database untuk integritas data".
    -- ------------------------------------------------------------------
    CONSTRAINT ck_products_stock_non_negative CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_price_positive CHECK (price_cents > 0)
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_active_created ON products (active, created_at DESC);

COMMENT ON COLUMN products.price_cents IS 'Harga dalam satuan minor (sen). 150000 = Rp 1.500,00';
COMMENT ON COLUMN products.version IS 'Optimistic locking - dikelola Hibernate (@Version)';
COMMENT ON COLUMN products.active IS 'Soft delete: produk lama tidak boleh dihapus keras';
