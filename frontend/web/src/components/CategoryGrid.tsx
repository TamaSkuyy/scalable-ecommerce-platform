/**
 * CategoryGrid.tsx — Server Component. Category chips derived from `GET /categories`.
 * Each chip is a link to the catalog with the category pre-applied — again, the URL is
 * the state, so this is all plain markup.
 */

import Link from "next/link";

import type { CategoryDto } from "@/lib/types";
import { cn } from "@/lib/cn";

export function CategoryGrid({ categories, activeSlug }: { categories: CategoryDto[]; activeSlug?: string }) {
  if (categories.length === 0) return null;

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {categories.map((category) => {
        const active = category.slug === activeSlug;
        return (
          <Link
            key={category.slug}
            href={`/products?category=${encodeURIComponent(category.slug)}`}
            className={cn(
              "flex items-center justify-between gap-3 rounded-xl border px-4 py-3.5 text-sm font-medium transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand",
              active
                ? "border-brand bg-brand-soft text-brand"
                : "border-border-subtle bg-surface hover:bg-surface-muted",
            )}
          >
            <span className="truncate">{category.name}</span>
            <span aria-hidden className="text-foreground/35">
              →
            </span>
          </Link>
        );
      })}
    </div>
  );
}
