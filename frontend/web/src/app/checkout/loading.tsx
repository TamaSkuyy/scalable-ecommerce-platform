/**
 * `loading.tsx` for `/checkout`.
 *
 * Safe to put a segment-level boundary here: `/checkout` has no child routes and never calls
 * `notFound()`, so an early-flushed 200 status cannot hide a 404. (Contrast with
 * `/products/[slug]` — see `src/components/CatalogSkeleton.tsx` for why the catalog's
 * skeleton lives inside the page instead.)
 */

import { ListSkeleton, Skeleton } from "@/components/ui/Skeleton";

export default function CheckoutLoading() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="h-4 w-96 max-w-full" />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_20rem] lg:items-start">
        <Skeleton className="h-96 w-full rounded-xl" />
        <ListSkeleton rows={2} />
      </div>
    </div>
  );
}
