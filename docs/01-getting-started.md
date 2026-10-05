# Mulai dari Nol

Panduan ini mengasumsikan kamu baru pertama kali membuka repo ini.

---

## 1. Yang perlu dipasang

| Alat | Versi minimal | Cara cek |
|---|---|---|
| **JDK** | 21 | `java -version` |
| **Docker + Compose** | Docker 24+ | `docker compose version` |
| **Node.js** | 20+ | `node -v` |
| **pnpm** | 9+ | `pnpm -v` (`npm i -g pnpm` kalau belum ada) |
| **Git** | apa saja | `git --version` |

**Maven tidak perlu dipasang** — repo ini menyertakan Maven Wrapper (`./mvnw`).

```bash
# Pastikan environment siap
java -version && docker compose version && node -v && pnpm -v
```

---

## 2. Dua cara menjalankan proyek

### Cara A — Semua di Docker (paling mudah, disarankan untuk pertama kali)

```bash
cp .env.example .env        # opsional: ubah password/secret kalau mau
make up                     # build + jalankan 8 service backend + infrastruktur
```

Build pertama akan memakan waktu **10–20 menit** (mengunduh dependency Maven + 9 image Docker).
Build berikutnya jauh lebih cepat karena layer di-cache.

Cek progres:

```bash
docker compose ps
make logs
```

Sampai semua container `healthy`:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP", ...}
```

Buka [http://localhost:8761](http://localhost:8761) — kamu akan melihat 7 service terdaftar
di Eureka (`API-GATEWAY`, `USER-SERVICE`, `PRODUCT-SERVICE`, dst.).
**Kalau Eureka kosong, hampir selalu masalahnya: service belum selesai start atau
`EUREKA_URL` salah.**

### Cara B — Infrastruktur di Docker, service di host (untuk ngoding)

Ini yang paling nyaman saat belajar, karena kamu bisa mengubah kode dan restart cepat
tanpa build Docker.

```bash
# 1. Jalankan hanya infrastruktur
make infra          # postgres + redis + rabbitmq

# 2. Jalankan service satu per satu, di terminal masing-masing
make dev-discovery-service      # tunggu sampai http://localhost:8761 hidup
make dev-user-service
make dev-product-service
make dev-cart-service
make dev-order-service
make dev-payment-service
make dev-notification-service
make dev-api-gateway

# 3. Frontend
cd frontend/web
cp .env.example .env.local
pnpm install
pnpm dev                        # http://localhost:3000
```

> **Catatan:** port `5432`, `6379`, `5672`, `15672` sudah dipublikasikan ke host, jadi
> service yang jalan di host bisa langsung terhubung dengan konfigurasi default.

---

## 3. Verifikasi bahwa semuanya benar-benar jalan

Ikuti urutan ini; setiap langkah bergantung pada langkah sebelumnya.

### 3.1 Registry hidup

```bash
curl -s http://localhost:8761/actuator/health | head -c 200
```

### 3.2 Gateway bisa membaca katalog (endpoint publik, tanpa login)

```bash
curl -s "http://localhost:8080/api/v1/products?size=3" | head -c 600
```

Harus muncul JSON berisi 18 produk contoh. Kalau muncul `503` → product-service belum
terdaftar di Eureka. Kalau `404` → rutenya salah. Kalau timeout → Gateway belum siap.

### 3.3 Registrasi & login

```bash
# Daftar akun baru
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"budi@example.com","password":"rahasia12345","fullName":"Budi Santoso"}'

# Login -> simpan token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"budi@example.com","password":"rahasia12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "$TOKEN" | head -c 40
```

### 3.4 Tambah ke keranjang (butuh token)

```bash
curl -s -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}' | python3 -m json.tool
```

### 3.5 Checkout — dan lihat siklus event lengkapnya

```bash
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "shippingAddress":{
      "recipientName":"Budi Santoso",
      "phone":"08123456789",
      "line1":"Jl. Merdeka No. 10",
      "city":"Bandung",
      "postalCode":"40115",
      "country":"Indonesia"
    },
    "notes":"Tolong dibungkus rapi"
  }' | python3 -m json.tool
```

Catat `orderNumber` yang keluar, lalu **dalam beberapa detik**:

```bash
# order-service menerima hasil pembayaran
docker compose logs --tail=50 order-service | grep -E "Pesanan|PAID|PAYMENT"

# payment-service melakukan charge
docker compose logs --tail=50 payment-service | grep -E "SIMULASI|BERHASIL|GAGAL"

# notification-service "mengirim email"
docker compose logs --tail=50 notification-service | grep -A5 "EMAIL TERKIRIM"

# Cek status akhir pesanan
curl -s http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Status akan berubah dari `PENDING_PAYMENT` menjadi `PAID` (90% kemungkinan) atau
`PAYMENT_FAILED` (10%). **Inilah nilai asinkron**: respons `POST /orders` sudah kembali
sebelum pembayaran selesai.

### 3.6 Uji jalur gagal dengan sengaja

```bash
# Buat payment-service SELALU gagal
SHOPFLOW_PAYMENT_SUCCESS_RATE=0.0 docker compose up -d payment-service

# Checkout lagi -> pesanan akan berakhir PAYMENT_FAILED
# dan stok yang ditahan akan dilepas kembali
```

Periksa juga di RabbitMQ UI ([http://localhost:15672](http://localhost:15672),
`shopflow` / `shopflow`): kamu bisa melihat exchange, queue, dan pesan yang gagal
berpindah ke `shopflow.events.dlq`.

---

## 4. Alur pemakaian lewat browser

1. Buka [http://localhost:3000](http://localhost:3000)
2. Klik **Daftar**, buat akun
3. Jelajahi **Katalog**, buka detail produk, klik **Tambah ke Keranjang**
4. Buka **Keranjang**, ubah jumlah
5. **Checkout**, isi alamat
6. Kamu diarahkan ke halaman pesanan — statusnya `Menunggu Pembayaran` sejenak,
   lalu berubah sendiri setelah event pembayaran diproses
7. Buka **Akun** → lihat daftar notifikasi (simulasi email yang dikirim notification-service)

**Coba juga:** buat akun dengan email yang sama dua kali → muncul error dari
`user-service` dalam format `ApiError` yang sama dengan service lain.

---

## 5. Port dan URL

| URL | Apa itu |
|---|---|
| http://localhost:3000 | Frontend Next.js |
| http://localhost:8080 | **API Gateway** — satu-satunya pintu masuk API |
| http://localhost:8080/swagger-ui.html | Swagger UI (di service, lewat gateway belum dirutekan) |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:15672 | RabbitMQ Management (`shopflow` / `shopflow`) |
| http://localhost:9090 | Prometheus |
| http://localhost:3001 | Grafana (`admin` / `admin`) |
| localhost:5432 | PostgreSQL (`shopflow` / `shopflow`) |
| localhost:6379 | Redis |

Port service individual (`8081`–`8086`) **sengaja tidak dipublikasikan** — hanya bisa
diakses dari dalam jaringan Docker. Ini keputusan keamanan: header `X-User-Id` hanya
boleh datang dari Gateway.

Untuk debugging, kamu tetap bisa masuk ke dalamnya:

```bash
docker compose exec api-gateway sh
curl http://product-service:8082/actuator/health
```

---

## 6. Perintah yang sering dipakai

```bash
make                     # daftar semua perintah
make up                  # jalankan semua
make infra               # hanya postgres+redis+rabbitmq
make logs                # ikuti log
make build               # build backend tanpa test
make test                # unit test backend
make verify              # build + test lengkap
make dev-product-service # jalankan satu service di host
make fe-dev              # Next.js dev server
make reset-data          # HAPUS semua volume (mulai dari nol)
```

---

## 7. Kalau ada masalah

### Eureka kosong / service tidak saling menemukan

Pastikan `EUREKA_URL` benar. Di dalam Docker: `http://discovery-service:8761/eureka/`.
Di host: `http://localhost:8761/eureka/`.

### `503 Service Unavailable` dari Gateway

Artinya Gateway meneruskan ke service yang belum terdaftar atau sedang mati.

```bash
docker compose ps                       # apakah service-nya healthy?
docker compose logs product-service | tail -50
curl http://localhost:8761/eureka/apps  # apakah terdaftar?
```

### `401 Unauthorized` padahal sudah login

Token kedaluwarsa (default 30 menit), atau `JWT_SECRET` berbeda antara `user-service`
dan `api-gateway`. Keduanya **wajib** memakai secret yang sama.

```bash
docker compose exec user-service   env | grep JWT_SECRET
docker compose exec api-gateway    env | grep JWT_SECRET
```

### Flyway gagal: "relation already exists"

Biasanya karena volume lama dari versi kode sebelumnya.

```bash
make reset-data && make up
```

### Cek container yang unhealthy

```bash
docker compose ps --format "table {{.Name}}\t{{.Status}}"
docker inspect --format '{{json .State.Health}}' shopflow-product | python3 -m json.tool | tail -20
```

### Port sudah dipakai

Ubah port di host pada `docker-compose.yml` (bagian `ports:`, sisi kiri), mis.
`"18080:8080"` kalau port 8080 dipakai aplikasi lain.

---

## 8. Langkah belajar berikutnya

Setelah aplikasi berjalan, jangan langsung menambah fitur. Lakukan ini dulu:

1. **Baca satu alur sampai tuntas** — buka `OrderService.checkout()`, ikuti sampai
   `NotificationService.onPaymentCompleted()`. Gambar diagramnya sendiri.
2. **Rusak dengan sengaja** — matikan `product-service`, lakukan checkout, amati
   error yang muncul dan pesan yang dilihat user. Apakah pesannya masuk akal?
3. **Cari satu bug** — hentikan `payment-service`, checkout, nyalakan lagi.
   Pesanan `order.created` masih ada di queue? Apakah akhirnya diproses?
4. **Tambahkan satu hal kecil** — endpoint `GET /api/v1/products/{slug}/related`,
   lengkap dengan test.

Lanjutkan ke [05-learning-path.md](05-learning-path.md).
