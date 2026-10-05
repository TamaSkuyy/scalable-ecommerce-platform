-- =====================================================================
-- Inisialisasi PostgreSQL untuk development lokal.
--
-- Prinsip "database per service" TETAP dipegang: setiap service punya
-- database sendiri dan tidak boleh mengakses tabel service lain.
--
-- Untuk mempermudah lokal, satu instance PostgreSQL menampung kelima
-- database. Di produksi, pisahkan instance-nya (atau setidaknya cluster
-- + kredensial per service) supaya kegagalan satu service tidak
-- menjatuhkan semuanya.
--
-- File ini dijalankan otomatis oleh image postgres saat volume masih kosong.
-- =====================================================================

\set ON_ERROR_STOP on

SELECT 'CREATE DATABASE shopflow_user OWNER shopflow'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopflow_user')\gexec

SELECT 'CREATE DATABASE shopflow_product OWNER shopflow'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopflow_product')\gexec

SELECT 'CREATE DATABASE shopflow_order OWNER shopflow'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopflow_order')\gexec

SELECT 'CREATE DATABASE shopflow_payment OWNER shopflow'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopflow_payment')\gexec

SELECT 'CREATE DATABASE shopflow_notification OWNER shopflow'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopflow_notification')\gexec

-- Ekstensi berguna (opsional, aman kalau belum dipakai):
--   pg_trgm  -> pencarian LIKE '%kata%' jadi cepat
--   unaccent -> pencarian tanpa peduli tanda diakritik
\connect shopflow_product
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

\echo 'Semua database ShopFlow siap.'
