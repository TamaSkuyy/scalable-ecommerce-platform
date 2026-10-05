/**
 * Badge.tsx — Small status/category pill.
 * Tones are semantic (success/warning/danger/brand/neutral) so status colours stay
 * consistent between order lists, product cards and payment panels.
 */

import type { ReactNode } from "react";

import { cn } from "@/lib/cn";
import type { Tone } from "@/lib/status";

const TONES: Record<Tone, string> = {
  neutral: "bg-surface-muted text-foreground/70 border-border-subtle",
  success: "bg-success/10 text-success border-success/25",
  warning: "bg-warning/10 text-warning border-warning/25",
  danger: "bg-danger/10 text-danger border-danger/25",
  brand: "bg-brand-soft text-brand border-brand/25",
};

export interface BadgeProps {
  tone?: Tone;
  children: ReactNode;
  className?: string;
}

export function Badge({ tone = "neutral", children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap",
        TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
