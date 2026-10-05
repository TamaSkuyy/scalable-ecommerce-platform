/**
 * Pagination.tsx — Server Component.
 *
 * It takes serializable props (`basePath` + `params`) instead of a `buildHref` callback,
 * so it stays usable from Server Components and could be reused inside a Client Component
 * without a "functions cannot be passed to Client Components" error.
 *
 * The page links are ordinary `<a>`/`<Link>` elements: pagination works without JavaScript
 * and each page is a distinct, shareable, crawlable URL.
 */

import Link from "next/link";

import { cn } from "@/lib/cn";

export interface PaginationProps {
  page: number;
  totalPages: number;
  basePath: string;
  /** Current query params to preserve (q, category, sort, size...). */
  params?: Record<string, string | undefined>;
  className?: string;
}

export function Pagination({ page, totalPages, basePath, params = {}, className }: PaginationProps) {
  if (totalPages <= 1) return null;

  const hrefFor = (target: number) => {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value) search.set(key, value);
    }
    search.set("page", String(target));
    return `${basePath}?${search.toString()}`;
  };

  const pages = pageWindow(page, totalPages);

  return (
    <nav className={cn("flex flex-wrap items-center justify-center gap-1.5", className)} aria-label="Navigasi halaman">
      <PageLink href={hrefFor(Math.max(0, page - 1))} disabled={page === 0} label="Sebelumnya" />

      {pages.map((entry, index) =>
        entry === "gap" ? (
          <span key={`gap-${index}`} className="px-1 text-sm text-foreground/45" aria-hidden>
            …
          </span>
        ) : (
          <Link
            key={entry}
            href={hrefFor(entry)}
            aria-current={entry === page ? "page" : undefined}
            className={cn(
              "flex h-9 min-w-9 items-center justify-center rounded-lg border px-3 text-sm font-medium transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand",
              entry === page
                ? "border-transparent bg-brand text-brand-foreground"
                : "border-border-subtle bg-surface hover:bg-surface-muted",
            )}
          >
            {entry + 1}
          </Link>
        ),
      )}

      <PageLink href={hrefFor(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1} label="Berikutnya" />
    </nav>
  );
}

function PageLink({ href, disabled, label }: { href: string; disabled: boolean; label: string }) {
  if (disabled) {
    return (
      <span className="flex h-9 items-center rounded-lg border border-border-subtle px-3 text-sm font-medium text-foreground/35">
        {label}
      </span>
    );
  }
  return (
    <Link
      href={href}
      className="flex h-9 items-center rounded-lg border border-border-subtle bg-surface px-3 text-sm font-medium transition-colors hover:bg-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
    >
      {label}
    </Link>
  );
}

/** Builds a compact page list: 1 … 4 5 6 … 20 */
function pageWindow(page: number, totalPages: number): Array<number | "gap"> {
  const windowSize = 1;
  const entries = new Set<number>([0, totalPages - 1, page]);
  for (let offset = 1; offset <= windowSize; offset += 1) {
    entries.add(page - offset);
    entries.add(page + offset);
  }

  const sorted = [...entries].filter((value) => value >= 0 && value < totalPages).sort((a, b) => a - b);

  const out: Array<number | "gap"> = [];
  let previous = -1;
  for (const value of sorted) {
    if (previous >= 0 && value - previous > 1) out.push("gap");
    out.push(value);
    previous = value;
  }
  return out;
}
