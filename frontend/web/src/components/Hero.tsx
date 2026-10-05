/**
 * Hero.tsx — Landing page hero. Server Component: pure markup, zero JavaScript.
 */

import Link from "next/link";

import { STORE } from "@/lib/config";
import { Button } from "@/components/ui/Button";
import { ArrowRightIcon } from "@/components/icons";

const HIGHLIGHTS = [
  { title: "Kurir same-day", description: "Pesanan sebelum jam 15.00 dikirim hari yang sama." },
  { title: "Garansi 30 hari", description: "Tidak cocok? Kembalikan tanpa pertanyaan." },
  { title: "Pembayaran aman", description: "Diproses lewat payment service terpisah." },
];

export function Hero() {
  return (
    <section className="overflow-hidden rounded-2xl border border-border-subtle bg-surface">
      <div className="grid gap-8 p-6 sm:p-10 lg:grid-cols-[1.15fr_1fr] lg:items-center lg:p-12">
        <div className="flex flex-col gap-5">
          <span className="inline-flex w-fit items-center gap-2 rounded-full border border-brand/25 bg-brand-soft px-3 py-1 text-xs font-medium text-brand">
            Proyek belajar Next.js App Router
          </span>

          <h1 className="text-3xl font-bold leading-tight tracking-tight sm:text-4xl lg:text-5xl">
            {STORE.tagline}
          </h1>

          <p className="max-w-xl text-base text-foreground/70">
            {STORE.name} adalah storefront contoh yang lengkap: katalog dengan ISR, keranjang, checkout, riwayat
            pesanan, dan autentikasi JWT yang disimpan di cookie <code className="rounded bg-surface-muted px-1.5 py-0.5 text-sm">httpOnly</code> —
            jadi token tidak pernah bisa dibaca JavaScript di browser.
          </p>

          <div className="flex flex-wrap items-center gap-3">
            <Link href="/products">
              <Button size="lg">
                Jelajahi katalog
                <ArrowRightIcon className="h-4 w-4" />
              </Button>
            </Link>
            <Link href="/register">
              <Button size="lg" variant="outline">
                Buat akun
              </Button>
            </Link>
          </div>
        </div>

        <ul className="grid gap-3 sm:grid-cols-3 lg:grid-cols-1">
          {HIGHLIGHTS.map((item) => (
            <li key={item.title} className="rounded-xl border border-border-subtle bg-surface-muted/60 p-4">
              <p className="text-sm font-semibold">{item.title}</p>
              <p className="mt-1 text-sm text-foreground/65">{item.description}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
