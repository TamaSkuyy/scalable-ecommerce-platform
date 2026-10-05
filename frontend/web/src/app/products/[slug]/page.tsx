/**
 * `/products/[slug]` — product detail. Server Component.
 *
 * ── generateMetadata ─────────────────────────────────────────────────────────
 * The product name/description must live in the HTML `<head>` for SEO and for link
 * previews, and the server already has the data. `generateMetadata` runs on the server
 * *before* the page renders, so both share the exact same cached fetch.
 *
 * ── SERVER / CLIENT SPLIT ────────────────────────────────────────────────────
 * The whole page is a Server Component (it can therefore be cached), except for the
 * "Tambah ke Keranjang" control, which needs `onClick` and `useState`.
 */

import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { fetchProductDetail } from "@/lib/catalog";
import { formatDate } from "@/lib/format";
import { ApiError } from "@/lib/api-error";
import { AddToCartButton } from "@/components/AddToCartButton";
import { BackendOfflineNotice } from "@/components/BackendOfflineNotice";
import { Badge } from "@/components/ui/Badge";
import { Card, CardContent, DetailRow } from "@/components/ui/Card";
import { Price } from "@/components/ui/Price";
import { ProductImage } from "@/components/ProductImage";
import { ProductGrid } from "@/components/ProductGrid";
import { fetchProducts } from "@/lib/catalog";
import { ArrowRightIcon, PackageIcon } from "@/components/icons";

export const revalidate = 60;

interface RouteParams {
  slug: string;
}

/**
 * Pre-renders nothing at build time (the gateway may not be up), but the route is still
 * ISR-cached on first request. Returning `[]` is explicit about that intent.
 */
export function generateStaticParams(): Array<RouteParams> {
  return [];
}

export async function generateMetadata({ params }: PageProps<"/products/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  try {
    const { data } = await fetchProductDetail(slug);
    if (!data) return { title: "Produk tidak ditemukan" };
    return {
      title: data.name,
      description: data.description ?? `Beli ${data.name} di ShopFlow.`,
      alternates: { canonical: `/products/${data.slug}` },
      openGraph: {
        title: data.name,
        description: data.description ?? undefined,
        images: data.imageUrl ? [{ url: data.imageUrl, alt: data.name }] : undefined,
      },
    };
  } catch {
    // Never let a metadata failure break the page — fall back to a generic title.
    return { title: "Detail produk" };
  }
}

export default async function ProductDetailPage({ params }: PageProps<"/products/[slug]">) {
  const { slug } = await params;

  let result: Awaited<ReturnType<typeof fetchProductDetail>>;
  try {
    result = await fetchProductDetail(slug);
  } catch (error) {
    // A real 404 from a running gateway means "this product does not exist".
    if (error instanceof ApiError && error.isNotFound) notFound();
    throw error;
  }

  const product = result.data;
  if (!product) notFound();

  // "Produk lainnya": same category, excluding the current product.
  const relatedResult = await fetchProducts({ page: 0, size: 5, category: product.categorySlug, sort: "createdAt,desc" });
  const related = relatedResult.data.content.filter((item) => item.slug !== product.slug).slice(0, 4);

  const lowStock = product.stockQuantity > 0 && product.stockQuantity <= 5;

  return (
    <div className="flex flex-col gap-10">
      {result.demo ? <BackendOfflineNotice /> : null}

      {/* Breadcrumb: also good for SEO and for orientation on mobile. */}
      <nav aria-label="Remah roti" className="flex flex-wrap items-center gap-2 text-sm text-foreground/60">
        <Link href="/" className="rounded hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
          Beranda
        </Link>
        <span aria-hidden>/</span>
        <Link href="/products" className="rounded hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
          Produk
        </Link>
        <span aria-hidden>/</span>
        <Link
          href={`/products?category=${encodeURIComponent(product.categorySlug)}`}
          className="rounded hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          {product.categoryName}
        </Link>
        <span aria-hidden>/</span>
        <span className="truncate font-medium text-foreground">{product.name}</span>
      </nav>

      <div className="grid gap-8 lg:grid-cols-2">
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          fallbackLabel={product.name}
          priority
          sizes="(max-width: 1024px) 100vw, 50vw"
          className="aspect-4/3 w-full rounded-2xl border border-border-subtle"
        />

        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="brand">{product.categoryName}</Badge>
            {product.inStock ? (
              <Badge tone="success">Stok tersedia</Badge>
            ) : (
              <Badge tone="danger">Stok habis</Badge>
            )}
            {lowStock ? <Badge tone="warning">Sisa {product.stockQuantity} unit</Badge> : null}
          </div>

          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{product.name}</h1>

          <Price cents={product.priceCents} currency={product.currency} size="xl" />

          <p className="text-sm leading-relaxed text-foreground/75">
            {product.description ?? "Belum ada deskripsi untuk produk ini."}
          </p>

          <AddToCartButton productId={product.id} inStock={product.inStock} stockQuantity={product.stockQuantity} />

          <Card>
            <CardContent>
              <dl className="divide-y divide-border-subtle">
                <DetailRow label="SKU" value={<span className="font-mono text-xs">{product.sku}</span>} />
                <DetailRow
                  label="Stok"
                  value={product.inStock ? `${product.stockQuantity} unit` : "Tidak tersedia"}
                />
                <DetailRow label="Kategori" value={product.categoryName} />
                <DetailRow label="Ditambahkan" value={formatDate(product.createdAt)} />
              </dl>
            </CardContent>
          </Card>

          <p className="flex items-start gap-2 text-xs text-foreground/55">
            <PackageIcon className="mt-0.5 h-4 w-4 shrink-0" />
            Pesanan dikirim dari gudang mitra. Nomor resi akan muncul di halaman detail pesanan setelah pembayaran
            terkonfirmasi.
          </p>
        </div>
      </div>

      {related.length > 0 ? (
        <section className="flex flex-col gap-4">
          <header className="flex flex-wrap items-end justify-between gap-3">
            <h2 className="text-lg font-semibold tracking-tight">Produk lain di {product.categoryName}</h2>
            <Link href={`/products?category=${encodeURIComponent(product.categorySlug)}`}>
              <span className="inline-flex items-center gap-1.5 text-sm font-medium text-brand underline underline-offset-2">
                Lihat kategori
                <ArrowRightIcon className="h-4 w-4" />
              </span>
            </Link>
          </header>
          <ProductGrid products={related} />
        </section>
      ) : null}
    </div>
  );
}
