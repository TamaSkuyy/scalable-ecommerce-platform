"use client";

/**
 * OrderCancelButton.tsx — Client Component on the order detail page.
 *
 * Uses a two-step inline confirmation instead of `window.confirm()`: it works the same in
 * every browser, is styleable, and is far easier to reason about (and to test) than a
 * blocking native dialog.
 */

import { useState } from "react";
import { useRouter } from "next/navigation";

import { ordersApi } from "@/lib/api";
import { toApiError } from "@/lib/api-error";
import { emitCartUpdated } from "@/lib/cart-events";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";

export function OrderCancelButton({ orderNumber }: { orderNumber: string }) {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCancel() {
    setPending(true);
    setError(null);
    try {
      await ordersApi.cancel(orderNumber);
      emitCartUpdated();
      setConfirming(false);
      router.refresh(); // the Server Component re-reads the order and shows CANCELLED
    } catch (cause) {
      setError(toApiError(cause).message);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      {error ? (
        <Callout tone="danger" title="Gagal membatalkan pesanan">
          {error}
        </Callout>
      ) : null}

      {confirming ? (
        <div className="flex flex-col gap-3 rounded-xl border border-danger/25 bg-danger/5 p-4">
          <p className="text-sm font-medium">Batalkan pesanan {orderNumber}?</p>
          <p className="text-sm text-foreground/70">Tindakan ini tidak dapat dibatalkan.</p>
          <div className="flex flex-wrap gap-2">
            <Button variant="danger" onClick={handleCancel} loading={pending}>
              Ya, batalkan
            </Button>
            <Button variant="outline" onClick={() => setConfirming(false)} disabled={pending}>
              Tidak jadi
            </Button>
          </div>
        </div>
      ) : (
        <Button variant="outline" onClick={() => setConfirming(true)} className="w-fit text-danger">
          Batalkan pesanan
        </Button>
      )}
    </div>
  );
}
