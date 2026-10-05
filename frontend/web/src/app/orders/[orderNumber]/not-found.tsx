/**
 * `not-found.tsx` for an unknown order number.
 */

import Link from "next/link";

import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PackageIcon } from "@/components/icons";

export default function OrderNotFound() {
  return (
    <EmptyState
      icon={<PackageIcon className="h-5 w-5" />}
      title="Pesanan tidak ditemukan"
      description="Nomor pesanan tidak dikenali, atau pesanan ini bukan milik akun Anda."
      action={
        <Link href="/orders">
          <Button>Lihat riwayat pesanan</Button>
        </Link>
      }
    />
  );
}
