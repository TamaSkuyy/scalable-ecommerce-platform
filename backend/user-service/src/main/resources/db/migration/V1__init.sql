-- =====================================================================
-- user-service :: V1 - skema awal
-- Database: shopflow_user
--
-- Kenapa Flyway, bukan ddl-auto=update?
--   * Setiap perubahan skema punya file + versi yang bisa di-review di PR
--   * Riwayat perubahan (termasuk ALTER yang rumit) tercatat
--   * Aman dijalankan berulang di CI/CD
-- =====================================================================

CREATE TABLE users
(
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    roles         VARCHAR(120) NOT NULL DEFAULT 'CUSTOMER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

COMMENT ON COLUMN users.roles IS 'Daftar role dipisahkan koma, contoh: CUSTOMER,ADMIN';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt - TIDAK PERNAH password mentah';
