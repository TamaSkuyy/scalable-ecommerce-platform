# ShopFlow — Web Storefront (Next.js)

Aplikasi Next.js 16 (App Router) untuk storefront ShopFlow.
Dokumentasi lengkap ada di **[../README.md](../README.md)** (quick-start, arsitektur BFF, dan Docker).

Ringkasan perintah:

```bash
pnpm install     # pasang dependensi
pnpm dev         # server pengembangan di http://localhost:3000
pnpm build       # build produksi (wajib hijau sebelum commit)
pnpm start       # jalankan hasil build
pnpm lint        # ESLint
pnpm typecheck   # tsc --noEmit
```

Struktur singkat:

- `src/app/` — route App Router (halaman, `loading.tsx`, `error.tsx`, `api/auth/*` BFF)
- `src/components/` — komponen UI kecil yang dapat dipakai ulang
- `src/lib/` — tipe DTO, klien API server & client, helper format, data demo
- `src/middleware.ts` — proxy `/api/v1/*` ke gateway + proteksi route privat
