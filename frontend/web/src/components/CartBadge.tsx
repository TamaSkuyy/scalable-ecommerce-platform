"use client";

/**
 * CartBadge.tsx — Live item-count badge in the header.
 *
 * WHY A CLIENT COMPONENT? The header lives in the root layout. If it read the cookie on the
 * server, *every* route would become dynamic and the ISR on `/` and `/products` would be
 * pointless. Instead the badge fetches `/api/v1/cart` from the browser after mount — and
 * because `src/middleware.ts` injects the Bearer token, this component needs no auth code.
 *
 * It stays in sync with the cart page through the tiny event bus in `src/lib/cart-events.ts`.
 */

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";

import { cartApi } from "@/lib/api";
import { onCartUpdated } from "@/lib/cart-events";
import { CartIcon } from "@/components/icons";

export function CartBadge() {
  const [count, setCount] = useState<number | null>(null);

  /**
   * Pure loader: it returns the count and never touches state itself.
   *
   * Why so strict? React's lint rules (and the React Compiler) want state updates to happen
   * in response to an *event* or to an async resolution — not synchronously inside an effect
   * body. Keeping the loader pure lets the effect, the event bus and any future retry button
   * share it without fighting the rules.
   */
  const fetchCount = useCallback(async (): Promise<number | null> => {
    try {
      const cart = await cartApi.get();
      return cart.itemCount;
    } catch {
      // Anonymous visitor (401) or the gateway is down: show the icon without a count.
      return null;
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    const apply = () => {
      void fetchCount().then((next) => {
        if (!controller.signal.aborted) setCount(next);
      });
    };

    apply();
    const unsubscribe = onCartUpdated(apply);

    return () => {
      controller.abort();
      unsubscribe();
    };
  }, [fetchCount]);

  return (
    <Link
      href="/cart"
      aria-label={count ? `Keranjang, ${count} item` : "Keranjang"}
      className="relative inline-flex h-10 w-10 items-center justify-center rounded-lg text-foreground/75 transition-colors hover:bg-surface-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
    >
      <CartIcon className="h-5 w-5" />
      {count && count > 0 ? (
        <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand px-1 text-[11px] font-semibold text-brand-foreground">
          {count > 99 ? "99+" : count}
        </span>
      ) : null}
    </Link>
  );
}
