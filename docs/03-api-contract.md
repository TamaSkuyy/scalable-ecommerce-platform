# Kontrak API

Semua endpoint bisnis diakses lewat **API Gateway** di `http://localhost:8080`.
Tidak ada klien yang boleh memanggil service individual secara langsung.

- **Base URL:** `http://localhost:8080/api/v1`
- **Autentikasi:** header `Authorization: Bearer <jwt>`
- **Content-Type:** `application/json`
- **Format error:** seragam untuk semua service (lihat [bagian Error](#error))
- **Uang:** selalu bilangan bulat dalam **satuan minor** (sen). `150000` = `Rp 1.500,00`

Endpoint bertanda 🔓 boleh diakses tanpa login. Sisanya butuh token.

---

## Ringkasan

| Method | Path | Auth | Service |
|---|---|---|---|
| POST | `/auth/register` | 🔓 | user-service |
| POST | `/auth/login` | 🔓 | user-service |
| GET | `/users/me` | 🔒 | user-service |
| PATCH | `/users/me` | 🔒 | user-service |
| GET | `/users/{id}` | 🔓 | user-service |
| GET | `/products` | 🔓 | product-service |
| GET | `/products/{slug}` | 🔓 | product-service |
| POST | `/products` | 👑 ADMIN | product-service |
| PATCH | `/products/{id}/stock` | 👑 ADMIN | product-service |
| GET | `/categories` | 🔓 | product-service |
| GET | `/cart` | 🔒 | cart-service |
| POST | `/cart/items` | 🔒 | cart-service |
| PATCH | `/cart/items/{productId}` | 🔒 | cart-service |
| DELETE | `/cart/items/{productId}` | 🔒 | cart-service |
| DELETE | `/cart` | 🔒 | cart-service |
| POST | `/orders` | 🔒 | order-service |
| GET | `/orders` | 🔒 | order-service |
| GET | `/orders/{orderNumber}` | 🔒 | order-service |
| POST | `/orders/{orderNumber}/cancel` | 🔒 | order-service |
| GET | `/payments/{orderNumber}` | 🔒 | payment-service |
| GET | `/payments` | 🔒 | payment-service |
| GET | `/notifications/me` | 🔒 | notification-service |
| POST | `/notifications/{id}/read` | 🔒 | notification-service |

---

## Error

Setiap kegagalan — dari service mana pun, termasuk Gateway — memakai bentuk yang sama.
Ini membuat frontend hanya perlu **satu** error parser.

```json
{
  "timestamp": "2026-10-05T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Stok Kopi Gayo Arabica 250g tidak cukup. Tersedia 3, diminta 5",
  "path": "/api/v1/cart/items",
  "traceId": "9f3c1a7b4e2d8c05",
  "violations": []
}
```

Untuk error validasi, `violations` terisi:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Data yang dikirim tidak valid",
  "violations": [
    { "field": "email", "message": "Format email tidak valid" },
    { "field": "password", "message": "Password harus 8-72 karakter" }
  ]
}
```

| Status | Arti di proyek ini |
|---|---|
| 400 | Request tidak valid (format/semantik) |
| 401 | Token tidak ada / kedaluwarsa / kredensial salah |
| 403 | Terautentikasi tapi tidak berhak (bukan admin, bukan pemilik resource) |
| 404 | Resource tidak ada |
| 409 | Konflik: email sudah dipakai, stok tidak cukup, status pesanan tidak bisa diubah |
| 503 | Service hilang / circuit breaker terbuka |
| 500 | Bug di server (pesan asli TIDAK dibocorkan; pakai `traceId` untuk mencari di log) |

> **Kenapa `traceId` penting?** User melaporkan "gagal checkout". Kamu tinggal mencari
> `traceId` itu di seluruh log service dan melihat perjalanan request-nya lengkap.

---

## Autentikasi

### POST /auth/register 🔓

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"budi@example.com","password":"rahasia12345","fullName":"Budi Santoso"}'
```

`201 Created`

```json
{
  "id": 1,
  "email": "budi@example.com",
  "fullName": "Budi Santoso",
  "roles": ["CUSTOMER"],
  "createdAt": "2026-10-05T10:00:00Z"
}
```

| Field | Aturan |
|---|---|
| `email` | wajib, format email, maks 180 karakter, unik (case-insensitive) |
| `password` | wajib, 8–72 karakter (72 = batas BCrypt) |
| `fullName` | wajib, maks 120 karakter |

### POST /auth/login 🔓

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"budi@example.com","password":"rahasia12345"}'
```

`200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 1800,
  "user": { "id": 1, "email": "budi@example.com", "fullName": "Budi Santoso", "roles": ["CUSTOMER"], "createdAt": "..." }
}
```

> Pesan error untuk "email tidak terdaftar" dan "password salah" **sengaja identik**
> (`Email atau password salah`) supaya endpoint ini tidak bisa dipakai untuk memetakan
> email mana yang terdaftar.

Isi token:

```json
{
  "iss": "shopflow-identity",
  "sub": "1",
  "email": "budi@example.com",
  "name": "Budi Santoso",
  "roles": ["CUSTOMER"],
  "iat": 1790000000,
  "exp": 1790001800
}
```

### GET /users/me 🔒 · PATCH /users/me 🔒

```bash
curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"

curl -X PATCH http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"fullName":"Budi Santoso Jr"}'
```

---

## Katalog

### GET /products 🔓

Query parameter:

| Param | Default | Keterangan |
|---|---|---|
| `q` | – | cari di `name` dan `sku` (case-insensitive, substring) |
| `category` | – | slug kategori, mis. `kopi-teh` |
| `page` | `0` | mulai dari 0 |
| `size` | `12` | maks 60 |
| `sort` | `createdAt,desc` | field yang diizinkan: `createdAt`, `name`, `price` |

```bash
curl "http://localhost:8080/api/v1/products?q=kopi&category=kopi-teh&size=4&sort=price,asc"
```

`200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "sku": "KOP-001",
      "slug": "kopi-gayo-arabica-250g",
      "name": "Kopi Gayo Arabica 250g",
      "priceCents": 8500000,
      "currency": "IDR",
      "categoryName": "Kopi & Teh",
      "categorySlug": "kopi-teh",
      "imageUrl": "https://picsum.photos/seed/kopi-gayo/600/600",
      "inStock": true
    }
  ],
  "page": 0,
  "size": 4,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

> `sort` memakai **whitelist**. Meneruskan input user langsung ke `Sort.by()` adalah cara
> umum membocorkan struktur database dan memicu error 500.

### GET /products/{slug} 🔓

```bash
curl http://localhost:8080/api/v1/products/kopi-gayo-arabica-250g
```

`200 OK` — sama seperti ringkasan di atas, plus:

```json
{
  "description": "Kopi single origin dari dataran tinggi Gayo...",
  "stockQuantity": 48,
  "createdAt": "2026-10-05T10:00:00Z"
}
```

Respons ini di-cache 2 menit di dalam product-service (Caffeine).

### POST /products 👑 ADMIN

```bash
TOKEN_ADMIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@shopflow.dev","password":"admin12345"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")

curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H 'Content-Type: application/json' \
  -d '{"sku":"KOP-003","name":"Kopi Aceh Premium","description":"...","priceCents":9900000,"categorySlug":"kopi-teh","imageUrl":null,"stockQuantity":25}'
```

`slug` dibuat otomatis dari `name`. Kalau sudah ada, ditambah suffix `-2`, `-3`, dst.

### PATCH /products/{id}/stock 👑 ADMIN

```bash
curl -X PATCH http://localhost:8080/api/v1/products/1/stock \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H 'Content-Type: application/json' \
  -d '{"delta":-3,"reason":"barang rusak"}'
```

`delta` positif = stok masuk, negatif = stok keluar. Tidak boleh 0.
Kalau hasilnya negatif → `409 Conflict`.

### GET /categories 🔓

```json
[
  { "id": 1, "slug": "kopi-teh", "name": "Kopi & Teh" }
]
```

---

## Keranjang

Semua endpoint keranjang butuh token. Keranjang disimpan di Redis dengan key
`shopflow:cart:{userId}` dan TTL 30 hari.

### GET /cart 🔒

```json
{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "sku": "KOP-001",
      "slug": "kopi-gayo-arabica-250g",
      "name": "Kopi Gayo Arabica 250g",
      "imageUrl": "https://picsum.photos/seed/kopi-gayo/600/600",
      "unitPriceCents": 8500000,
      "quantity": 2,
      "lineTotalCents": 17000000
    }
  ],
  "subtotalCents": 17000000,
  "itemCount": 2,
  "currency": "IDR",
  "updatedAt": "2026-10-05T10:01:00Z"
}
```

### POST /cart/items 🔒

```bash
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
```

`201 Created` — mengembalikan keranjang terbaru.

**Kalau produk sudah ada di keranjang, jumlahnya ditumpuk** (bukan diganti).
Maksimum 20 unit per produk; melebihi itu → `400`.

### PATCH /cart/items/{productId} 🔒

```bash
curl -X PATCH http://localhost:8080/api/v1/cart/items/1 \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"quantity":5}'
```

`quantity: 0` berarti **hapus item** (konvensi umum, menghemat satu endpoint).

Harga dan nama di keranjang disegarkan dari katalog setiap kali diubah — keranjang
tidak boleh menyimpan harga basi. Harga final tetap ditentukan order-service saat checkout.

### DELETE /cart/items/{productId} 🔒 · DELETE /cart 🔒

`204 No Content`

---

## Pesanan

### POST /orders 🔒 — Checkout

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
    "shippingAddress": {
      "recipientName": "Budi Santoso",
      "phone": "08123456789",
      "line1": "Jl. Merdeka No. 10",
      "line2": "RT 02 / RW 05",
      "city": "Bandung",
      "postalCode": "40115",
      "country": "Indonesia"
    },
    "notes": "Tolong dibungkus rapi"
  }'
```

`201 Created`

```json
{
  "orderNumber": "ORD-20261005-7F3A91",
  "status": "PENDING_PAYMENT",
  "totalCents": 17000000,
  "currency": "IDR",
  "items": [
    { "productId": 1, "sku": "KOP-001", "name": "Kopi Gayo Arabica 250g", "imageUrl": "...",
      "unitPriceCents": 8500000, "quantity": 2, "lineTotalCents": 17000000 }
  ],
  "shippingAddress": { "recipientName": "Budi Santoso", "...": "..." },
  "notes": "Tolong dibungkus rapi",
  "createdAt": "2026-10-05T10:02:00Z",
  "updatedAt": "2026-10-05T10:02:00Z"
}
```

> **Penting: body checkout TIDAK berisi daftar item.** Pesanan dibangun dari keranjang
> milik user. Kalau klien boleh mengirim item + harga sendiri, klien bisa memalsukan
> harga — kesalahan keamanan yang sangat umum.

Yang terjadi setelah respons dikembalikan: stok ditahan, keranjang dikosongkan,
`order.created` diterbitkan, dan pembayaran diproses **secara asinkron**.
Status akan berubah dalam hitungan detik. Lihat [04-events.md](04-events.md).

### Status pesanan

| Status | Arti |
|---|---|
| `PENDING_PAYMENT` | dibuat, menunggu hasil pembayaran |
| `PAID` | pembayaran berhasil |
| `PAYMENT_FAILED` | pembayaran gagal; stok sudah dilepas kembali |
| `CANCELLED` | dibatalkan user sebelum dibayar; stok dilepas |
| `FULFILLED` | selesai dikirim (belum ada otomatisasi di base ini) |

### GET /orders 🔒 · GET /orders/{orderNumber} 🔒

```bash
curl "http://localhost:8080/api/v1/orders?page=0&size=10" -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/v1/orders/ORD-20261005-7F3A91" -H "Authorization: Bearer $TOKEN"
```

Pesanan milik user lain → `403 Forbidden`.

### POST /orders/{orderNumber}/cancel 🔒

Hanya untuk status `PENDING_PAYMENT`. Selain itu → `409 Conflict`.

---

## Pembayaran

### GET /payments/{orderNumber} 🔒

```json
{
  "paymentId": "PAY-7F3A91C2",
  "orderNumber": "ORD-20261005-7F3A91",
  "status": "SUCCEEDED",
  "method": "SIMULATED_CARD",
  "amountCents": 17000000,
  "currency": "IDR",
  "providerRef": "ch_9a8b7c6d5e4f3a2b",
  "createdAt": "2026-10-05T10:02:01Z"
}
```

Status: `PENDING` | `SUCCEEDED` | `FAILED` | `REFUNDED`.

Karena pemrosesan asinkron, mungkin ada jeda beberapa detik sebelum baris pembayaran
muncul. Kalau belum ada → `404`.

### GET /payments 🔒

Daftar pembayaran milik user yang login (berhalaman).

---

## Notifikasi

### GET /notifications/me 🔒

```json
[
  {
    "id": 2,
    "type": "PAYMENT_COMPLETED",
    "subject": "Pembayaran untuk ORD-20261005-7F3A91 berhasil",
    "body": "Pembayaran sebesar Rp 170.000 berhasil kami terima...",
    "orderNumber": "ORD-20261005-7F3A91",
    "channel": "EMAIL",
    "read": false,
    "createdAt": "2026-10-05T10:02:02Z"
  }
]
```

`type`: `ORDER_CREATED` | `ORDER_CANCELLED` | `PAYMENT_COMPLETED` | `PAYMENT_FAILED`.

### POST /notifications/{id}/read 🔒

`204 No Content`

---

## Endpoint internal (BUKAN untuk publik)

Endpoint di bawah ini **tidak dirutekan** oleh API Gateway dan dilindungi header
`X-Internal-Token`. Dipakai untuk komunikasi antar-service.

| Method | Path | Pemanggil |
|---|---|---|
| GET | `/api/v1/internal/products/{id}` | cart-service, order-service |
| POST | `/api/v1/internal/products/{id}/stock/reserve` | order-service |
| POST | `/api/v1/internal/products/{id}/stock/release` | order-service |
| GET | `/api/v1/internal/cart/{userId}` | order-service |
| DELETE | `/api/v1/internal/cart/{userId}` | order-service |

Contoh memanggil dari dalam jaringan Docker:

```bash
docker compose exec api-gateway sh -c '
  wget -qO- --header="X-Internal-Token: shopflow-internal-dev-token" \
    http://product-service:8082/api/v1/internal/products/1'
```

---

## Dokumentasi interaktif (Swagger)

Setiap service backend memakai springdoc-openapi:

- `http://localhost:8081/swagger-ui.html` — user-service
- `http://localhost:8082/swagger-ui.html` — product-service
- `http://localhost:8084/swagger-ui.html` — order-service
- `http://localhost:8085/swagger-ui.html` — payment-service
- `http://localhost:8086/swagger-ui.html` — notification-service
- `http://localhost:8083/swagger-ui.html` — cart-service

**Latihan:** aggregate semua OpenAPI menjadi satu UI di Gateway (springdoc + Gateway
`/v3/api-docs/{service}`). Ini pola yang umum dipakai supaya dokumentasi tidak tersebar.

---

## Konvensi penamaan

| Konvensi | Aturan |
|---|---|
| Path | jamak, kebab-case, selalu berawalan `/api/v1` |
| Versi | di URL (`/v1`), bukan di header |
| ID | `Long` untuk entitas internal, string ber-prefix untuk yang dibaca manusia (`ORD-…`, `PAY-…`) |
| Waktu | ISO-8601 UTC (`2026-10-05T10:00:00Z`) |
| Uang | bilangan bulat satuan minor + field `currency` terpisah |
| Enum | `SCREAMING_SNAKE_CASE` |
| DTO | `XxxRequest` / `XxxResponse`, tidak pernah mengembalikan entity JPA |
