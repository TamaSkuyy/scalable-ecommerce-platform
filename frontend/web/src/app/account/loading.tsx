/**
 * `loading.tsx` for `/account`.
 */

import { ListSkeleton, Skeleton } from "@/components/ui/Skeleton";

export default function AccountLoading() {
  return (
    <div className="flex flex-col gap-6">
      <Skeleton className="h-8 w-48" />
      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-72 w-full rounded-xl" />
        <ListSkeleton rows={2} />
      </div>
    </div>
  );
}
