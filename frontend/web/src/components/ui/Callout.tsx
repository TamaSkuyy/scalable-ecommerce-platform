/**
 * Callout.tsx — Inline informational banner.
 *
 * Used for the demo-mode notice, form-level errors, and empty/edge states. `role` switches
 * between `status` (polite) and `alert` (assertive) so assistive tech announces errors
 * immediately but warnings only when convenient.
 */

import type { ReactNode } from "react";

import { cn } from "@/lib/cn";
import { AlertIcon, CheckIcon, InfoIcon } from "@/components/icons";

export type CalloutTone = "info" | "success" | "warning" | "danger";

const TONES: Record<CalloutTone, { wrapper: string; icon: ReactNode }> = {
  info: { wrapper: "border-brand/25 bg-brand-soft/60 text-foreground", icon: <InfoIcon className="h-4 w-4 text-brand" /> },
  success: { wrapper: "border-success/25 bg-success/10 text-foreground", icon: <CheckIcon className="h-4 w-4 text-success" /> },
  warning: { wrapper: "border-warning/30 bg-warning/10 text-foreground", icon: <AlertIcon className="h-4 w-4 text-warning" /> },
  danger: { wrapper: "border-danger/30 bg-danger/10 text-foreground", icon: <AlertIcon className="h-4 w-4 text-danger" /> },
};

export interface CalloutProps {
  tone?: CalloutTone;
  title?: string;
  children?: ReactNode;
  className?: string;
  /** Optional right-hand slot, e.g. a "Coba lagi" button. */
  action?: ReactNode;
}

export function Callout({ tone = "info", title, children, className, action }: CalloutProps) {
  const config = TONES[tone];
  return (
    <div
      role={tone === "danger" ? "alert" : "status"}
      className={cn("flex items-start gap-3 rounded-xl border px-4 py-3 text-sm", config.wrapper, className)}
    >
      <span className="mt-0.5 shrink-0">{config.icon}</span>
      <div className="min-w-0 flex-1">
        {title ? <p className="font-semibold">{title}</p> : null}
        {children ? <div className={cn("text-foreground/80", title && "mt-0.5")}>{children}</div> : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
