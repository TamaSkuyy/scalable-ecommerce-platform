/**
 * `/cart` — Server Component *shell* around a Client Component body.
 *
 * The page itself is server-rendered (so metadata and layout are static), while `CartView`
 * does all the interactive work in the browser: it calls `/api/v1/cart` — a relative path —
 * and the middleware turns that into an authenticated gateway request.
 *
 * The whole route is also protected by `src/middleware.ts`, which redirects anonymous
 * visitors to `/login?redirect=/cart` before this page ever renders.
 */

import type { Metadata } from "next";

import { CartView } from "@/components/CartView";

export const metadata: Metadata = {
  title: "Keranjang",
  description: "Tinjau produk di keranjang Anda, ubah jumlah, lalu lanjutkan ke checkout.",
  robots: { index: false, follow: false },
};

export default function CartPage() {
  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Keranjang belanja</h1>
        <p className="text-sm text-foreground/65">
          Perubahan jumlah tersimpan otomatis di server setiap kali Anda menekan tombol.
        </p>
      </header>

      <CartView />
    </div>
  );
}
