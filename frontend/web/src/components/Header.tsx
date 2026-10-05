/**
 * Header.tsx — Server Component.
 *
 * It deliberately renders NO user-specific data itself: the session label and the cart
 * badge are client islands (`HeaderSession`, `CartBadge`). That keeps the root layout
 * static, so `/` and `/products` can still be served from the ISR cache.
 */

import Link from "next/link";

import { STORE } from "@/lib/config";
import { CartBadge } from "@/components/CartBadge";
import { HeaderSession } from "@/components/HeaderSession";
import { MobileNav } from "@/components/MobileNav";
import { NavLinks } from "@/components/NavLinks";
import { StoreIcon } from "@/components/icons";

export function Header() {
  return (
    <header className="sticky top-0 z-40 border-b border-border-subtle bg-background/85 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-3 px-4 sm:px-6">
        <Link
          href="/"
          className="flex items-center gap-2 rounded-lg text-base font-semibold tracking-tight focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand text-brand-foreground">
            <StoreIcon className="h-4 w-4" />
          </span>
          {STORE.name}
        </Link>

        <NavLinks className="ml-4 hidden md:flex" />

        <div className="ml-auto flex items-center gap-1.5">
          <HeaderSession />
          <CartBadge />
          <MobileNav />
        </div>
      </div>
    </header>
  );
}
