/**
 * ProductGrid.tsx — Server Component. Responsive grid + empty state.
 */

import Link from "next/link";

import type { ProductSummary } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { ProductCard } from "@/components/ProductCard";
import { SearchIcon } from "@/components/icons";

export interface ProductGridProps {
  products: ProductSummary[];
  /** Copy for the empty state — differs between "no search results" and "empty catalog". */
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: { href: string; label: string };
}

export function ProductGrid({
  products,
  emptyTitle = "Produk tidak ditemukan",
  emptyDescription = "Coba ubah kata kunci pencarian atau pilih kategori lain.",
  emptyAction,
}: ProductGridProps) {
  if (products.length === 0) {
    return (
      <EmptyState
        icon={<SearchIcon className="h-5 w-5" />}
        title={emptyTitle}
        description={emptyDescription}
        action={
          emptyAction ? (
            <Link href={emptyAction.href}>
              <Button variant="outline">{emptyAction.label}</Button>
            </Link>
          ) : null
        }
      />
    );
  }

  return (
    <div className="grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-3 xl:grid-cols-4">
      {products.map((product, index) => (
        <ProductCard key={product.id} product={product} priority={index < 4} />
      ))}
    </div>
  );
}
