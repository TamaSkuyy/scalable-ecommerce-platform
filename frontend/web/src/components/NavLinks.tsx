"use client";

/**
 * NavLinks.tsx — Client Component, only because it needs `usePathname()` to highlight the
 * active section. It is the smallest possible island: the rest of the header stays a
 * Server Component.
 */

import Link from "next/link";
import { usePathname } from "next/navigation";

import { cn } from "@/lib/cn";

export interface NavItem {
  href: string;
  label: string;
}

export const PRIMARY_NAV: NavItem[] = [
  { href: "/", label: "Beranda" },
  { href: "/products", label: "Produk" },
  { href: "/orders", label: "Pesanan" },
  { href: "/account", label: "Akun" },
];

export function NavLinks({ items = PRIMARY_NAV, className, onNavigate }: { items?: NavItem[]; className?: string; onNavigate?: () => void }) {
  const pathname = usePathname();

  return (
    <nav className={cn("flex items-center gap-1", className)} aria-label="Navigasi utama">
      {items.map((item) => {
        const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            aria-current={active ? "page" : undefined}
            className={cn(
              "rounded-lg px-3 py-2 text-sm font-medium transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand",
              active ? "bg-surface-muted text-foreground" : "text-foreground/70 hover:bg-surface-muted hover:text-foreground",
            )}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
