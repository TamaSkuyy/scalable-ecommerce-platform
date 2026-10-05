/**
 * ProductCard.tsx — Server Component.
 *
 * Rendered on the server for every product in the grid, so the card ships as pure HTML with
 * no JavaScript. Only the image inside is a client island (for its error fallback).
 */

import Link from "next/link";

import type { ProductSummary } from "@/lib/types";
import { Badge } from "@/components/ui/Badge";
import { Price } from "@/components/ui/Price";
import { ProductImage } from "@/components/ProductImage";

export function ProductCard({ product, priority = false }: { product: ProductSummary; priority?: boolean }) {
  return (
    <article className="group flex h-full flex-col overflow-hidden rounded-xl border border-border-subtle bg-surface shadow-[0_1px_2px_rgba(0,0,0,0.04)] transition-shadow hover:shadow-md">
      <Link
        href={`/products/${product.slug}`}
        className="flex h-full flex-col focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-brand"
      >
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          fallbackLabel={product.name}
          priority={priority}
          sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
          className="aspect-4/3 w-full"
        />

        <div className="flex flex-1 flex-col gap-2 p-4">
          <div className="flex items-center justify-between gap-2">
            <Badge tone="neutral">{product.categoryName}</Badge>
            {product.inStock ? (
              <span className="text-xs font-medium text-success">Tersedia</span>
            ) : (
              <span className="text-xs font-medium text-danger">Stok habis</span>
            )}
          </div>

          <h3 className="line-clamp-2 text-sm font-medium leading-snug group-hover:underline">{product.name}</h3>

          <div className="mt-auto flex items-end justify-between gap-2 pt-1">
            <Price cents={product.priceCents} currency={product.currency} size="lg" />
          </div>

          <p className="text-xs text-foreground/45">SKU {product.sku}</p>
        </div>
      </Link>
    </article>
  );
}
