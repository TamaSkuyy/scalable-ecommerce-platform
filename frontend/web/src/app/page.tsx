/**
 * `/` — landing page. Server Component.
 *
 * DATA FETCHING: `fetchCategories()` / `fetchProducts()` run on the server and hit the
 * gateway directly (`src/lib/api-client.ts`). Because the requests carry
 * `next: { revalidate: 60 }`, the rendered result can be cached and reused for a minute —
 * that is ISR (Incremental Static Regeneration) applied to a dynamic segment.
 *
 * If the gateway is down, `src/lib/catalog.ts` swaps in the demo data and we render a
 * notice instead of an error page.
 */

import Link from "next/link";

import { fetchCategories, fetchFeaturedProducts } from "@/lib/catalog";
import { Button } from "@/components/ui/Button";
import { BackendOfflineNotice } from "@/components/BackendOfflineNotice";
import { CategoryGrid } from "@/components/CategoryGrid";
import { Hero } from "@/components/Hero";
import { ProductGrid } from "@/components/ProductGrid";
import { ArrowRightIcon } from "@/components/icons";

/** ISR window for this route (seconds). */
export const revalidate = 60;

export default async function HomePage() {
  // Fetch in parallel: two independent requests should never be serialised.
  const [categories, featured] = await Promise.all([fetchCategories(), fetchFeaturedProducts(8)]);

  const usingDemo = categories.demo || featured.demo;

  return (
    <div className="flex flex-col gap-10">
      <Hero />

      {usingDemo ? <BackendOfflineNotice /> : null}

      <section className="flex flex-col gap-4">
        <header className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold tracking-tight">Kategori pilihan</h2>
            <p className="mt-1 text-sm text-foreground/65">Temukan produk berdasarkan kategori favorit Anda.</p>
          </div>
          <Link href="/products" className="text-sm font-medium text-brand underline underline-offset-2">
            Lihat semua produk
          </Link>
        </header>
        <CategoryGrid categories={categories.data} />
      </section>

      <section className="flex flex-col gap-4">
        <header className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold tracking-tight">Produk unggulan</h2>
            <p className="mt-1 text-sm text-foreground/65">Kurasi produk terbaru dari katalog kami.</p>
          </div>
          <Link href="/products">
            <Button variant="outline" size="sm">
              Semua katalog
              <ArrowRightIcon className="h-4 w-4" />
            </Button>
          </Link>
        </header>

        <ProductGrid
          products={featured.data}
          emptyTitle="Katalog masih kosong"
          emptyDescription="Belum ada produk yang bisa ditampilkan."
        />
      </section>
    </div>
  );
}
