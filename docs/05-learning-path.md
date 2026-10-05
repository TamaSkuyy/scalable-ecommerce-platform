# Jalur Belajar

Repo ini bukan tutorial yang harus dibaca dari atas ke bawah. Cara terbaik memakainya:
**jalankan dulu, lalu bongkar satu alur sampai paham, baru tambah fitur.**

Setiap bagian punya (a) apa yang harus kamu pahami, (b) file yang perlu dibaca,
(c) latihan yang bisa diverifikasi.

---

## Cara belajar yang efektif di repo ini

**Jangan** menyalin-tempel lalu berharap paham. Lakukan siklus ini:

1. **Prediksi** — sebelum membaca kode, tebak bagaimana sesuatu bekerja
2. **Baca** — cek tebakanmu, catat yang meleset
3. **Rusak** — ubah satu hal, lihat apa yang gagal
4. **Jelaskan** — tulis 3 kalimat penjelasan tanpa membuka kode

Kalau kamu tidak bisa menjelaskannya dalam 3 kalimat, kamu belum paham. Ulangi langkah 2–3.

---

## Level 1 — Menjalankan & Membaca (1–2 hari)

**Target:** bisa menjelaskan apa yang terjadi dari klik "Checkout" sampai pesanan `PAID`.

| # | Topik | Baca | Verifikasi |
|---|---|---|---|
| 1.1 | Jalankan seluruh sistem | [01-getting-started.md](01-getting-started.md) | `curl http://localhost:8080/actuator/health` → UP |
| 1.2 | Service discovery | `DiscoveryServiceApplication`, `application.yml` Eureka | Buka http://localhost:8761, hitung service terdaftar |
| 1.3 | Routing Gateway | `GatewayRoutesConfig` | `curl localhost:8080/api/v1/products \| head` |
| 1.4 | Error seragam | `ApiExceptionHandler`, `ApiError` | `curl localhost:8080/api/v1/products/tidak-ada` |
| 1.5 | Alur checkout | `OrderService.checkout` → `OrderStore.create` → `OrderPostCommitListener` | Lakukan checkout, ikuti log 3 service |

**Latihan L1:**
- Matikan `product-service` (`docker compose stop product-service`), lalu buka katalog.
  Error apa yang muncul? Setelah 10 detik, coba 10× lagi — apakah responsnya berubah
  (circuit breaker terbuka)? Amati log `cart-service`.
- Cari satu `X-Correlation-Id` dan lacak request yang sama di seluruh log.

---

## Level 2 — Dasar Spring Boot & JPA (1–2 minggu)

**Target:** bisa menambah endpoint lengkap (entity → repository → service → controller → test)
tanpa menyalin kode yang tidak kamu pahami.

| # | Topik | Baca | Konsep kunci |
|---|---|---|---|
| 2.1 | Dependency Injection | semua `*Service` constructor | Kenapa constructor injection, bukan `@Autowired` di field |
| 2.2 | Lapisan aplikasi | `product-service` (`Product` → `ProductRepository` → `ProductService` → `ProductController`) | Tidak ada entity yang keluar dari lapisan service |
| 2.3 | DTO & validasi | `CreateProductRequest`, `RegisterRequest` | Kenapa `record` + Bean Validation |
| 2.4 | Exception handling | `ApiException`, `ApiExceptionHandler` | Bedakan "error yang diharapkan" dari "bug" |
| 2.5 | Transaksi JPA | `OrderStore`, `ProductService.adjustStock` | `@Transactional` harus di method **public**, dipanggil dari bean lain |
| 2.6 | N+1 query | `ProductRepository.search` (`join fetch`), `OrderRepository.findDetailByOrderNumber` | Aktifkan `spring.jpa.show-sql=true` dan hitung query-nya |
| 2.7 | Migrasi skema | `src/main/resources/db/migration/*.sql` | Kenapa `ddl-auto` bukan `update` |
| 2.8 | Pagination | `PageResponse`, `ProductController.toPageable` | Kenapa whitelist untuk `sort` |
| 2.9 | Testing | `ProductServiceSlugTest`, `OrderTest` | Test logika tanpa Spring context = cepat & tidak flaky |

**Latihan L2:**
1. Tambahkan `GET /api/v1/products/{slug}/related` — produk lain di kategori yang sama,
   maksimal 4, hanya yang stoknya tersedia. Tulis testnya juga.
2. Aktifkan `logging.level.org.hibernate.SQL=DEBUG` dan `spring.jpa.show-sql=true`.
   Lakukan satu request katalog. **Hitung query yang dijalankan.** Lalu hapus
   `join fetch` dan hitung lagi. Selisihnya adalah pelajaran N+1.
3. Buat `GET /api/v1/products/{slug}/reviews` dengan entity + migrasi Flyway baru.

---

## Level 3 — Microservices Essentials (2–4 minggu)

**Target:** bisa menjelaskan apa yang terjadi saat satu service mati, dan mengapa.

| # | Topik | Baca | Pertanyaan yang harus bisa kamu jawab |
|---|---|---|---|
| 3.1 | Service discovery | `application.yml` Eureka + `@EnableDiscoveryClient` | Kenapa `lb://product-service`, bukan `http://10.0.1.7:8082` |
| 3.2 | Klien deklaratif | `ProductClient`, `CartClient` (Feign) | Bagaimana Feign tahu di mana service tujuan |
| 3.3 | Circuit breaker | `CatalogGateway`, `CartGateway`, config `resilience4j` | Kenapa 404 tidak dihitung sebagai kegagalan |
| 3.4 | Timeout | `spring.cloud.openfeign.client.config` | Kenapa timeout default (tanpa batas) sangat berbahaya |
| 3.5 | Saga + kompensasi | `OrderService.checkout` (blok `try/catch` `reserve`) | Bagaimana stok tidak "bocor" saat gagal di item ke-3 |
| 3.6 | Idempotensi | `PaymentService.handleOrderCreated`, `OrderStore.markPaid` | Kenapa at-least-once berarti kode harus tahan duplikat |
| 3.7 | Event-driven | [04-events.md](04-events.md), `OrderEventPublisher`, `NotificationListener` | Kenapa email tidak dikirim di dalam transaksi |
| 3.8 | BFF & keamanan token | `frontend/web/src/middleware.ts`, `app/api/auth/*` | Kenapa JWT di httpOnly cookie, bukan localStorage |
| 3.9 | Auth di edge | `api-gateway/SecurityConfig`, `UserHeaderRelayFilter` | Kenapa Gateway harus **menghapus** `X-User-Id` dari klien |

**Latihan L3:**
1. **Simulasi cascading failure.** Beri product-service query yang lambat
   (`SELECT pg_sleep(5)`). Lakukan checkout. Amati: apakah cart-service ikut lambat?
   Setelah 10 percobaan, apakah circuit breaker terbuka? Apa pesan yang diterima user?
2. **Idempotensi.** Kirim ulang event `order.created` secara manual dari RabbitMQ UI.
   Pastikan **tidak** ada pembayaran kedua dan **tidak** ada notifikasi kedua.
3. **Kompensasi.** Buat salah satu produk stoknya 1, lalu checkout 5 unit bersamaan
   dengan produk lain. Pastikan stok produk pertama tidak berkurang.
4. **Outbox.** Implementasikan pola Transactional Outbox (lihat
   [04-events.md §8](04-events.md#outbox)). Ini latihan paling berharga di level ini.

---

## Level 4 — Frontend Next.js (2–3 minggu)

| # | Topik | Baca | Konsep |
|---|---|---|---|
| 4.1 | App Router & Server Components | `src/app/products/page.tsx` | Server Component tidak punya `useState`/`useEffect` |
| 4.2 | Client Components | `src/app/cart/page.tsx` | Kapan `'use client'` benar-benar dibutuhkan |
| 4.3 | SSR / ISR | `export const revalidate` di halaman katalog | Kapan ISR lebih tepat daripada SSR |
| 4.4 | BFF pattern | `src/app/api/auth/login/route.ts` | Route Handler sebagai perantara |
| 4.5 | Middleware & cookie | `src/middleware.ts` | Menyuntikkan `Authorization` dari cookie httpOnly |
| 4.6 | Streaming & loading | `loading.tsx`, `error.tsx`, `not-found.tsx` | Skeleton tanpa JavaScript klien |
| 4.7 | SEO | `generateMetadata` di `products/[slug]/page.tsx` | Kenapa SSR penting untuk e-commerce |

**Latihan L4:**
1. Ubah katalog dari ISR (`revalidate = 60`) menjadi SSR penuh (`dynamic = 'force-dynamic'`).
   Ukur waktu load. Kapan masing-masing lebih tepat?
2. Tambahkan `generateStaticParams` di halaman detail produk untuk pre-render 10 produk populer.
3. Tambahkan halaman `/search` dengan debounce di sisi klien.

---

## Level 5 — Produksi (1–2 bulan)

| # | Topik | Baca |
|---|---|---|
| 5.1 | Observability | [02-architecture.md §9](02-architecture.md#9-observability), `infra/prometheus/prometheus.yml` |
| 5.2 | Centralized logging | Bagian Loki di [06-scalability-and-production.md](06-scalability-and-production.md) |
| 5.3 | CI/CD | `.github/workflows/ci.yml` |
| 5.4 | Kubernetes | `infra/k8s/` |
| 5.5 | Skalabilitas & bottleneck | [06-scalability-and-production.md](06-scalability-and-production.md) |
| 5.6 | Keamanan lanjutan | [02-architecture.md §6](02-architecture.md#6-keamanan-dari-browser-sampai-database) |

---

## Urutan baca file (kalau bingung mulai dari mana)

```
1.  docs/01-getting-started.md
2.  backend/platform-common/.../api/ApiError.java
3.  backend/platform-common/.../api/ApiExceptionHandler.java
4.  backend/product-service/.../ProductController.java
5.  backend/product-service/.../ProductService.java
6.  backend/product-service/.../domain/Product.java
7.  backend/product-service/src/main/resources/db/migration/V1__init.sql
8.  backend/api-gateway/.../config/GatewayRoutesConfig.java
9.  backend/api-gateway/.../config/SecurityConfig.java
10. backend/api-gateway/.../filter/UserHeaderRelayFilter.java
11. backend/user-service/.../security/TokenService.java
12. backend/order-service/.../service/OrderService.java          <-- inti
13. backend/order-service/.../service/OrderStore.java
14. backend/order-service/.../event/OrderPostCommitListener.java
15. backend/payment-service/.../service/PaymentService.java
16. backend/notification-service/.../event/NotificationListener.java
17. frontend/web/src/middleware.ts
18. frontend/web/src/app/products/page.tsx
```

---

## Jebakan yang paling sering dialami pemula

| Gejala | Penyebab yang sering | Perbaikan |
|---|---|---|
| `@Transactional` tidak bekerja | Method `private`, atau dipanggil dari dalam kelas yang sama (self-invocation) | Jadikan `public` dan pindahkan ke bean terpisah (lihat `OrderStore`) |
| LazyInitializationException | Mengakses relasi lazy di luar transaksi (mis. saat serialisasi JSON) | `join fetch`, atau pakai DTO di dalam transaksi; `open-in-view` sudah `false` di sini |
| Entity terkirim ke klien | Controller mengembalikan entity | Selalu pakai DTO (`XxxResponse.from(entity)`) |
| Service tidak terdaftar di Eureka | `EUREKA_URL` salah, atau service mati saat start | Cek `docker compose logs <service>` |
| `401` padahal token benar | `JWT_SECRET` berbeda antara user-service dan gateway | Samakan lewat env var |
| Pesan berputar di RabbitMQ | `default-requeue-rejected: true` | Set `false` + dead-letter exchange |
| `Failed to convert message` / `not in the trusted packages` | `Jackson2JsonMessageConverter` menolak kelas event di luar daftar aman | Tambahkan paketmu ke `TRUSTED_PACKAGES` ([detail](04-events.md#6-trusted-packages--jebakan-pertama-yang-akan-kamu-temui)) — **jangan** pakai `"*"` |
| Semua panggilan antar-service gagal `503` / `NoFallbackAvailableException: No fallback available.` | `spring.cloud.openfeign.circuitbreaker.enabled: true` membungkus Feign dengan TimeLimiter **bawaan 1 detik** | Set `false`, lalu pasang circuit breaker lewat `@CircuitBreaker` di kelas pembungkus seperti `CatalogGateway` |
| Konsumen crash pada pesan tertentu | Tidak ada DLQ, jadi pesan kembali ke queue terus | DLQ + `max-attempts` (sudah dikonfigurasi) |
| Event terkirim padahal transaksi rollback | Publish di dalam transaksi tanpa `AFTER_COMMIT` | `@TransactionalEventListener(AFTER_COMMIT)` |
| Stok minus | Tidak ada penguncian saat update stok | `SELECT ... FOR UPDATE` + `CHECK (stock >= 0)` |
| Data harga basi | Memakai harga dari keranjang | Ambil ulang dari katalog saat checkout |

---

## Contoh pertanyaan wawancara yang bisa kamu jawab setelah ini

- "Bagaimana kamu mencegah oversell saat flash sale?" → optimistic/pessimistic locking,
  `CHECK` constraint, reservasi stok terpisah
- "Bagaimana kamu menjaga konsistensi data antar-service?" → saga + kompensasi,
  dan kenapa eventual consistency diterima di sini
- "Bagaimana kamu menangani service yang mati?" → timeout, retry, circuit breaker,
  fallback, dan DLQ untuk pesan
- "Kenapa event kamu tidak boleh dikirim di dalam transaksi?" → event hantu saat rollback;
  pakai `AFTER_COMMIT` + outbox untuk kasus crash
- "Bagaimana kamu melacak satu request lintas 6 service?" → correlation id di MDC
- "Bagaimana kamu menyimpan JWT dengan aman di aplikasi Next.js?" → BFF + httpOnly cookie

---

## Sumber belajar yang layak

**Spring / Java**
- Spring Boot reference: https://docs.spring.io/spring-boot/index.html
- Spring Cloud Gateway: https://docs.spring.io/spring-cloud-gateway/reference/
- Spring Cloud OpenFeign: https://docs.spring.io/spring-cloud-openfeign/reference/
- Baeldung (untuk contoh cepat, tapi selalu verifikasi dengan dokumentasi resmi)

**Arsitektur**
- *Microservices Patterns* — Chris Richardson (saga, outbox, API composition)
- *Designing Data-Intensive Applications* — Martin Kleppmann (fondasi konsistensi)
- Microsoft Cloud Design Patterns: https://learn.microsoft.com/azure/architecture/patterns/

**Next.js**
- https://nextjs.org/docs/app
- https://nextjs.org/docs/app/building-your-application/data-fetching

**RabbitMQ**
- https://www.rabbitmq.com/docs/tutorials (tutorial 1–6 wajib)
- https://www.rabbitmq.com/docs/dlx (dead-letter exchange)

---

## Aturan terakhir

Kalau kamu hanya bisa melakukan **satu** hal dari dokumen ini, lakukan ini:

> Ambil satu alur (checkout), baca kodenya lima kali, lalu gambar ulang diagramnya
> dari ingatan. Satu alur yang benar-benar dipahami jauh lebih berharga daripada
> dua puluh file yang pernah dilihat sekali.
