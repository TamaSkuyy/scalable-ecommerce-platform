/**
 * Card.tsx — Surface container with a subtle border and shadow.
 * Composed from small parts (`CardHeader`, `CardTitle`, `CardContent`, `CardFooter`) so
 * pages stay readable and spacing stays consistent.
 */

import type { HTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/cn";

export function Card({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "rounded-xl border border-border-subtle bg-surface shadow-[0_1px_2px_rgba(0,0,0,0.04)]",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

export function CardHeader({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("flex flex-wrap items-center justify-between gap-3 border-b border-border-subtle px-5 py-4", className)} {...rest}>
      {children}
    </div>
  );
}

export function CardTitle({ className, children, ...rest }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h2 className={cn("text-base font-semibold tracking-tight", className)} {...rest}>
      {children}
    </h2>
  );
}

export function CardDescription({ className, children, ...rest }: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p className={cn("text-sm text-foreground/65", className)} {...rest}>
      {children}
    </p>
  );
}

export function CardContent({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("px-5 py-4", className)} {...rest}>
      {children}
    </div>
  );
}

export function CardFooter({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("flex flex-wrap items-center gap-3 border-t border-border-subtle px-5 py-4", className)} {...rest}>
      {children}
    </div>
  );
}

/** Two-column definition list used on the order/account pages. */
export function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 py-2 sm:flex-row sm:items-baseline sm:gap-4">
      <dt className="w-full text-sm text-foreground/60 sm:w-44 sm:shrink-0">{label}</dt>
      <dd className="text-sm font-medium break-words">{value}</dd>
    </div>
  );
}
