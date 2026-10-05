/**
 * CatalogFilters.tsx — Server Component that renders a plain HTML `<form method="get">`.
 *
 * WHY NO JAVASCRIPT? Because search/filter/sort are just query parameters. A native GET form
 * submits `/products?q=kaos&category=fashion&sort=price,asc` and the Server Component
 * re-renders from `searchParams`. The result is:
 *   - zero client bundle for the filters,
 *   - a shareable URL for every filter combination,
 *   - filters that still work if JavaScript fails to load.
 *
 * That is the "URL is the state" model the App Router encourages.
 */

import Link from "next/link";

import type { CategoryDto } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Field";
import { SearchIcon } from "@/components/icons";

export interface CatalogFiltersProps {
  categories: CategoryDto[];
  q: string;
  category: string;
  sort: string;
  size: number;
  sortOptions: ReadonlyArray<{ value: string; label: string }>;
}

export function CatalogFilters({ categories, q, category, sort, size, sortOptions }: CatalogFiltersProps) {
  const hasFilters = Boolean(q || category) || sort !== sortOptions[0]?.value;

  return (
    <form
      action="/products"
      method="get"
      className="flex flex-col gap-3 rounded-xl border border-border-subtle bg-surface p-4 sm:flex-row sm:flex-wrap sm:items-end"
    >
      {/* Preserve page size across submissions. `page` is intentionally omitted so a new
          search always starts at page 0. */}
      <input type="hidden" name="size" value={size} />

      <div className="flex min-w-0 flex-1 flex-col gap-1.5 sm:min-w-56">
        <label htmlFor="filter-q" className="text-xs font-medium text-foreground/70">
          Cari produk
        </label>
        <div className="relative">
          <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground/40" />
          <Input
            id="filter-q"
            name="q"
            type="search"
            defaultValue={q}
            placeholder="Nama produk atau SKU…"
            className="pl-9"
          />
        </div>
      </div>

      <div className="flex flex-col gap-1.5 sm:w-48">
        <label htmlFor="filter-category" className="text-xs font-medium text-foreground/70">
          Kategori
        </label>
        <Select id="filter-category" name="category" defaultValue={category}>
          <option value="">Semua kategori</option>
          {categories.map((item) => (
            <option key={item.slug} value={item.slug}>
              {item.name}
            </option>
          ))}
        </Select>
      </div>

      <div className="flex flex-col gap-1.5 sm:w-48">
        <label htmlFor="filter-sort" className="text-xs font-medium text-foreground/70">
          Urutkan
        </label>
        <Select id="filter-sort" name="sort" defaultValue={sort}>
          {sortOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </Select>
      </div>

      <div className="flex items-center gap-2">
        <Button type="submit" variant="primary" className="sm:w-auto">
          Terapkan
        </Button>
        {hasFilters ? (
          <Link
            href="/products"
            className="rounded-lg px-3 py-2 text-sm font-medium text-foreground/70 transition-colors hover:bg-surface-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            Reset
          </Link>
        ) : null}
      </div>
    </form>
  );
}
