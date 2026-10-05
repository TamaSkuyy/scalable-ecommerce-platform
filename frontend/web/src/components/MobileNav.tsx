"use client";

/**
 * MobileNav.tsx — Small-screen navigation.
 *
 * Built on the native `<details>` element, which the browser already knows how to toggle.
 * That means zero JavaScript state for an open/closed menu — a nice reminder that not every
 * interactive element needs to be a React state machine.
 */

import { useState } from "react";

import { NavLinks } from "@/components/NavLinks";
import { MenuIcon } from "@/components/icons";

export function MobileNav() {
  const [open, setOpen] = useState(false);

  return (
    <details
      className="relative md:hidden"
      open={open}
      onToggle={(event) => setOpen((event.currentTarget as HTMLDetailsElement).open)}
    >
      <summary
        className="flex h-10 w-10 cursor-pointer list-none items-center justify-center rounded-lg text-foreground/75 transition-colors hover:bg-surface-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand [&::-webkit-details-marker]:hidden"
        aria-label="Buka menu navigasi"
      >
        <MenuIcon className="h-5 w-5" />
      </summary>
      <div className="absolute right-0 top-11 z-50 w-52 rounded-xl border border-border-subtle bg-surface p-2 shadow-lg">
        <NavLinks
          className="flex-col items-stretch"
          onNavigate={() => setOpen(false)}
        />
      </div>
    </details>
  );
}
