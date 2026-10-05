/**
 * `/products` — the catalog. Server Component, driven entirely by `searchParams`.
 *
 * ── SSR + ISR, EXPLAINED ─────────────────────────────────────────────────────
 * `export const revalidate = 60` tells Next.js: "the *data* this page fetches may be cached
 * for 60 seconds". The underlying `fetch` calls in `src/lib/api-client.ts` carry
 * `next: { revalidate: 60 }`, so a burst of visitors costs the gateway exactly one request
 * per minute per filter combination.
 *
 * Reading `searchParams` makes the *route* dynamic (each query string is a different page),
 * but the upstream calls are still served from the Next.js data cache. That combination —
 * cached data, dynamic rendering — is what people usually mean by ISR in the App Router.
 *
 * ── WHY THE FILTERS ARE A NATIVE FORM ────────────────────────────────────────
 * Search, category and sort are just query parameters. A plain `<form method="get">`
 * produces `/products?q=...&category=...`, and this Server Component re-renders from
 * `searchParams`. No client JavaScript, and every filter combination is a shareable URL.
 *
 * ── WHY `<Suspense>` LIVES HERE AND NOT IN `loading.tsx` ─────────────────────
 * See `src/components/CatalogSkeleton.tsx`: a segment-level `loading.tsx` would also wrap
 * `/products/[slug]` and turn its `notFound()` into a soft 404. An in-page boundary gives
 * the same instant skeleton with none of that collateral damage.
 */

import { Suspense } from "react";
import type { Metadata } from "next";

import { fetchCategories, fetchProducts } from "@/lib/catalog";
import { DEFAULT_SORT, DEMO_SORT_OPTIONS } from "@/lib/demo-data";
import { firstParam, type ProductQuery, type SearchParams } from "@/lib/types";
import { BackendOfflineNotice } from "@/components/BackendOfflineNotice";
import { CatalogFilters } from "@/components/CatalogFilters";
import { CatalogSkeleton } from "@/components/CatalogSkeleton";
import { CategoryGrid } from "@/components/CategoryGrid";
import { Pagination } from "@/components/Pagination";
import { ProductGrid } from "@/components/ProductGrid";

/** ISR window for the catalog (seconds). */
export const revalidate = 60;

const DEFAULT_PAGE_SIZE = 12;

export const metadata: Metadata = {
  title: "Katalog Produk",
  description:
    "Jelajahi seluruh katalog ShopFlow: cari produk, filter berdasarkan kategori, dan urutkan sesuai kebutuhan Anda.",
  alternates: { canonical: "/products" },
};

export default async function ProductsPage({ searchParams }: PageProps<"/products">) {
  // Awaiting searchParams is cheap (no I/O) — the slow part is the gateway call, which the
  // Suspense boundary below is allowed to stream in.
  const params: SearchParams = await searchParams;

  const q = firstParam(params.q);
  const category = firstParam(params.category);
  const sort = firstParam(params.sort) || DEFAULT_SORT;
  const pageParam = Number.parseInt(firstParam(params.page) || "0", 10);
  const sizeParam = Number.parseInt(firstParam(params.size) || String(DEFAULT_PAGE_SIZE), 10);
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 0;
  const size = Number.isFinite(sizeParam) && sizeParam > 0 && sizeParam <= 48 ? sizeParam : DEFAULT_PAGE_SIZE;

  const query: ProductQuery = { page, size, q, category, sort };

  return (
    <Suspense fallback={<CatalogSkeleton />}>
      <CatalogResults query={query} q={q} category={category} sort={sort} size={size} />
    </Suspense>
  );
}

/**
 * The slow half: it awaits the gateway and streams into the shell above.
 * Being a separate async component is what lets `<Suspense>` do its job.
 */
async function CatalogResults({
  query,
  q,
  category,
  sort,
  size,
}: {
  query: ProductQuery;
  q: string;
  category: string;
  sort: string;
  size: number;
}) {
  // Categories power the filter dropdown and the chip row; both are cheap and cached.
  const [categories, products] = await Promise.all([fetchCategories(), fetchProducts(query)]);

  const pageData = products.data;
  const activeCategoryName = categories.data.find((item) => item.slug === category)?.name;

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Katalog produk</h1>
        <p className="text-sm text-foreground/65">
          {pageData.totalElements} produk ditemukan
          {q ? (
            <>
              {" "}
              untuk pencarian <span className="font-medium text-foreground">“{q}”</span>
            </>
          ) : null}
          {activeCategoryName ? (
            <>
              {" "}
              di kategori <span className="font-medium text-foreground">{activeCategoryName}</span>
            </>
          ) : null}
          .
        </p>
      </header>

      {products.demo ? <BackendOfflineNotice /> : null}

      <CategoryGrid categories={categories.data} activeSlug={category || undefined} />

      <CatalogFilters
        categories={categories.data}
        q={q}
        category={category}
        sort={sort}
        size={size}
        sortOptions={DEMO_SORT_OPTIONS}
      />

      <ProductGrid
        products={pageData.content}
        emptyTitle="Tidak ada produk yang cocok"
        emptyDescription="Coba kata kunci lain, atau reset filter untuk melihat seluruh katalog."
        emptyAction={{ href: "/products", label: "Reset filter" }}
      />

      <Pagination
        page={pageData.page}
        totalPages={pageData.totalPages}
        basePath="/products"
        params={{ q, category, sort, size: String(size) }}
      />

      <p className="text-center text-xs text-foreground/50">
        Halaman {pageData.page + 1} dari {Math.max(pageData.totalPages, 1)} · data diperbarui otomatis setiap 60 detik
        (ISR).
      </p>
    </div>
  );
}
