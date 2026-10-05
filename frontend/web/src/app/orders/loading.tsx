/**
 * `loading.tsx` for `/orders` and `/orders/[orderNumber]`.
 */

import { ListSkeleton, Skeleton } from "@/components/ui/Skeleton";

export default function OrdersLoading() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-4 w-72" />
      </div>
      <ListSkeleton rows={4} />
    </div>
  );
}
