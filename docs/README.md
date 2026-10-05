# Dokumentasi ShopFlow

Urutan baca yang disarankan:

| # | Dokumen | Isi | Untuk siapa |
|---|---|---|---|
| 1 | [01-getting-started.md](01-getting-started.md) | Menjalankan proyek, verifikasi, troubleshooting | Hari pertama |
| 2 | [02-architecture.md](02-architecture.md) | Keputusan arsitektur & alasannya | Sebelum mengubah struktur |
| 3 | [03-api-contract.md](03-api-contract.md) | Semua endpoint + contoh `curl` | Saat membuat klien / integrasi |
| 4 | [04-events.md](04-events.md) | RabbitMQ, idempotensi, outbox, DLQ | Saat menambah event |
| 5 | [05-learning-path.md](05-learning-path.md) | Kurikulum bertingkat + latihan | Untuk belajar terstruktur |
| 6 | [06-scalability-and-production.md](06-scalability-and-production.md) | Skala, observability, Kubernetes, checklist produksi | Sebelum deploy nyata |
| 7 | [07-roadmap.md](07-roadmap.md) | Status & rencana per tahap | Untuk melihat gambaran besar |

---

## Cara cepat menemukan sesuatu

**"Saya ingin tahu bagaimana X bekerja"**

| X | Baca |
|---|---|
| Service saling menemukan | [02 §3–4](02-architecture.md) + `backend/discovery-service/` |
| JWT diterbitkan & diverifikasi | [02 §6](02-architecture.md) + `user-service/security/TokenService.java` |
| Header identitas diteruskan | `api-gateway/.../filter/UserHeaderRelayFilter.java` |
| Checkout & kompensasi stok | [02 §5](02-architecture.md) + `order-service/.../OrderService.java` |
| Event terkirim & diterima | [04-events.md](04-events.md) |
| Keranjang disimpan di Redis | `cart-service/.../repository/CartRepository.java` |
| Error jadi JSON seragam | `platform-common/.../api/ApiExceptionHandler.java` |
| Frontend menyimpan token | `frontend/web/src/middleware.ts` + `src/app/api/auth/` |

**"Saya ingin menambahkan…"**

| Yang ditambahkan | Mulai dari | Jangan lupa |
|---|---|---|
| Endpoint baru | Controller → Service → (Repository) | DTO + validasi + test |
| Service baru | Salin `order-service` sebagai kerangka | Daftarkan di parent POM, Gateway, compose, Prometheus, CI |
| Event baru | `platform-common/.../events/EventRouting.java` | DTO + publisher + binding di konsumen + DLQ |
| Tabel baru | `src/main/resources/db/migration/V2__*.sql` | Entity harus cocok (`ddl-auto: validate`) |
| Halaman baru | `frontend/web/src/app/` | Kalau butuh login, tambahkan path ke `middleware.ts` |
| Perubahan skema | Migrasi Flyway baru | **Jangan** ubah migrasi yang sudah dijalankan |
