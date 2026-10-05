"use client";

/**
 * AddToCartButton.tsx — Client Component on the product detail page.
 *
 * This is the boundary between the server world and the browser:
 *   - the product page is a Server Component (it fetched the data on the server),
 *   - this button needs `onClick`, `useState` and browser `fetch`, so it is a Client
 *     Component and the page passes only serializable props (a number and a boolean).
 *
 * The POST goes to the *relative* path `/api/v1/cart/items`. The middleware adds the Bearer
 * token, so no auth code appears here at all.
 */

import { useState } from "react";
import { useRouter } from "next/navigation";

import { cartApi } from "@/lib/api";
import { toApiError } from "@/lib/api-error";
import { emitCartUpdated } from "@/lib/cart-events";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { CheckIcon, MinusIcon, PlusIcon } from "@/components/icons";

export interface AddToCartButtonProps {
  productId: number | string;
  inStock: boolean;
  /** Remaining stock, used to cap the quantity stepper. */
  stockQuantity?: number;
}

export function AddToCartButton({ productId, inStock, stockQuantity }: AddToCartButtonProps) {
  const router = useRouter();
  const [quantity, setQuantity] = useState(1);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [added, setAdded] = useState(false);

  const max = Math.max(1, Math.min(stockQuantity ?? 99, 99));

  async function handleAdd() {
    setPending(true);
    setError(null);
    setAdded(false);
    try {
      await cartApi.addItem({ productId, quantity });
      setAdded(true);
      emitCartUpdated(); // keeps the header badge honest
      router.refresh(); // re-renders server components that may show cart totals
    } catch (cause) {
      const apiError = toApiError(cause);
      // 401 => the session expired; send the shopper to login and come straight back.
      if (apiError.isUnauthorized) {
        router.push(`/login?redirect=${encodeURIComponent(`/products`)}`);
        return;
      }
      setError(apiError.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex h-12 items-center rounded-lg border border-border-subtle bg-surface">
          <button
            type="button"
            onClick={() => setQuantity((value) => Math.max(1, value - 1))}
            disabled={!inStock || quantity <= 1}
            aria-label="Kurangi jumlah"
            className="flex h-full w-11 items-center justify-center rounded-l-lg text-foreground/70 transition-colors hover:bg-surface-muted disabled:opacity-40 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-brand"
          >
            <MinusIcon className="h-4 w-4" />
          </button>
          <span aria-live="polite" className="w-12 text-center text-sm font-semibold tabular-nums">
            {quantity}
          </span>
          <button
            type="button"
            onClick={() => setQuantity((value) => Math.min(max, value + 1))}
            disabled={!inStock || quantity >= max}
            aria-label="Tambah jumlah"
            className="flex h-full w-11 items-center justify-center rounded-r-lg text-foreground/70 transition-colors hover:bg-surface-muted disabled:opacity-40 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-brand"
          >
            <PlusIcon className="h-4 w-4" />
          </button>
        </div>

        <Button size="lg" onClick={handleAdd} loading={pending} disabled={!inStock} className="min-w-52 flex-1 sm:flex-none">
          {inStock ? "Tambah ke Keranjang" : "Stok habis"}
        </Button>
      </div>

      {added ? (
        <Callout tone="success" className="items-center">
          <span className="inline-flex items-center gap-2">
            <CheckIcon className="h-4 w-4 text-success" />
            {quantity} item ditambahkan ke keranjang.
          </span>
        </Callout>
      ) : null}

      {error ? (
        <Callout tone="danger" title="Gagal menambahkan ke keranjang">
          {error}
        </Callout>
      ) : null}
    </div>
  );
}
