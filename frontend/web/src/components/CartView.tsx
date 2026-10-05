"use client";

/**
 * CartView.tsx — The `/cart` page body.
 *
 * WHY IS THE WHOLE CART A CLIENT COMPONENT?
 * Every interaction (change quantity, remove, empty the cart) is a mutation followed by a
 * re-read. Doing that on the client keeps the UI instant and avoids a full round-trip
 * through a Server Component on every stepper click. Server Components are for *reading*;
 * this screen is dominated by writing.
 *
 * Note the fetch paths: `/api/v1/cart...` — relative, same-origin. The middleware swaps in
 * the gateway host and the Bearer token. No token variable exists in this file, by design.
 */

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";

import { cartApi } from "@/lib/api";
import { toApiError } from "@/lib/api-error";
import { emitCartUpdated } from "@/lib/cart-events";
import { formatMoney } from "@/lib/format";
import type { CartDto } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Price } from "@/components/ui/Price";
import { ListSkeleton } from "@/components/ui/Skeleton";
import { ProductImage } from "@/components/ProductImage";
import { CartIcon, MinusIcon, PlusIcon, TrashIcon } from "@/components/icons";

type Status = "loading" | "ready" | "error";

export function CartView() {
  const [cart, setCart] = useState<CartDto | null>(null);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);
  /** productId currently being mutated, so only that row shows a spinner. */
  const [busyId, setBusyId] = useState<string | null>(null);
  const [clearing, setClearing] = useState(false);

  /** Pure loader — returns the cart, never sets state. See `CartBadge` for the rationale. */
  const fetchCart = useCallback(() => cartApi.get(), []);

  /** Applies a successfully loaded cart to the UI. Only ever called from async callbacks. */
  const applyCart = useCallback((data: CartDto) => {
    setCart(data);
    setStatus("ready");
    setError(null);
  }, []);

  const applyError = useCallback((cause: unknown) => {
    setError(toApiError(cause).message);
    setStatus("error");
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchCart()
      .then((data) => {
        if (!controller.signal.aborted) applyCart(data);
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) applyError(cause);
      });
    return () => controller.abort();
  }, [fetchCart, applyCart, applyError]);

  /** Manual retry, triggered by a button click (an event, not an effect). */
  async function reload() {
    setStatus("loading");
    try {
      applyCart(await fetchCart());
    } catch (cause) {
      applyError(cause);
    }
  }

  /** Runs a cart mutation, then republishes the server's answer as the new state. */
  async function mutate(key: string, action: () => Promise<CartDto | void>) {
    setBusyId(key);
    setError(null);
    try {
      const result = await action();
      if (result) {
        setCart(result);
      } else {
        // DELETE endpoints answer 204 with no body — refetch to get the fresh totals.
        applyCart(await fetchCart());
      }
      setStatus("ready");
      emitCartUpdated();
    } catch (cause) {
      setError(toApiError(cause).message);
    } finally {
      setBusyId(null);
    }
  }

  if (status === "loading") {
    return (
      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <ListSkeleton rows={3} />
        <Card className="h-48 animate-pulse" />
      </div>
    );
  }

  if (status === "error" && !cart) {
    return (
      <div className="flex flex-col gap-4">
        <Callout tone="danger" title="Gagal memuat keranjang">
          {error}
        </Callout>
        <div>
          <Button variant="outline" onClick={() => void reload()} type="button">
            Coba lagi
          </Button>
        </div>
      </div>
    );
  }

  const items = cart?.items ?? [];

  if (items.length === 0) {
    return (
      <EmptyState
        icon={<CartIcon className="h-5 w-5" />}
        title="Keranjang Anda masih kosong"
        description="Yuk mulai belanja. Tambahkan produk favorit Anda, lalu selesaikan pembayaran di halaman checkout."
        action={
          <Link href="/products">
            <Button>Lihat katalog produk</Button>
          </Link>
        }
      />
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_20rem] lg:items-start">
      <div className="flex flex-col gap-3">
        {error ? (
          <Callout tone="danger" title="Perubahan gagal disimpan">
            {error}
          </Callout>
        ) : null}

        <ul className="flex flex-col gap-3">
          {items.map((item) => {
            const key = String(item.productId);
            const busy = busyId === key;
            return (
              <li key={key}>
                <Card className="flex gap-4 p-4">
                  <Link
                    href={`/products/${item.slug}`}
                    className="shrink-0 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                  >
                    <ProductImage
                      src={item.imageUrl}
                      alt={item.name}
                      fallbackLabel={item.name}
                      sizes="96px"
                      className="h-20 w-20 rounded-lg sm:h-24 sm:w-24"
                    />
                  </Link>

                  <div className="flex min-w-0 flex-1 flex-col gap-2">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div className="min-w-0">
                        <Link
                          href={`/products/${item.slug}`}
                          className="line-clamp-2 rounded text-sm font-medium hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                        >
                          {item.name}
                        </Link>
                        <p className="mt-0.5 text-xs text-foreground/50">SKU {item.sku}</p>
                      </div>
                      <Price cents={item.lineTotalCents} size="md" />
                    </div>

                    <p className="text-xs text-foreground/60">
                      {formatMoney(item.unitPriceCents)} × {item.quantity}
                    </p>

                    <div className="mt-auto flex flex-wrap items-center gap-2">
                      <div className="flex h-9 items-center rounded-lg border border-border-subtle">
                        <button
                          type="button"
                          aria-label={`Kurangi jumlah ${item.name}`}
                          disabled={busy || item.quantity <= 1}
                          onClick={() => void mutate(key, () => cartApi.updateItem(item.productId, { quantity: item.quantity - 1 }))}
                          className="flex h-full w-9 items-center justify-center rounded-l-lg text-foreground/70 transition-colors hover:bg-surface-muted disabled:opacity-40 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-brand"
                        >
                          <MinusIcon className="h-3.5 w-3.5" />
                        </button>
                        <span className="w-10 text-center text-sm font-semibold tabular-nums">{item.quantity}</span>
                        <button
                          type="button"
                          aria-label={`Tambah jumlah ${item.name}`}
                          disabled={busy || item.quantity >= 99}
                          onClick={() => void mutate(key, () => cartApi.updateItem(item.productId, { quantity: item.quantity + 1 }))}
                          className="flex h-full w-9 items-center justify-center rounded-r-lg text-foreground/70 transition-colors hover:bg-surface-muted disabled:opacity-40 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-brand"
                        >
                          <PlusIcon className="h-3.5 w-3.5" />
                        </button>
                      </div>

                      <Button
                        variant="ghost"
                        size="sm"
                        loading={busy}
                        onClick={() => void mutate(key, () => cartApi.removeItem(item.productId))}
                        className="text-danger hover:bg-danger/10"
                      >
                        <TrashIcon className="h-4 w-4" />
                        Hapus
                      </Button>
                    </div>
                  </div>
                </Card>
              </li>
            );
          })}
        </ul>

        <div>
          <Button
            variant="ghost"
            size="sm"
            loading={clearing}
            className="text-foreground/60"
            onClick={() => {
              setClearing(true);
              void mutate("__clear__", () => cartApi.clear()).finally(() => setClearing(false));
            }}
          >
            Kosongkan keranjang
          </Button>
        </div>
      </div>

      {/* Order summary — sticky on large screens */}
      <Card className="flex flex-col gap-4 p-5 lg:sticky lg:top-24">
        <h2 className="text-base font-semibold">Ringkasan pesanan</h2>

        <dl className="flex flex-col gap-2 text-sm">
          <div className="flex items-center justify-between">
            <dt className="text-foreground/65">Jumlah item</dt>
            <dd className="font-medium tabular-nums">{cart?.itemCount ?? 0}</dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-foreground/65">Subtotal</dt>
            <dd>
              <Price cents={cart?.subtotalCents ?? 0} currency={cart?.currency} size="md" />
            </dd>
          </div>
        </dl>

        <div className="flex items-center justify-between border-t border-border-subtle pt-3">
          <span className="text-sm font-semibold">Total</span>
          <Price cents={cart?.subtotalCents ?? 0} currency={cart?.currency} size="lg" />
        </div>

        <Link href="/checkout" className="mt-1">
          <Button fullWidth size="lg">
            Lanjut ke checkout
          </Button>
        </Link>

        <Link
          href="/products"
          className="text-center text-sm font-medium text-foreground/65 underline underline-offset-2 hover:text-foreground"
        >
          Lanjut belanja
        </Link>
      </Card>
    </div>
  );
}
