# Roadmap

Peta jalan dari "base yang bisa di-push ke GitHub" sampai "siap produksi".
Setiap tahap punya definisi selesai yang bisa diverifikasi, bukan sekadar daftar keinginan.

---

## Status sekarang

```
TAHAP 1  Setup & Containerization          ████████████████████ 100%  ✅
TAHAP 2  Backend Microservices             ████████████████████ 100%  ✅
TAHAP 3  Frontend Next.js                  ████████████████████ 100%  ✅
TAHAP 4  Logging, Monitoring & CI/CD       ██████████████░░░░░░  70%  🚧
TAHAP 5  Produksi & Skala                  ██░░░░░░░░░░░░░░░░░░  10%  ⬜
```

Yang sudah jalan di Tahap 1–3:

- 9 modul backend (8 service + 1 library bersama), **semua compile dan test lulus**
- Database per service + Flyway + data contoh 18 produk
- Komunikasi sinkron (Feign + resilience4j) dan asinkron (RabbitMQ + DLQ)
- Alur checkout end-to-end dengan saga dan kompensasi stok
- Frontend Next.js dengan BFF pattern, cookie httpOnly, SSR/ISR
- `docker compose up` menjalankan seluruh sistem

---

## TAHAP 1 — Setup & Containerization ✅

- [x] Struktur monorepo (`backend/`, `frontend/`, `infra/`, `docs/`)
- [x] Maven multi-module dengan parent POM & dependency management terpusat
- [x] Satu `Dockerfile` untuk semua service Spring Boot (build arg `MODULE`)
- [x] `Dockerfile` multi-stage untuk Next.js (`output: 'standalone'`)
- [x] `docker-compose.yml` lengkap: PostgreSQL, Redis, RabbitMQ, Prometheus, Grafana
- [x] `.env.example`, `.gitignore`, `.editorconfig`, `Makefile`
- [x] Maven Wrapper (`./mvnw`) — tidak perlu memasang Maven
- [x] Database per service dibuat otomatis lewat init script

**Selesai bila:** `make up` lalu `curl localhost:8080/actuator/health` → `UP`

---

## TAHAP 2 — Backend Microservices ✅

- [x] `discovery-service` — Eureka
- [x] `api-gateway` — routing Java DSL, verifikasi JWT, CORS, relay identitas
- [x] `user-service` — registrasi, login, JWT (HS256), profil, BCrypt cost 12
- [x] `product-service` — katalog, kategori, stok, cache Caffeine, optimistic + pessimistic locking
- [x] `cart-service` — Redis, Feign + circuit breaker
- [x] `order-service` — saga checkout, kompensasi stok, event publisher, DLQ
- [x] `payment-service` — konsumen event, gateway pembayaran tersimulasi, idempotensi
- [x] `notification-service` — konsumen 4 jenis event, idempotensi lewat `event_id`
- [x] `platform-common` — error seragam, correlation id, `CurrentUser`, kontrak event
- [x] Komunikasi sinkron (OpenFeign) **dan** asinkron (RabbitMQ)
- [x] Circuit breaker + timeout di semua klien Feign
- [x] Autentikasi internal (`X-Internal-Token`) untuk endpoint antar-service
- [x] 34 unit test, semua lulus

**Selesai bila:** alur register → lihat katalog → keranjang → checkout → `PAID` berjalan
dan tiap statusnya bisa kamu jelaskan.

---

## TAHAP 3 — Frontend Next.js ✅

- [x] App Router + TypeScript + Tailwind
- [x] SSR/ISR untuk katalog dan detail produk (SEO-friendly)
- [x] Client Components untuk keranjang, checkout, profil
- [x] BFF pattern: Route Handler `/api/auth/*` + cookie httpOnly
- [x] Middleware menyuntikkan `Authorization` dari cookie ke Gateway
- [x] Guard rute: `/cart`, `/checkout`, `/orders`, `/account`
- [x] Halaman: home, katalog, detail, keranjang, checkout, pesanan, akun, login, register
- [x] `loading.tsx`, `error.tsx`, `not-found.tsx`
- [x] Fallback ramah saat backend belum jalan
- [x] `Dockerfile` produksi (standalone, non-root)

**Selesai bila:** `pnpm build` sukses dan alur belanja bisa diselesaikan dari browser.

---

## TAHAP 4 — Logging, Monitoring & CI/CD 🚧 (sedang berjalan)

Sudah ada:
- [x] `X-Correlation-Id` disebarkan Gateway → Feign → RabbitMQ → MDC
- [x] Format log menyertakan nama service + correlation id
- [x] Spring Boot Actuator + Micrometer di semua service
- [x] Prometheus scraping 8 target + RabbitMQ
- [x] Grafana dengan datasource Prometheus ter-provisioning
- [x] GitHub Actions: build backend, test, lint + build frontend, smoke build Docker

Belum ada (urutan pengerjaan yang disarankan):

1. **Dashboard Grafana** — buat satu dashboard JSON dengan panel: RPS, error rate,
   p95 latency per service, status circuit breaker, kedalaman queue, koneksi Hikari.
   Simpan di `infra/grafana/provisioning/dashboards/shopflow.json`.
2. **Alert rules** — `infra/prometheus/alerts.yml`: error rate > 5% selama 5 menit,
   p95 > 1s, circuit breaker terbuka, `shopflow.events.dlq` tidak kosong.
3. **Distributed tracing** — tambahkan `micrometer-tracing-bridge-otel` + Tempo/Jaeger.
   Rasio manfaat/biaya paling tinggi setelah Prometheus.
4. **Logging terpusat** — Loki + Promtail, atau `logging.structured.format.console: ecs`.
5. **Test integration dengan Testcontainers** — profil `it` sudah disiapkan di
   parent POM; tambahkan test untuk `OrderService` memakai PostgreSQL + RabbitMQ nyata.
6. **Test kontrak** — Spring Cloud Contract agar perubahan kontrak antar-service
   tertangkap sebelum merge.
7. **Aggregate Swagger di Gateway** — satu UI untuk semua service.
8. **Build & push image ke GHCR** dengan tag commit SHA.

**Selesai bila:** kamu bisa menjawab "kenapa checkout lambat 5 menit lalu?" dalam
< 5 menit, memakai dashboard + trace, tanpa SSH ke server mana pun.

---

## TAHAP 5 — Produksi & Skala ⬜

### 5.1 Ketahanan data
- [ ] **Transactional Outbox** di order-service ([panduan](04-events.md#outbox))
- [ ] Job rekonsiliasi stok: deteksi stok tertahan yang tidak punya pesanan aktif
- [ ] Idempotency key untuk `POST /api/v1/orders` (mencegah double-submit)
- [ ] Uji kegagalan: matikan service satu per satu saat checkout, pastikan tidak ada data korup

### 5.2 Keamanan
- [ ] Rate limiting di `/auth/login` dan `/auth/register` (Redis rate limiter)
- [ ] Refresh token + rotasi, dan endpoint revoke
- [ ] RS256 + JWKS (hanya user-service bisa menerbitkan token)
- [ ] Secret dari Vault / AWS Secrets Manager
- [ ] TLS di ingress
- [ ] Audit log untuk aksi admin

### 5.3 Kubernetes
- [ ] Deployment + Service + ConfigMap per service (`infra/k8s/`)
- [ ] HPA berdasarkan CPU **dan** queue depth (KEDA)
- [ ] Liveness/readiness probe (aktifkan `management.endpoint.health.probes.enabled`)
- [ ] `terminationGracePeriodSeconds: 45` + `preStop` sleep
- [ ] NetworkPolicy: hanya Gateway yang boleh masuk ke service
- [ ] Ingress + cert-manager
- [ ] SealedSecrets atau External Secrets Operator

### 5.4 Operasional
- [ ] SLO terdokumentasi (mis. ketersediaan checkout 99.9%, p95 < 800ms)
- [ ] Alert berbasis SLO, bukan CPU
- [ ] Backup PostgreSQL terjadwal + **uji restore** (backup yang belum pernah
      di-restore bukan backup)
- [ ] Runbook: checkout gagal, DLQ menumpuk, database down, Redis down
- [ ] Load test dengan k6/Gatling, cari titik jenuh sebelum user menemukannya

### 5.5 Penskalaan
- [ ] Redis Sentinel/Cluster
- [ ] Read replica PostgreSQL untuk katalog
- [ ] Cache produk ke Redis (bukan Caffeine lokal) bila konsistensi antar-instance mulai penting
- [ ] Evaluasi Kafka bila butuh replay/retensi panjang

---

## Ide fitur (setelah fondasi kuat)

Jangan mengerjakan ini sebelum Tahap 4 selesai. Fondasi yang rapuh + banyak fitur
= sistem yang rapuh dengan banyak fitur.

| Prioritas | Fitur | Yang akan kamu pelajari |
|---|---|---|
| Tinggi | Kupon / diskon | Aturan harga, validasi, race condition kurs kupon |
| Tinggi | Manajemen pesanan untuk admin | Peran & izin, audit log |
| Tinggi | Ulasan produk | Agregasi rating, moderasi, cache |
| Sedang | Wishlist | Relasi user-produk, Redis vs PostgreSQL |
| Sedang | Melacak pengiriman | Event baru, integrasi pihak ketiga, webhook |
| Sedang | Upload gambar produk | Object storage (S3/MinIO), URL presigned |
| Sedang | Pencarian dengan Elasticsearch | Reverse index, sinkronisasi dari event |
| Sedang | Multi-currency | Pembulatan, kurs, snapshot harga |
| Rendah | Rekomendasi produk | Sinyal event, agregasi, machine learning |
| Rendah | Program loyalitas | Konsistensi saldo poin, transaksi |

---

## Prinsip yang dipertahankan sepanjang roadmap

1. **Setiap keputusan punya konsekuensi yang ditulis.** Kalau kamu menambahkan
   kompleksitas, tulis apa yang kamu bayar untuk itu.
2. **Uji jalur gagal, bukan hanya jalur sukses.** Jalur sukses sudah pernah kamu
   lihat; jalur gagal yang akan membangunkanmu jam 3 pagi.
3. **Ukur sebelum optimasi.** Tanpa angka, "optimasi" hanya menambah kode.
4. **Hapus sebelum menambah.** Kompleksitas yang tidak lagi dibutuhkan adalah utang.
5. **Kalau tidak bisa dijelaskan dalam 3 kalimat, jangan di-merge.** Termasuk kode
   yang kamu tulis sendiri tiga bulan lalu.
