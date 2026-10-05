-- =====================================================================
-- payment-service :: V1 - catatan pembayaran
-- Database: shopflow_payment
--
-- Perhatikan dua UNIQUE constraint di bawah. Keduanya adalah pengaman
-- idempotensi: pesan RabbitMQ boleh datang dua kali, tapi baris pembayaran
-- untuk satu pesanan hanya boleh ada satu.
-- =====================================================================

CREATE TABLE payments
(
    id                 BIGSERIAL   PRIMARY KEY,
    payment_id         VARCHAR(40) NOT NULL,
    order_number       VARCHAR(32) NOT NULL,
    user_id            BIGINT,
    amount_cents       BIGINT      NOT NULL,
    currency           VARCHAR(3)  NOT NULL DEFAULT 'IDR',
    method             VARCHAR(32) NOT NULL DEFAULT 'SIMULATED_CARD',
    status             VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(64),
    failure_reason     VARCHAR(200),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_payments_order_number UNIQUE (order_number),
    CONSTRAINT uk_payments_payment_id UNIQUE (payment_id),
    CONSTRAINT ck_payments_amount_positive CHECK (amount_cents >= 0)
);

CREATE INDEX idx_payments_user_created ON payments (user_id, created_at DESC);
CREATE INDEX idx_payments_status ON payments (status);

COMMENT ON COLUMN payments.status IS 'PENDING | SUCCEEDED | FAILED | REFUNDED';
COMMENT ON COLUMN payments.provider_reference IS 'ID transaksi dari gateway (Stripe: pi_..., charge: ch_...)';
