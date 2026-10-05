/**
 * Product-level 404. Next.js renders this whenever `notFound()` is called inside
 * `/products/[slug]` — i.e. the gateway answered 404, or the demo dataset has no such slug.
 */

import Link from "next/link";

import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { SearchIcon } from "@/components/icons";

export default function ProductNotFound() {
  return (
    <EmptyState
      icon={<SearchIcon className="h-5 w-5" />}
      title="Produk tidak ditemukan"
      description="Produk yang Anda cari mungkin sudah dihapus, atau tautannya salah. Coba jelajahi katalog kami."
      action={
        <div className="flex flex-wrap justify-center gap-3">
          <Link href="/products">
            <Button>Lihat katalog</Button>
          </Link>
          <Link href="/">
            <Button variant="outline">Kembali ke beranda</Button>
          </Link>
        </div>
      }
    />
  );
}
