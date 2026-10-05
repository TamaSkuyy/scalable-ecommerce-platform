/**
 * `/orders/[orderNumber]` — order detail. Server Component.
 *
 * Three server-side reads in parallel:
 *   - the order itself                `GET /orders/{orderNumber}`
 *   - its payment record              `GET /payments/{orderNumber}`  (may 404 → normal)
 *   - the signed-in shopper           `GET /users/me`                (for the guard)
 *
 * The only interactive control — "Batalkan pesanan" — is a small Client Component, and it
 * only appears while the order is still `PENDING_PAYMENT`.
 */

import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";

import { getCurrentUser } from "@/lib/api-client";
import { fetchOrderDetail, fetchPayment } from "@/lib/catalog";
import { formatDateTime } from "@/lib/format";
import { ApiError } from "@/lib/api-error";
import { canCancelOrder, orderStatusHint } from "@/lib/status";
import { BackendOfflineNotice } from "@/components/BackendOfflineNotice";
import { OrderCancelButton } from "@/components/OrderCancelButton";
import { OrderStatusBadge, PaymentStatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { Card, CardContent, CardHeader, CardTitle, DetailRow } from "@/components/ui/Card";
import { Price } from "@/components/ui/Price";
import { ProductImage } from "@/components/ProductImage";

export const metadata: Metadata = {
  title: "Detail Pesanan",
  robots: { index: false, follow: false },
};

export default async function OrderDetailPage({ params }: PageProps<"/orders/[orderNumber]">) {
  const { orderNumber } = await params;

  const user = await getCurrentUser();
  if (!user) redirect(`/login?redirect=${encodeURIComponent(`/orders/${orderNumber}`)}`);

  let result: Awaited<ReturnType<typeof fetchOrderDetail>>;
  try {
    result = await fetchOrderDetail(orderNumber);
  } catch (error) {
    if (error instanceof ApiError && (error.isNotFound || error.isForbidden)) notFound();
    throw error;
  }

  const order = result.data;
  if (!order) notFound();

  const payment = await fetchPayment(orderNumber);

  return (
    <div className="flex flex-col gap-6">
      <nav aria-label="Remah roti" className="flex items-center gap-2 text-sm text-foreground/60">
        <Link href="/orders" className="rounded hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
          Riwayat pesanan
        </Link>
        <span aria-hidden>/</span>
        <span className="font-medium text-foreground">{order.orderNumber}</span>
      </nav>

      {result.demo ? <BackendOfflineNotice /> : null}

      <header className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{order.orderNumber}</h1>
          <OrderStatusBadge status={order.status} />
        </div>
        <p className="text-sm text-foreground/65">
          Dibuat {formatDateTime(order.createdAt)} · diperbarui {formatDateTime(order.updatedAt)}
        </p>
        {orderStatusHint(order.status) ? (
          <Callout tone={order.status === "PAYMENT_FAILED" ? "danger" : order.status === "PAID" ? "success" : "info"}>
            {orderStatusHint(order.status)}
          </Callout>
        ) : null}
      </header>

      <div className="grid gap-6 lg:grid-cols-[1fr_20rem] lg:items-start">
        <div className="flex flex-col gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Item pesanan</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              {order.items.map((item) => (
                <div key={`${item.productId}-${item.sku}`} className="flex gap-4">
                  <ProductImage
                    src={item.imageUrl}
                    alt={item.name}
                    fallbackLabel={item.name}
                    sizes="64px"
                    className="h-16 w-16 shrink-0 rounded-lg"
                  />
                  <div className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <p className="text-sm font-medium">{item.name}</p>
                    <p className="text-xs text-foreground/55">SKU {item.sku}</p>
                    <p className="text-xs text-foreground/55">
                      {item.quantity} × <Price cents={item.unitPriceCents} currency={order.currency} size="sm" />
                    </p>
                  </div>
                  <Price cents={item.lineTotalCents} currency={order.currency} size="md" />
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Alamat pengiriman</CardTitle>
            </CardHeader>
            <CardContent>
              <address className="not-italic text-sm leading-relaxed text-foreground/80">
                <span className="block font-medium text-foreground">{order.shippingAddress.recipientName}</span>
                <span className="block">{order.shippingAddress.phone}</span>
                <span className="block">{order.shippingAddress.line1}</span>
                {order.shippingAddress.line2 ? <span className="block">{order.shippingAddress.line2}</span> : null}
                <span className="block">
                  {order.shippingAddress.city} {order.shippingAddress.postalCode}
                </span>
                <span className="block">{order.shippingAddress.country}</span>
              </address>
              {order.notes ? (
                <p className="mt-4 rounded-lg bg-surface-muted px-3 py-2 text-sm text-foreground/75">
                  <span className="font-medium">Catatan: </span>
                  {order.notes}
                </p>
              ) : null}
            </CardContent>
          </Card>
        </div>

        <div className="flex flex-col gap-4 lg:sticky lg:top-24">
          <Card>
            <CardHeader>
              <CardTitle>Pembayaran</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap items-center gap-2">
                <PaymentStatusBadge status={payment?.status} />
              </div>
              <dl className="mt-3 divide-y divide-border-subtle">
                <DetailRow label="Metode" value={payment?.method ?? "—"} />
                <DetailRow label="Referensi" value={<span className="font-mono text-xs">{payment?.providerRef ?? "—"}</span>} />
                <DetailRow label="Dibuat" value={formatDateTime(payment?.createdAt)} />
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold">Total</span>
                <Price cents={order.totalCents} currency={order.currency} size="xl" />
              </div>
              <p className="text-xs text-foreground/55">{order.itemCount} item dalam pesanan ini.</p>
            </CardContent>
          </Card>

          {canCancelOrder(order.status) ? (
            <OrderCancelButton orderNumber={order.orderNumber} />
          ) : null}

          <Link href="/products">
            <Button variant="outline" fullWidth>
              Belanja lagi
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
