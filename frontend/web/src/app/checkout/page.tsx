/**
 * `/checkout` — Server Component shell.
 *
 * The page reads the session on the server (it needs the shopper's name to prefill the
 * recipient field) which is a legitimate use of `cookies()`: `/checkout` is private and
 * never cacheable anyway.
 *
 * `getCurrentUser()` returns `null` both for anonymous visitors and when the gateway is
 * unreachable, so the page degrades to an empty prefill instead of erroring.
 */

import type { Metadata } from "next";

import { getCurrentUser } from "@/lib/api-client";
import { CheckoutForm } from "@/components/CheckoutForm";
import { Callout } from "@/components/ui/Callout";

export const metadata: Metadata = {
  title: "Checkout",
  description: "Isi alamat pengiriman dan buat pesanan Anda.",
  robots: { index: false, follow: false },
};

export default async function CheckoutPage() {
  // Server-side session read — the JWT stays in the httpOnly cookie, we only get the user.
  const user = await getCurrentUser().catch(() => null);

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Checkout</h1>
        <p className="text-sm text-foreground/65">
          Lengkapi alamat pengiriman. Pesanan akan dibuat lewat <code className="rounded bg-surface-muted px-1.5 py-0.5">POST /api/v1/orders</code> dan
          status pembayarannya langsung ditampilkan.
        </p>
      </header>

      {user ? null : (
        <Callout tone="warning" title="Profil tidak termuat">
          Kami tidak dapat membaca profil Anda (gateway mungkin belum berjalan). Anda tetap bisa mengisi alamat secara
          manual.
        </Callout>
      )}

      <CheckoutForm defaultRecipientName={user?.fullName ?? ""} />
    </div>
  );
}
