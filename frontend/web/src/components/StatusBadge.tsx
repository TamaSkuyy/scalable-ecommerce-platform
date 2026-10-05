/**
 * StatusBadge.tsx — Renders a backend enum as a pill with the right colour.
 * Shared by the order list, the order detail page and the payment panel so the same status
 * never gets two different colours.
 */

import { Badge } from "@/components/ui/Badge";
import { orderStatusLabel, orderStatusTone, paymentStatusLabel, paymentStatusTone } from "@/lib/status";
import type { OrderStatus, PaymentStatus } from "@/lib/types";

export function OrderStatusBadge({ status }: { status: OrderStatus | string }) {
  return <Badge tone={orderStatusTone(status)}>{orderStatusLabel(status)}</Badge>;
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus | null | undefined }) {
  return <Badge tone={paymentStatusTone(status)}>{paymentStatusLabel(status)}</Badge>;
}

/** Marks placeholder data so a demo screenshot is never mistaken for real data. */
export function DemoBadge() {
  return <Badge tone="warning">Data contoh</Badge>;
}
