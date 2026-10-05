-- =====================================================================
-- notification-service :: V1 - riwayat notifikasi
-- Database: shopflow_notification
-- =====================================================================

CREATE TABLE notifications
(
    id           BIGSERIAL     PRIMARY KEY,
    event_id     VARCHAR(64)   NOT NULL,
    user_id      BIGINT        NOT NULL,
    type         VARCHAR(32)   NOT NULL,
    subject      VARCHAR(200)  NOT NULL,
    body         VARCHAR(2000) NOT NULL,
    order_number VARCHAR(32),
    channel      VARCHAR(16)   NOT NULL DEFAULT 'EMAIL',
    is_read      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- Idempotensi: satu event = satu notifikasi, walau pesan dikirim berkali-kali.
    CONSTRAINT uk_notifications_event_id UNIQUE (event_id)
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id, is_read) WHERE is_read = FALSE;

COMMENT ON COLUMN notifications.type IS 'ORDER_CREATED | ORDER_CANCELLED | PAYMENT_COMPLETED | PAYMENT_FAILED';
COMMENT ON COLUMN notifications.channel IS 'EMAIL | SMS | PUSH (saat ini hanya EMAIL yang disimulasikan)';
