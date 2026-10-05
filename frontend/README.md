# ShopFlow — Frontend

Storefront e-commerce **ShopFlow** dibangun dengan **Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS v4**.
Proyek ini adalah bagian frontend dari monorepo `scalable-ecommerce-platform`; ia berbicara ke **API gateway**
(Spring Boot) lewat pola **Backend-for-Frontend (BFF)** dengan cookie `httpOnly`.

> Seluruh teks antarmuka menggunakan Bahasa Indonesia. Kode, komentar, dan nama identifier menggunakan Bahasa Inggris.

---

## 1. Quick start

```bash
cd frontend/web

# 1. Salin konfigurasi environment
cp .env.example .env.local

# 2. Pasang dependensi
pnpm install

# 3. Jalankan server pengembangan
pnpm dev          # http://localhost:3000
```

Perintah lain:

| Perintah          | Kegunaan                                                       |
| ----------------- | -------------------------------------------------------------- |
| `pnpm dev`        | Server pengembangan (hot reload)                                |
| `pnpm build`      | Build produksi — **wajib hijau**                                |
| `pnpm start`      | Menjalankan hasil build                                         |
| `pnpm lint`       | ESLint (`eslint-config-next`)                                   |
| `pnpm typecheck`  | `tsc --noEmit`                                                  |

### Backend belum berjalan? Tetap aman.

Halaman katalog **tidak akan error** kalau API gateway belum hidup. Semua pembacaan katalog publik
melewati `src/lib/catalog.ts`, yang menangkap kegagalan koneksi dan menggantinya dengan data contoh dari
`src/lib/demo-data.ts`, disertai banner **"Backend belum berjalan"**.

Yang **tidak** memakai fallback adalah operasi tulis (tambah ke keranjang, checkout, batalkan pesanan) —
kalau gateway mati, pengguna harus melihat pesan error, bukan seolah-olah pesanannya berhasil.

---

## 2. Variabel environment

| Variabel                 | Default                 | Dipakai di            | Keterangan                                                                                       |
| ------------------------ | ----------------------- | --------------------- | ------------------------------------------------------------------------------------------------ |
| `API_BASE_URL`           | `http://localhost:8080` | **Server saja**       | Alamat API gateway. Tanpa prefix `NEXT_PUBLIC_`, jadi tidak pernah ikut ke bundle browser.         |
| `NEXT_PUBLIC_SITE_URL`   | `http://localhost:3000` | Server + browser      | Origin publik storefront, untuk `metadataBase`, canonical URL, dan Open Graph.                     |

Di dalam Docker Compose, host gateway adalah `http://api-gateway:8080` (nama service), bukan `localhost`.

> `API_BASE_URL` juga dibaca oleh `src/middleware.ts`, yang di-bundle saat **build**. Karena itu nilainya
> diberikan sebagai build arg di Dockerfile, dan tetap bisa ditimpa lewat env runtime.

---

## 3. Arsitektur: pola BFF + cookie httpOnly

**Aturan utamanya: JWT tidak boleh pernah bisa dibaca oleh JavaScript di browser.**

```
┌─────────────┐   1. POST /api/auth/login      ┌──────────────────────┐
│   Browser   │ ─────────────────────────────▶ │  Next.js (BFF)       │
│ (React)     │      { email, password }       │  route handler       │
│             │                                │  /api/auth/login     │
│             │                                └──────────┬───────────┘
│             │                                           │ 2. POST /api/v1/auth/login
│             │                                           ▼
│             │                                ┌──────────────────────┐
│             │                                │    API Gateway       │
│             │                                │  { accessToken, ... }│
│             │   3. Set-Cookie:               └──────────────────────┘
│             │      shopflow_token=<JWT>                 │
│             │      HttpOnly; SameSite=Lax; Path=/       │
│             │ ◀──────────────────────────────────────────┘
│             │      body: { user }        ← token TIDAK ada di body
└─────────────┘
```

Setelah login, komponen client cukup memanggil **path relatif** `/api/v1/...`:

```
Browser ──fetch("/api/v1/cart")──▶ src/middleware.ts ──▶ http://api-gateway:8080/api/v1/cart
                                        │  menyisipkan "Authorization: Bearer <cookie>"
```

### Di mana apa berada

| Berkas                          | Peran                                                                                          |
| ------------------------------- | ---------------------------------------------------------------------------------------------- |
| `src/middleware.ts`             | Proxy `/api/v1/:path*` → gateway + suntik Bearer token; redirect route privat ke `/login`.       |
| `src/app/api/auth/login/route.ts`    | Login: panggil gateway, simpan `accessToken` ke cookie `shopflow_token`, balas `{ user }` saja.  |
| `src/app/api/auth/register/route.ts` | Registrasi + auto-login (satu round trip).                                                       |
| `src/app/api/auth/logout/route.ts`   | Hapus cookie (204).                                                                              |
| `src/app/api/auth/session/route.ts`  | `{ user }` untuk komponen client; `{ user: null, offline: true }` bila gateway mati.             |
| `src/lib/api-client.ts`         | **Server-only.** Baca cookie via `next/headers`, panggil gateway dengan Bearer.                  |
| `src/lib/api.ts` + `src/lib/http.ts` | Klien untuk Client Component: `fetch` ke path relatif, melempar `ApiError`.                      |
| `src/lib/types.ts`              | Semua DTO sesuai kontrak backend.                                                               |

### Opsi cookie

```ts
{
  name: "shopflow_token",
  httpOnly: true,                              // JavaScript tidak bisa membacanya
  sameSite: "lax",                             // mitigasi CSRF
  secure: process.env.NODE_ENV === "production",
  path: "/",
  maxAge: expiresInSeconds,                    // seumur JWT
}
```

### Route yang dilindungi

`/account`, `/cart`, `/checkout`, `/orders` — pengunjung tanpa cookie diarahkan ke
`/login?redirect=<path-asal>`.

---

## 4. Server vs Client Components

| Halaman / komponen                | Jenis            | Alasan                                                                 |
| --------------------------------- | ---------------- | ---------------------------------------------------------------------- |
| `/`, `/products`, `/products/[slug]` | Server        | Membaca gateway langsung, bisa di-cache (ISR).                          |
| `/orders`, `/orders/[orderNumber]`, `/account` | Server | Data privat dibaca dengan cookie, tidak pernah menyentuh browser.      |
| `Header`, `Footer`, `ProductCard`, `ProductGrid` | Server | Markup murni, nol JavaScript.                                        |
| `HeaderSession`, `CartBadge`      | Client (pulau kecil) | Butuh `useEffect` + reaksi terhadap perubahan keranjang.           |
| `CartView`, `CheckoutForm`, `AddToCartButton`, `LoginForm`, `RegisterForm`, `ProfileForm`, `OrderCancelButton` | Client | Interaksi, mutation, dan state lokal. |

Root layout **sengaja tidak membaca cookie**. Kalau ia melakukannya, semua halaman menjadi dinamis dan ISR
di katalog kehilangan gunanya. Bagian yang bergantung sesi diisolasi ke `HeaderSession` dan `CartBadge`.

---

## 5. Caching: ISR, `no-store`, dan Suspense

- **Katalog publik** → `fetch(..., { next: { revalidate: 60 } })`. Satu permintaan ke gateway per menit per
  kombinasi filter, berapa pun jumlah pengunjung.
- **Data privat** (cart, orders, notifications, users/me) → `{ cache: "no-store" }`. Tidak pernah di-cache,
  tidak pernah tercampur antar pengguna.
- `/products` memakai `export const revalidate = 60`; karena membaca `searchParams` route-nya dirender
  dinamis per permintaan, tetapi **datanya** tetap dilayani dari cache 60 detik.

### Catatan: kenapa skeleton katalog tidak ada di `app/products/loading.tsx`

`loading.tsx` membuat Suspense boundary untuk **satu segmen route**, sehingga ia juga membungkus
`/products/[slug]`. Boundary yang sudah ter-flush lebih dulu mengunci status HTTP ke `200` sebelum halaman
memutuskan apa yang dirender — akibatnya `notFound()` pada slug produk yang tidak ada menghasilkan
**soft 404** (status 200, isi "tidak ditemukan") yang bisa diindeks mesin pencari.

Karena itu skeleton katalog diletakkan **di dalam** `app/products/page.tsx` lewat `<Suspense>`
(lihat `src/components/CatalogSkeleton.tsx`). Hasilnya sama instan, tetapi slug yang tidak ada tetap
menjawab `404` yang benar. `loading.tsx` tetap dipakai di route yang tidak punya anak dan tidak pernah
memanggil `notFound()`: `/orders`, `/account`, `/checkout`.

---

## 6. Build produksi & Docker

```bash
pnpm build       # menghasilkan .next/standalone (lihat output: "standalone" di next.config.ts)
pnpm start
```

### Docker

```bash
cd frontend/web

docker build \
  --build-arg API_BASE_URL=http://api-gateway:8080 \
  --build-arg NEXT_PUBLIC_SITE_URL=http://localhost:3000 \
  -t shopflow-web .

docker run --rm -p 3000:3000 \
  -e API_BASE_URL=http://api-gateway:8080 \
  shopflow-web
```

Di dalam jaringan Docker Compose, host gateway adalah **`http://api-gateway:8080`** (nama service),
bukan `localhost` — `localhost` di dalam container menunjuk ke container itu sendiri.

Dockerfile-nya multi-stage:

1. **deps** — hanya `package.json` + lockfile, jadi layer ini dipakai ulang selama dependensi tidak berubah.
2. **builder** — kompilasi aplikasi, menghasilkan `.next/standalone`.
3. **runner** — image ramping berisi output standalone saja, berjalan sebagai user non-root (`nextjs`),
   `EXPOSE 3000`, dengan `HEALTHCHECK`.

---

## 7. Struktur berkas

```
frontend/
├── README.md                    ← dokumen ini
└── web/
    ├── Dockerfile               ← multi-stage: deps → builder → runner
    ├── .dockerignore
    ├── .env.example
    ├── next.config.ts           ← output: "standalone"
    ├── public/images/           ← placeholder SVG untuk mode demo
    └── src/
        ├── middleware.ts        ← proxy /api/v1 + proteksi route
        ├── app/
        │   ├── layout.tsx, page.tsx, error.tsx, not-found.tsx, globals.css
        │   ├── api/auth/{login,register,logout,session}/route.ts
        │   ├── products/{page.tsx, [slug]/{page,not-found}.tsx}
        │   ├── cart/, checkout/, orders/, account/, login/, register/
        ├── components/          ← UI, Header/Footer, ProductCard, CartView, form, dsb.
        └── lib/                 ← types, api-client (server), api (client), catalog, demo-data, format
```

---

## 8. Konsep Next.js yang didemonstrasikan

- **App Router** dengan `src/` dan alias impor `@/*`.
- **Server vs Client Components** dan kapan masing-masing dipakai.
- **BFF pattern** — route handler Next.js sebagai lapisan perantara yang memegang JWT di cookie `httpOnly`.
- **Middleware** untuk rewrite/proxy dan proteksi route.
- **ISR** (`revalidate`) di katalog publik, `no-store` untuk data privat.
- **Suspense / streaming** lewat `<Suspense>` dan `loading.tsx`.
- **`generateMetadata`** untuk SEO halaman detail produk.
- **Error boundary** (`error.tsx`), **`not-found.tsx`**, dan **`EmptyState`** sebagai UI keadaan kosong.
- **URL sebagai state** — pencarian, filter, sort, dan paginasi semuanya query parameter; form filternya
  `<form method="get">` tanpa JavaScript.
