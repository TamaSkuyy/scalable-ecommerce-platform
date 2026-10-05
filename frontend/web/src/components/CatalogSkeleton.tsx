/**
 * CatalogSkeleton.tsx — the streaming fallback for the catalog results.
 *
 * WHY THIS IS A COMPONENT AND NOT `app/products/loading.tsx`:
 *
 * A `loading.tsx` creates a Suspense boundary for a whole route *segment*, which means it
 * also wraps `app/products/[slug]`. A boundary that flushes early commits the HTTP status
 * to 200 before the page has decided what to render — so `notFound()` on an unknown product
 * slug would produce a **soft 404** (200 status, "not found" body). Search engines treat
 * that as a real page and index it.
 *
 * Putting the boundary *inside* `app/products/page.tsx` gives the catalog the exact same
 * instant skeleton while leaving the detail route streaming-free, so a deleted product still
 * answers a hard `404`.
 */

import { ProductGridSkeleton, Skeleton } from "@/components/ui/Skeleton";

export function CatalogSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-busy="true" aria-live="polite">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-40" />
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {Array.from({ length: 5 }, (_, index) => (
          <Skeleton key={index} className="h-12 w-full rounded-xl" />
        ))}
      </div>

      <Skeleton className="h-24 w-full rounded-xl" />

      <ProductGridSkeleton count={8} />
    </div>
  );
}
