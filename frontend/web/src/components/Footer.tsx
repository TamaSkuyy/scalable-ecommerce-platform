/**
 * Footer.tsx — Server Component. Static content, so it costs nothing at runtime.
 */

import Link from "next/link";

import { API_BASE_URL, STORE } from "@/lib/config";

const COLUMNS = [
  {
    title: "Belanja",
    links: [
      { href: "/products", label: "Semua produk" },
      { href: "/products?sort=price,asc", label: "Harga terendah" },
      { href: "/products?category=elektronik", label: "Elektronik" },
      { href: "/products?category=fashion", label: "Fashion" },
    ],
  },
  {
    title: "Akun",
    links: [
      { href: "/login", label: "Masuk" },
      { href: "/register", label: "Daftar" },
      { href: "/orders", label: "Riwayat pesanan" },
      { href: "/account", label: "Profil & notifikasi" },
    ],
  },
  {
    title: "Bantuan",
    links: [
      { href: "/cart", label: "Keranjang" },
      { href: "/checkout", label: "Checkout" },
      { href: "/products", label: "Katalog" },
    ],
  },
];

export function Footer() {
  return (
    <footer className="mt-16 border-t border-border-subtle bg-surface">
      <div className="mx-auto grid w-full max-w-6xl gap-8 px-4 py-12 sm:px-6 md:grid-cols-[1.4fr_repeat(3,1fr)]">
        <div className="flex flex-col gap-3">
          <p className="text-base font-semibold tracking-tight">{STORE.name}</p>
          <p className="max-w-xs text-sm text-foreground/65">{STORE.tagline}</p>
          <p className="text-xs text-foreground/50">
            API gateway: <code className="rounded bg-surface-muted px-1.5 py-0.5">{API_BASE_URL}</code>
          </p>
        </div>

        {COLUMNS.map((column) => (
          <div key={column.title} className="flex flex-col gap-3">
            <p className="text-sm font-semibold">{column.title}</p>
            <ul className="flex flex-col gap-2">
              {column.links.map((link) => (
                <li key={`${column.title}-${link.href}-${link.label}`}>
                  <Link
                    href={link.href}
                    className="rounded text-sm text-foreground/65 transition-colors hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <div className="border-t border-border-subtle">
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-2 px-4 py-5 text-xs text-foreground/55 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p>
            © {new Date().getFullYear()} {STORE.name}. Proyek belajar Next.js App Router.
          </p>
          <p>
            Butuh bantuan?{" "}
            <a className="font-medium text-foreground underline underline-offset-2" href={`mailto:${STORE.supportEmail}`}>
              {STORE.supportEmail}
            </a>
          </p>
        </div>
      </div>
    </footer>
  );
}
