<div align="center">

# 🛒 ShopFlow

**Platform e-commerce berskala besar berbasis Microservices — Java Spring Boot + Next.js**

Kerangka produksi-siap untuk belajar membangun sistem terdistribusi sungguhan:
service discovery, API gateway, database per service, event-driven messaging,
resilience, observability, dan CI/CD.

[![CI](https://github.com/USERNAME/scalable-ecommerce-platform/actions/workflows/ci.yml/badge.svg)](.github/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0-blue?logo=spring)
![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4-FF6600?logo=rabbitmq)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

</div>

---

## Apa ini?

Sebuah **kerangka kerja (base project)** e-commerce lengkap dengan 8 microservices yang
saling terhubung, alur checkout end-to-end yang benar-benar berjalan, dan dokumentasi
yang menjelaskan **alasan** di balik setiap keputusan — bukan hanya kodenya.

Dibuat untuk siapa pun yang ingin naik dari "belajar Spring Boot" menjadi
"bisa merancang sistem terdistribusi". Setiap pola penting (saga, idempotensi,
circuit breaker, outbox, BFF) diimplementasikan di kode yang bisa dibaca,
bukan hanya dijelaskan di teori.

> **Filosofi repo ini:** kode yang tidak kamu pahami lebih berbahaya daripada kode yang
> tidak ada. Karena itu hampir setiap kelas punya komentar yang menjelaskan
> *"kenapa begitu"*, bukan *"apa yang dilakukan baris ini"*.

---

## Arsitektur

```
                          ┌───────────────────────┐
                          │   Next.js (App Router)│
                          │   SSR / ISR / Client  │
                          └───────────┬───────────┘
                                      │  JWT di cookie httpOnly (BFF)
                                      ▼
                     ┌────────────────────────────────────┐
                     │   API GATEWAY   :8080              │
                     │   routing • verifikasi JWT • CORS  │
                     │   relay identitas ke service hilir │
                     └──┬────────┬────────┬────────┬──────┘
                        ▼        ▼        ▼        ▼
                  ┌──────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐  ┌──────────┐
                  │  user    │ │ product  │ │  cart  │ │  order   │  │ payment  │
                  │  :8081   │ │  :8082   │ │ :8083  │ │  :8084   │  │  :8085   │
                  │ Postgres │ │ Postgres │ │ Redis  │ │ Postgres │  │ Postgres │
                  └──────────┘ └──────────┘ └────────┘ └────┬─────┘  └────┬─────┘
                                                            │             │
                            semua mendaftar ke Eureka        │  RabbitMQ   │
                            ┌──────────────┐                └──────┬──────┘
                            │ discovery    │                       │
                            │   :8761      │              ┌────────▼────────┐
                            └──────────────┘              │ notification    │
                                                          │    :8086        │
                                                          └─────────────────┘

                   Prometheus :9090  ──►  Grafana :3001
```

Diagram lebih lengkap + alasan setiap keputusan: **[docs/02-architecture.md](docs/02-architecture.md)**

---

## Mulai cepat

**Prasyarat:** JDK 21 · Docker + Compose · Node 20+ · pnpm 9+

```bash
git clone https://github.com/USERNAME/scalable-ecommerce-platform.git
cd scalable-ecommerce-platform

cp .env.example .env      # opsional
make up                   # build + jalankan 8 service backend + infrastruktur
```

Build pertama 10–20 menit (unduh dependency + image). Selanjutnya jauh lebih cepat.

```bash
# Pastikan semuanya hidup
curl http://localhost:8080/actuator/health        # {"status":"UP"}

# Coba katalog (endpoint publik)
curl "http://localhost:8080/api/v1/products?size=3"
```

Lalu jalankan frontend:

```bash
cd frontend/web
pnpm install && pnpm dev      # http://localhost:3000
```

**Login sebagai admin:** `admin@shopflow.dev` / `admin12345`

Buat akun sendiri lewat halaman **Daftar** untuk mencoba alur belanja lengkap.

> Panduan langkah demi langkah + troubleshooting: **[docs/01-getting-started.md](docs/01-getting-started.md)**

---

## Titik akses

| URL | Apa |
|---|---|
| http://localhost:3000 | Frontend Next.js |
| http://localhost:8080 | **API Gateway** — satu-satunya pintu masuk API |
| http://localhost:8761 | Eureka dashboard (lihat service yang terdaftar) |
| http://localhost:15672 | RabbitMQ Management UI (`shopflow` / `shopflow`) |
| http://localhost:9090 | Prometheus |
| http://localhost:3001 | Grafana (`admin` / `admin`) |

---

## Yang sudah diimplementasikan

### Backend — 8 microservices + 1 library bersama

| Service | Port | Peran | Sorotan teknis |
|---|---|---|---|
| `discovery-service` | 8761 | Service registry | Eureka server, client-side load balancing |
| `api-gateway` | 8080 | API Gateway | Routing Java DSL, verifikasi JWT sekali, relay identitas, CORS |
| `user-service` | 8081 | Identitas & auth | JWT HS256, BCrypt cost 12, pesan error anti user-enumeration |
| `product-service` | 8082 | Katalog & stok | Cache Caffeine, optimistic + pessimistic locking, `CHECK` anti-oversell |
| `cart-service` | 8083 | Keranjang | Redis + TTL, Feign + circuit breaker |
| `order-service` | 8084 | Checkout & pesanan | **Saga orchestration**, kompensasi stok, `AFTER_COMMIT` event |
| `payment-service` | 8085 | Pembayaran | Konsumen event, idempotensi, gateway tersimulasi (plug-in Stripe/PayPal) |
| `notification-service` | 8086 | Email/SMS | Idempoten lewat `event_id`, template Bahasa Indonesia |

**Pola & praktik yang dipakai:**

- ✅ **Database per service** — isolasi data tegas, tidak ada query lintas domain
- ✅ **Flyway** untuk migrasi + `ddl-auto: validate` (gagal cepat kalau skema tidak sinkron)
- ✅ **Error JSON seragam** di seluruh service, otomatis lewat `platform-common` auto-configuration
- ✅ **Correlation ID** menyebar Gateway → Feign → RabbitMQ → MDC SLF4J
- ✅ **Idempotensi** tiga lapis: cek aplikasi + UNIQUE constraint + transisi status yang aman
- ✅ **Circuit breaker + timeout** (Resilience4j) di semua panggilan antar-service
- ✅ **Dead-letter queue** — pesan gagal tidak hilang, tidak berputar selamanya
- ✅ **Kompensasi saga** — stok tidak "bocor" saat checkout gagal di tengah jalan
- ✅ **Autentikasi internal** antar-service (`X-Internal-Token`, constant-time compare)
- ✅ **Graceful shutdown**, health check, metrik Prometheus di semua service
- ✅ **34 unit test** tanpa database — cepat, tidak flaky

### Frontend — Next.js

- ✅ App Router + TypeScript + Tailwind CSS
- ✅ **SSR/ISR** untuk katalog & detail produk (SEO-friendly)
- ✅ **BFF pattern** — JWT di cookie **httpOnly**, tidak pernah menyentuh JavaScript
- ✅ **Middleware** yang menyuntikkan `Authorization` dari cookie, plus guard rute
- ✅ Client Components untuk keranjang, checkout, dan profil
- ✅ `loading.tsx` / `error.tsx` / `not-found.tsx` + fallback ramah saat backend mati
- ✅ Nyaman dipakai: keranjang, checkout, riwayat pesanan, notifikasi, akun

### Infrastruktur

- ✅ **Satu `Dockerfile`** untuk 9 service backend (build arg `MODULE`) — DRY
- ✅ Dockerfile multi-stage untuk Next.js (`output: 'standalone'`, non-root)
- ✅ `docker-compose.yml` dengan healthcheck & urutan start yang benar
- ✅ Prometheus + Grafana ter-provisioning
- ✅ **GitHub Actions CI** — build & test backend, lint & build frontend, smoke build Docker
- ✅ `Makefile` untuk perintah sehari-hari

---

## Struktur proyek

```
scalable-ecommerce-platform/
├── backend/                     # Maven multi-module
│   ├── pom.xml                  # parent: versi & dependency management
│   ├── mvnw / mvnw.cmd          # Maven Wrapper (tidak perlu install Maven)
│   ├── Dockerfile               # satu Dockerfile untuk semua service
│   ├── platform-common/         # library bersama (error, security, events, feign)
│   ├── discovery-service/
│   ├── api-gateway/
│   ├── user-service/
│   ├── product-service/
│   ├── cart-service/
│   ├── order-service/
│   ├── payment-service/
│   └── notification-service/
├── frontend/web/                # Next.js (App Router, BFF, httpOnly cookie)
├── infra/                       # postgres init, prometheus, grafana, rabbitmq, k8s
├── docs/                        # 7 dokumen: arsitektur, API, event, jalur belajar, ...
├── docker-compose.yml
├── Makefile
└── .env.example
```

---

## Dokumentasi

| Dokumen | Isi |
|---|---|
| **[01 · Mulai dari Nol](docs/01-getting-started.md)** | Menjalankan, verifikasi, troubleshooting |
| **[02 · Arsitektur](docs/02-architecture.md)** | Kenapa disusun begini, trade-off, keamanan |
| **[03 · Kontrak API](docs/03-api-contract.md)** | Semua endpoint + contoh `curl` siap pakai |
| **[04 · Event-Driven](docs/04-events.md)** | RabbitMQ, idempotensi, DLQ, **Transactional Outbox** |
| **[05 · Jalur Belajar](docs/05-learning-path.md)** | Kurikulum 5 level + 30+ latihan terverifikasi |
| **[06 · Skala & Produksi](docs/06-scalability-and-production.md)** | Bottleneck, observability, K8s, checklist |
| **[07 · Roadmap](docs/07-roadmap.md)** | Status per tahap & langkah selanjutnya |

---

## Perintah berguna

```bash
make                      # daftar semua perintah
make up                   # jalankan seluruh sistem
make infra                # hanya postgres + redis + rabbitmq (untuk ngoding di host)
make dev-product-service  # jalankan SATU service dengan hot reload
make logs                 # ikuti log semua service
make test                 # unit test backend
make verify               # build + test lengkap
make fe-dev               # Next.js dev server
make reset-data           # hapus semua volume (mulai dari nol)
```

---

## Uji jalur kegagalan

Fondasi yang baik dibuktikan oleh perilakunya saat ada yang rusak.
Coba skenario ini satu per satu:

```bash
# 1. Circuit breaker: matikan katalog, lihat bagaimana cart-service merespons
docker compose stop product-service
curl -s localhost:8080/api/v1/cart -H "Authorization: Bearer $TOKEN"
# -> 503 dengan pesan yang ramah, bukan timeout 30 detik

# 2. Jalur pembayaran gagal: paksa payment selalu ditolak
SHOPFLOW_PAYMENT_SUCCESS_RATE=0.0 docker compose up -d payment-service
# checkout -> pesanan PAYMENT_FAILED + stok dilepas kembali

# 3. Idempotensi: kirim ulang event order.created dari RabbitMQ UI
# -> tidak ada pembayaran kedua, tidak ada notifikasi kedua

# 4. Pesan gagal: lihat dead-letter queue
docker compose exec rabbitmq rabbitmqctl list_queues name messages | grep dlq

# 5. Skala horizontal tanpa mengubah konfigurasi
docker compose up -d --scale product-service=3   # Gateway otomatis round-robin
```

---

## Roadmap singkat

| Tahap | Status |
|---|---|
| 1 · Setup & Containerization | ✅ selesai |
| 2 · Backend Microservices | ✅ selesai |
| 3 · Frontend Next.js | ✅ selesai |
| 4 · Logging, Monitoring & CI/CD | 🚧 dashboard, alert, tracing, Testcontainers |
| 5 · Produksi & Skala | ⬜ outbox, K8s, rate limiting, SLO |

Detail + langkah konkret: **[docs/07-roadmap.md](docs/07-roadmap.md)**

---

## Catatan penting sebelum produksi

Repo ini adalah **base yang jujur**: hal-hal yang belum siap produksi ditulis apa adanya,
bukan disembunyikan.

- 🔐 `JWT_SECRET` dan `INTERNAL_API_TOKEN` di `.env.example` hanya untuk development —
  ganti dengan secret manager
- 💳 Payment gateway **disimulasikan** (`SimulatedPaymentGateway`) — titik integrasi
  Stripe/PayPal sudah disiapkan lewat interface `PaymentGateway`
- 📧 Email/SMS **disimulasikan** (dicatat ke database + log) — ganti `NotificationService`
  dengan SendGrid/Twilio
- ⚠️ Belum ada **Transactional Outbox** → bisa kehilangan event kalau aplikasi mati
  tepat setelah commit ([penjelasan & solusi](docs/04-events.md#outbox))
- ⚠️ Belum ada **rate limiting** di endpoint auth
- ⚠️ Satu instance PostgreSQL untuk semua database (lokal) — pisahkan di produksi

Semua ini tercantum sebagai checklist di
[docs/06-scalability-and-production.md](docs/06-scalability-and-production.md#8-checklist-sebelum-produksi).

---

## Kontribusi

Pull request diterima. Yang diharapkan:

1. Satu PR = satu perubahan yang bisa dijelaskan dalam satu kalimat
2. Test untuk logika baru (`mvn verify` harus lulus)
3. Komentar menjelaskan **kenapa**, bukan **apa**
4. Perubahan skema database harus lewat migrasi Flyway baru — jangan ubah migrasi lama

---

## Lisensi

MIT — lihat [LICENSE](LICENSE).

<div align="center">

**Kalau repo ini membantumu belajar, beri ⭐ supaya orang lain menemukannya.**

</div>
