/**
 * Root layout — Server Component.
 *
 * It wraps every route with the storefront chrome (Header + Footer) and declares the
 * metadata template. Note what it does NOT do: read cookies. That is deliberate — a layout
 * that touches `cookies()` would opt every page out of static rendering, and the catalog
 * relies on ISR. Session-dependent chrome is isolated in small client islands
 * (`HeaderSession`, `CartBadge`).
 */

import type { Metadata, Viewport } from "next";

import { SITE_URL, STORE } from "@/lib/config";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import "./globals.css";

export const metadata: Metadata = {
  // Lets Next.js turn relative URLs in metadata into absolute ones.
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${STORE.name} — ${STORE.tagline}`,
    template: `%s · ${STORE.name}`,
  },
  description: STORE.description,
  applicationName: STORE.name,
  keywords: ["ecommerce", "next.js", "belanja online", "shopflow"],
  openGraph: {
    type: "website",
    locale: "id_ID",
    siteName: STORE.name,
    title: `${STORE.name} — ${STORE.tagline}`,
    description: STORE.description,
    url: SITE_URL,
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f7f7f8" },
    { media: "(prefers-color-scheme: dark)", color: "#09090b" },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="id" className="h-full">
      <body className="flex min-h-full flex-col bg-background text-foreground">
        {/* Accessible skip link: first thing a keyboard user reaches. */}
        <a
          href="#konten-utama"
          className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-brand-foreground"
        >
          Lompat ke konten utama
        </a>

        <Header />

        <main id="konten-utama" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-10">
          {children}
        </main>

        <Footer />
      </body>
    </html>
  );
}
