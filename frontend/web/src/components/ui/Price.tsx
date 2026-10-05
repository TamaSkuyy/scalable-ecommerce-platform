/**
 * Price.tsx — Renders an integer minor-unit amount as localized currency.
 *
 * All price rendering funnels through here, which is why `formatMoney` (divide by 100 once)
 * is the only place in the codebase that touches raw amounts.
 */

import { cn } from "@/lib/cn";
import { formatMoney } from "@/lib/format";

const SIZES = {
  sm: "text-sm font-semibold",
  md: "text-base font-semibold",
  lg: "text-xl font-bold tracking-tight",
  xl: "text-3xl font-bold tracking-tight",
} as const;

export interface PriceProps {
  cents: number | null | undefined;
  currency?: string;
  size?: keyof typeof SIZES;
  className?: string;
}

export function Price({ cents, currency, size = "md", className }: PriceProps) {
  const formatted = formatMoney(cents, currency);
  return (
    <span className={cn("tabular-nums", SIZES[size], className)}>
      {/* Screen readers get the raw formatted string; the visual output is identical. */}
      {formatted}
    </span>
  );
}
