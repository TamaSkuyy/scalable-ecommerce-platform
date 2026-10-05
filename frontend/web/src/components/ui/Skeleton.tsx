/**
 * Skeleton.tsx — Loading placeholders.
 *
 * Used by the `loading.tsx` files. Next.js streams these instantly while the Server
 * Component awaits the gateway, which is what makes navigation feel immediate even on a
 * slow connection (this is React Suspense working for you).
 */

import { cn } from "@/lib/cn";

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-md bg-surface-muted", className)} />;
}

/** Placeholder that mirrors `ProductCard`'s proportions to avoid layout shift. */
export function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-xl border border-border-subtle bg-surface">
      <Skeleton className="aspect-4/3 w-full rounded-none" />
      <div className="flex flex-col gap-3 p-4">
        <Skeleton className="h-3 w-20" />
        <Skeleton className="h-4 w-4/5" />
        <Skeleton className="h-6 w-28" />
      </div>
    </div>
  );
}

/** Responsive grid of product skeletons. */
export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-3 xl:grid-cols-4">
      {Array.from({ length: count }, (_, index) => (
        <ProductCardSkeleton key={index} />
      ))}
    </div>
  );
}

/** Generic stacked text-line skeleton for lists (orders, notifications). */
export function ListSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="flex flex-col gap-3">
      {Array.from({ length: rows }, (_, index) => (
        <div key={index} className="rounded-xl border border-border-subtle bg-surface p-5">
          <div className="flex items-center justify-between gap-4">
            <Skeleton className="h-4 w-40" />
            <Skeleton className="h-5 w-24 rounded-full" />
          </div>
          <Skeleton className="mt-3 h-3 w-2/3" />
          <Skeleton className="mt-2 h-3 w-1/3" />
        </div>
      ))}
    </div>
  );
}
