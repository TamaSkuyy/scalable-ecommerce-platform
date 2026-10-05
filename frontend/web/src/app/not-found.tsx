/**
 * Generic 404 for any unmatched route.
 *
 * `notFound()` from a Server Component, or simply a URL that matches no route, lands here.
 * It is a Server Component, so it costs no client JavaScript.
 */

import Link from "next/link";

import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { SearchIcon } from "@/components/icons";

export default function NotFound() {
  return (
    <EmptyState
      icon={<SearchIcon className="h-5 w-5" />}
      title="Halaman tidak ditemukan"
      description="Alamat yang Anda buka tidak ada atau sudah dipindahkan. Coba mulai dari katalog produk."
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
