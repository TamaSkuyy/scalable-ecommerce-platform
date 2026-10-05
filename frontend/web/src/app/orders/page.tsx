/**
 * `/orders` — order history. Server Component.
 *
 * AUTH ON THE SERVER: `getCurrentUser()` reads the httpOnly cookie through
 * `next/headers` and calls `GET /users/me`. If there is no session we `redirect()` to
 * `/login` — the same guard the middleware applies, repeated here so the page is safe even
 * if its matcher ever changes.
 *
 * PAGINATION is URL-driven (`?page=2`), so each page of history is a real, shareable URL.
 */

import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/lib/api-client";
import { fetchOrders } from "@/lib/catalog";
import { formatDateTime } from "@/lib/format";
import { firstParam } from "@/lib/types";
import { BackendOfflineNotice } from "@/components/BackendOfflineNotice";
import { OrderStatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Pagination } from "@/components/Pagination";
import { Price } from "@/components/ui/Price";
import { PackageIcon } from "@/components/icons";

export const metadata: Metadata = {
  title: "Riwayat Pesanan",
  description: "Lihat status dan detail seluruh pesanan Anda.",
  robots: { index: false, follow: false },
};

const PAGE_SIZE = 10;

export default async function OrdersPage({ searchParams }: PageProps<"/orders">) {
  const params = await searchParams;
  const pageParam = Number.parseInt(firstParam(params.page) || "0", 10);
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 0;

  // Server-side gate. `cookies()` here makes the route dynamic — correct for private data.
  const user = await getCurrentUser();
  if (!user) redirect("/login?redirect=%2Forders");

  const orders = await fetchOrders(page, PAGE_SIZE);

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Riwayat pesanan</h1>
        <p className="text-sm text-foreground/65">
          Halo {user.fullName.split(" ")[0]}, berikut daftar pesanan yang pernah Anda buat.
        </p>
      </header>

      {orders.demo ? <BackendOfflineNotice /> : null}

      {orders.data.content.length === 0 ? (
        <EmptyState
          icon={<PackageIcon className="h-5 w-5" />}
          title="Belum ada pesanan"
          description="Setelah Anda menyelesaikan checkout, pesanan akan muncul di halaman ini beserta status pembayarannya."
          action={
            <Link href="/products">
              <Button>Mulai belanja</Button>
            </Link>
          }
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {orders.data.content.map((order) => (
            <li key={order.orderNumber}>
              <Card className="flex flex-col gap-3 p-5 transition-shadow hover:shadow-md sm:flex-row sm:items-center sm:justify-between">
                <div className="flex min-w-0 flex-col gap-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Link
                      href={`/orders/${order.orderNumber}`}
                      className="rounded font-semibold hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                    >
                      {order.orderNumber}
                    </Link>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <p className="text-sm text-foreground/65">
                    {formatDateTime(order.createdAt)} · {order.itemCount} item
                  </p>
                </div>

                <div className="flex items-center justify-between gap-4 sm:justify-end">
                  <Price cents={order.totalCents} currency={order.currency} size="lg" />
                  <Link href={`/orders/${order.orderNumber}`}>
                    <Button variant="outline" size="sm">
                      Detail
                    </Button>
                  </Link>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <Pagination
        page={orders.data.page}
        totalPages={orders.data.totalPages}
        basePath="/orders"
        params={{ size: String(PAGE_SIZE) }}
      />
    </div>
  );
}
