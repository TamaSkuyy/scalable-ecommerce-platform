/**
 * EmptyState.tsx — Shared "nothing here yet" panel.
 *
 * Reused by the cart, the order history, the notification list and the product grid so an
 * empty screen always looks intentional and always offers a next step.
 */

import type { ReactNode } from "react";

import { cn } from "@/lib/cn";

export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: ReactNode;
  /** Primary call to action, usually a `<Link>` styled as a button. */
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border-subtle bg-surface px-6 py-14 text-center",
        className,
      )}
    >
      {icon ? (
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-surface-muted text-foreground/60">
          {icon}
        </span>
      ) : null}
      <h2 className="text-base font-semibold">{title}</h2>
      {description ? <p className="max-w-md text-sm text-foreground/65">{description}</p> : null}
      {action ? <div className="mt-1">{action}</div> : null}
    </div>
  );
}
