# Arsitektur ShopFlow

Dokumen ini menjelaskan **mengapa** sistem ini disusun seperti ini, bukan hanya **apa** isinya.
Kalau kamu baru mulai, baca [01-getting-started.md](01-getting-started.md) dulu.

---

## 1. Gambaran besar

```
                          ┌───────────────────────┐
                          │   Next.js (App Router)│
                          │   SSR / ISR / Client  │
                          └───────────┬───────────┘
                                      │ HTTPS /api/v1/**
                                      ▼
                     ┌────────────────────────────────────┐
                     │   API GATEWAY  (Spring Cloud GW)   │
                     │   • routing (lb://)                │
                     │   • verifikasi JWT  (sekali saja)  │
                     │   • CORS                           │
                     │   • menyuntik X-User-Id / Roles    │
                     └───┬────────┬────────┬────────┬─────┘
                         │        │        │        │
        ┌────────────────┘        │        │        └────────────────┐
        ▼                         ▼        ▼                         ▼
 ┌─────────────┐          ┌─────────────┐ ┌────────────┐    ┌──────────────┐
 │ user-service│          │product-svc  │ │ cart-svc   │    │ order-svc    │
 │ PostgreSQL  │          │ PostgreSQL  │ │  REDIS     │    │ PostgreSQL   │
 └─────────────┘          │ + Caffeine  │ └─────┬──────┘    └──────┬───────┘
                          └──────┬──────┘       │ Feign            │ Feign
                                 │              └──────────────────┘
                                 │  Feign (internal)
                                 ▼
                          ┌──────────────────────────────────────────────┐
                          │        RabbitMQ  (exchange: shopflow.events) │
                          └───┬──────────────────────────┬───────────────┘
                              │ order.created            │ payment.completed / failed
                              ▼                          ▼
                     ┌─────────────────┐        ┌──────────────────┐
                     │ payment-service │        │ notification-svc │
                     │ PostgreSQL      │        │ PostgreSQL       │
                     └─────────────────┘        └──────────────────┘

        Semua service mendaftar ke  →  discovery-service (Eureka)
        Semua service mengirim metrik ke  →  Prometheus → Grafana
```

---

## 2. Kenapa microservices? (dan kapan JANGAN memakainya)

Microservices **bukan** default yang benar. Ini yang kamu dapat dan yang kamu bayar:

| Kamu dapat | Kamu bayar |
|---|---|
| Tim bisa deploy mandiri tanpa saling menunggu | Debugging jadi lintas proses (butuh tracing) |
| Skala per-bagian (katalog butuh 10 replika, pembayaran cukup 2) | Transaksi lintas service tidak lagi gratis (saga) |
| Isolasi kegagalan (cart mati, katalog tetap hidup) | Konsistensi data jadi *eventual* |
| Teknologi berbeda per service | Jaringan selalu bisa gagal → retry, timeout, circuit breaker |
| Database terpisah = batas kepemilikan data yang jelas | Join lintas domain jadi mustahil → duplikasi data sengaja |

**Aturan praktis:** mulai monolit modular. Pecah saat ada *alasan nyata* (skala tim, skala trafik,
atau kebutuhan ketersediaan yang berbeda). Proyek ini sengaja memakai microservices sebagai
**media belajar** — dan setiap keputusan yang "mahal" di sini diberi komentar alternatifnya.

---

## 3. Tanggung jawab setiap service

| Service | Port | Database | Tanggung jawab | Tidak boleh tahu tentang |
|---|---|---|---|---|
| `discovery-service` | 8761 | – | Registry: siapa ada di mana | Semua domain bisnis |
| `api-gateway` | 8080 | – | Routing, auth di edge, CORS | Semua domain bisnis |
| `user-service` | 8081 | `shopflow_user` | Akun, password (BCrypt), penerbit JWT | Produk, pesanan |
| `product-service` | 8082 | `shopflow_product` | Katalog, kategori, **stok** | Siapa yang membeli |
| `cart-service` | 8083 | **Redis** | Keranjang sementara | Pesanan, pembayaran |
| `order-service` | 8084 | `shopflow_order` | Pesanan, saga checkout, status | Detail cara bayar |
| `payment-service` | 8085 | `shopflow_payment` | Charge ke gateway, status bayar | Isi keranjang |
| `notification-service` | 8086 | `shopflow_notification` | Email/SMS (disimulasikan) | Semua detail teknis lainnya |

**Aturan emas:** kalau kamu butuh data milik service lain, ada dua pilihan —
panggil API-nya (sinkron) atau simpan salinannya dari event (asinkron).
Yang **tidak boleh**: query langsung ke database service lain.

---

## 4. Pola komunikasi: sinkron vs asinkron

### Sinkron (REST + OpenFeign) — dipakai saat butuh jawaban sekarang

```
order-service ──GET  cart-service  /api/v1/internal/cart/{userId}      → isi keranjang
order-service ──GET  product-service /api/v1/internal/products/{id}    → harga & stok terkini
order-service ──POST product-service /{id}/stock/reserve               → tahan stok
```

Cocok untuk: validasi yang harus selesai sebelum melanjutkan, dan data yang cepat berubah
(harga, stok). Selalu dibungkus **timeout + circuit breaker** — lihat `CatalogGateway`.

### Asinkron (RabbitMQ) — dipakai saat "nanti juga tidak apa-apa"

```
order-service    ──order.created──────►  payment-service
                                        notification-service
payment-service  ──payment.completed──►  order-service
                 ──payment.failed─────►  order-service + notification-service
order-service    ──order.cancelled────►  notification-service
```

Cocok untuk: efek samping yang tidak boleh menahan respons user (kirim email, update analytics,
proses pembayaran). Detail lengkap: [04-events.md](04-events.md).

**Cara memilih dengan cepat:** kalau kegagalan langkah itu harus membatalkan seluruh
operasi → sinkron. Kalau bisa diselesaikan lalu → asinkron.

---

## 5. Alur checkout (saga orchestration)

Ini bagian paling penting untuk dipahami.

```
POST /api/v1/orders
        │
        │  ┌──────────────────────────────────────────────────────────┐
        │  │  OrderService.checkout()  — TANPA transaksi database     │
        │  └──────────────────────────────────────────────────────────┘
        ▼
 1. Ambil keranjang dari cart-service                    (Feign, sinkron)
        │   keranjang kosong? → 400
        ▼
 2. Untuk setiap item:
       a. GET produk (harga & stok terkini)              (Feign, sinkron)
       b. POST /stock/reserve                            (Feign, sinkron)
       c. catat di daftar `reserved` untuk kompensasi
        │   gagal di item ke-N? → LEPAS semua stok 1..N-1, lalu lempar error
        ▼
 3. OrderStore.create() — TRANSAKSI DB PENDEK
       a. simpan pesanan (status PENDING_PAYMENT)
       b. antre domain event (dikirim SETELAH commit)
        │   gagal simpan? → LEPAS semua stok
        ▼
 4. [setelah commit] OrderPostCommitListener
       a. kosongkan keranjang (boleh gagal, hanya di-log)
       b. publish `order.created` ke RabbitMQ
        ▼
 5. payment-service menerima `order.created`
       a. sudah ada payment untuk order ini? → abaikan (idempoten)
       b. charge ke gateway (disimulasikan)
       c. publish `payment.completed` atau `payment.failed`
        ▼
 6. order-service menerima hasil
       • completed → status PAID
       • failed    → status PAYMENT_FAILED + LEPAS stok yang ditahan
```

### Detail yang mudah terlewat

**a) Kompensasi harus tercatat selama proses berjalan.**
Kita menambahkan ke `reserved` **setelah** `reserve()` sukses. Kalau ditambahkan sebelum,
kita akan mencoba melepas stok yang tidak pernah ditahan.

**b) Urutan "ubah status dulu, baru lepas stok"** (lihat `OrderService.cancel`).
Kalau dibalik, dua request pembatalan bersamaan bisa melepas stok dua kali.
Dengan urutan ini, request yang kalah gagal di pengecekan status (dijaga `@Version`)
dan tidak melepas apa pun. Risikonya berubah menjadi "stok tertahan" — jauh lebih mudah
direkonsiliasi daripada stok negatif.

**c) Konsumen event harus idempoten.**
RabbitMQ menjamin *at-least-once*, bukan *exactly-once*. Setiap konsumen kita memakai
penjaga: `existsByOrderNumber` + UNIQUE constraint. Lihat `payment-service` dan
`notification-service`.

**d) Event hanya dikirim setelah commit.**
`@TransactionalEventListener(AFTER_COMMIT)` mencegah "event hantu" — yaitu konsumen
menerima kabar pesanan yang ternyata di-rollback.

**e) Masih ada celah yang harus kamu tahu.**
Kalau aplikasi mati tepat setelah commit tapi sebelum pesan terkirim, event itu hilang.
Solusi industri adalah **Transactional Outbox**: tulis event ke tabel `outbox` di transaksi
yang sama, lalu poller mengirim & menandainya. Detail: [04-events.md](04-events.md#outbox).

---

## 6. Keamanan: dari browser sampai database

```
1. Browser  ──login──►  Next.js Route Handler (/api/auth/login)
                        │  menyimpan JWT di cookie httpOnly
                        ▼
2. Browser  ──/api/v1/...──►  Next.js middleware
                        │  menambahkan header Authorization dari cookie
                        ▼
3. API Gateway  ──memverifikasi JWT──►  401 kalau tidak valid
                ──menghapus X-User-Id palsu dari klien──►
                ──menambahkan X-User-Id / X-User-Email / X-User-Roles yang tepercaya──►
                        ▼
4. Service hilir  ──membaca header menjadi parameter `CurrentUser`──►
```

### Empat hal yang sengaja diperhatikan

| Risiko | Penanganan di proyek ini |
|---|---|
| XSS mencuri token | JWT disimpan di cookie **httpOnly**, tidak pernah menyentuh JavaScript |
| Pemalsuan identitas lewat header | Gateway **menghapus** `X-User-Id` dari klien sebelum menulis ulang |
| Panggilan langsung ke service internal | Port service tidak dipublikasikan ke host; hanya Gateway yang terbuka |
| Service lain memalsukan diri | Endpoint `/api/v1/internal/**` butuh `X-Internal-Token` (constant-time compare) |

### Yang BELUM ada (dan sebaiknya kamu tambahkan)

- **Rate limiting** di Gateway (Spring Cloud Gateway + Redis rate limiter)
- **Refresh token / rotasi token** (saat ini access token saja, TTL 30 menit)
- **Verifikasi JWT di service hilir** (defense in depth) atau mTLS antar-service
- **RS256** alih-alih HS256, supaya hanya user-service yang bisa menerbitkan token
- **Audit log** untuk aksi admin

Semuanya tercantum sebagai latihan di [07-roadmap.md](07-roadmap.md).

---

## 7. Struktur repository

```
backend/
├── pom.xml                      # parent: dependency management + versi
├── Dockerfile                   # SATU Dockerfile untuk 9 service (build arg MODULE)
├── platform-common/             # library bersama (BUKAN aplikasi)
│   └── src/main/java/com/shopflow/common/
│       ├── api/                 # ApiError, PageResponse, exception, handler
│       ├── security/            # CurrentUser, JwtProperties, InternalApiTokenFilter
│       ├── events/              # kontrak event + nama queue/routing key
│       ├── feign/               # interceptor internal-token + ErrorDecoder
│       └── web/                 # CorrelationIdFilter
├── discovery-service/
├── api-gateway/
├── user-service/
├── product-service/
├── cart-service/
├── order-service/
├── payment-service/
└── notification-service/

frontend/web/                    # Next.js App Router (BFF + httpOnly cookie)
infra/                           # postgres init, prometheus, grafana, rabbitmq, k8s
docs/                            # dokumen yang sedang kamu baca
```

### Apa isi `platform-common`?

Hanya hal-hal yang **benar-benar lintas semua service**:

- `ApiError` / `PageResponse` — bentuk respons seragam
- `ApiExceptionHandler` — diaktifkan otomatis lewat `AutoConfiguration`
- `CorrelationIdFilter` — `X-Correlation-Id` masuk ke MDC SLF4J
- `CurrentUser` + `CurrentUserArgumentResolver` — identitas sebagai parameter controller
- `EventRouting` + DTO event — kontrak queue & payload
- `FeignSupportAutoConfiguration` — interceptor + `ErrorDecoder` untuk semua klien Feign

Yang **tidak** ada di sana: entity JPA, logika bisnis, atau apa pun yang hanya dipakai
satu service. Kalau ragu, taruh di service-nya dulu — baru naikkan saat benar-benar dipakai
dua tempat.

> **Catatan teknis:** `platform-common` mendeklarasikan `spring-boot-starter-web` sebagai
> `optional`. Ini penting: `api-gateway` berbasis WebFlux, dan kalau `spring-webmvc` bocor
> ke sana, Gateway akan gagal start.

---

## 8. Database per service

| Keputusan | Alasan |
|---|---|
| Satu database per service | Batas kepemilikan data tegas; tidak ada "join diam-diam" antar domain |
| Flyway, bukan `ddl-auto: update` | Setiap perubahan skema bisa di-review di PR dan aman dijalankan di CI/CD |
| `ddl-auto: validate` | Gagal cepat kalau entity dan tabel tidak sinkron |
| Constraint di database | `CHECK (stock_quantity >= 0)` adalah jaring pengaman terakhir anti-oversell |
| `@Version` (optimistic locking) | Mencegah dua penulis saling menimpa tanpa mengunci baris |
| `SELECT ... FOR UPDATE` saat ubah stok | Menyerialkan perubahan stok produk yang sama saja |

**Local development** memakai satu container PostgreSQL dengan lima database
(lihat `infra/postgres/init/01-create-databases.sql`). Di produksi, pisahkan instance —
setidaknya pisahkan kredensial, idealnya pisahkan cluster, supaya satu database yang
bermasalah tidak menjatuhkan seluruh platform.

---

## 9. Observability

| Kebutuhan | Alat di proyek ini |
|---|---|
| Melacak satu request lintas service | `X-Correlation-Id` → MDC → format log `[service,korelasi]` |
| Melihat kesehatan service | `GET /actuator/health` (+ liveness/readiness probe) |
| Metrik | Micrometer → `GET /actuator/prometheus` → Prometheus → Grafana |
| Antrean pesan | RabbitMQ Management UI (`http://localhost:15672`) |
| Menelusuri registry | Eureka dashboard (`http://localhost:8761`) |

Cara cepat memakai correlation id:

```bash
# Ambil sebuah correlation id dari log gateway
docker compose logs api-gateway | grep "Meneruskan identitas" | tail -1

# Cari request yang sama di seluruh service
docker compose logs | grep "<correlation-id>"
```

---

## 10. Padanan YAML untuk rute Gateway

`GatewayRoutesConfig` memakai Java DSL. Kalau kamu lebih suka YAML (misalnya saat
konfigurasi harus bisa diubah tanpa rebuild), padanannya seperti ini:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/**,/api/v1/users/**
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/v1/products/**,/api/v1/categories/**
        - id: cart-service
          uri: lb://cart-service
          predicates:
            - Path=/api/v1/cart/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/v1/payments/**
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**
```

**Kapan memilih YAML:** rute sederhana, ingin diubah lewat Config Server tanpa deploy ulang.
**Kapan memilih Java DSL:** butuh filter kustom, logika kondisional, atau ingin typo path
tertangkap saat compile. Proyek ini memakai Java DSL supaya aman terhadap perubahan nama
properti antar versi Spring Cloud Gateway.

---

## 11. Versi & kompatibilitas

| Komponen | Versi | Catatan |
|---|---|---|
| Java | 21 (LTS) | `maven.compiler.release=21` |
| Spring Boot | 3.5.16 | |
| Spring Cloud | 2025.0.3 | **Harus cocok dengan Spring Boot.** Jangan naikkan salah satu saja |
| Next.js | 16.x | React 19, App Router, Tailwind v4 |
| PostgreSQL | 17 | |
| Redis | 7 | |
| RabbitMQ | 4 (management) | |

Cara memastikan pasangan versi Boot ↔ Cloud benar: lihat
[start.spring.io/spring-cloud](https://start.spring.io) atau halaman
*Release Train Compatibility* di dokumentasi Spring Cloud. Naikkan keduanya dalam satu PR,
lalu jalankan `mvn verify` + `docker compose up --build`.
