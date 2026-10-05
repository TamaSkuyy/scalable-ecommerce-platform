/**
 * Button.tsx — The single button primitive used everywhere.
 *
 * NO `"use client"` ON PURPOSE: a component without the directive can be rendered by both
 * Server and Client Components. It only becomes interactive when a *parent* that is a
 * Client Component passes an `onClick`.
 */

import type { ButtonHTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/cn";
import { SpinnerIcon } from "@/components/icons";

export type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "danger";
export type ButtonSize = "sm" | "md" | "lg";

const VARIANTS: Record<ButtonVariant, string> = {
  primary: "bg-brand text-brand-foreground hover:opacity-90 border border-transparent shadow-sm",
  secondary: "bg-surface-muted text-foreground hover:bg-border-subtle border border-transparent",
  outline: "bg-surface text-foreground border border-border-subtle hover:bg-surface-muted",
  ghost: "bg-transparent text-foreground border border-transparent hover:bg-surface-muted",
  danger: "bg-danger text-white hover:opacity-90 border border-transparent shadow-sm",
};

const SIZES: Record<ButtonSize, string> = {
  sm: "h-9 px-3 text-sm gap-1.5",
  md: "h-11 px-4 text-sm gap-2",
  lg: "h-12 px-6 text-base gap-2",
};

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Shows a spinner and blocks interaction. */
  loading?: boolean;
  fullWidth?: boolean;
  children?: ReactNode;
}

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  fullWidth = false,
  className,
  children,
  disabled,
  type = "button",
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        "inline-flex items-center justify-center rounded-lg font-medium transition-colors",
        // Accessible focus ring, visible on both light and dark backgrounds.
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand",
        "disabled:cursor-not-allowed disabled:opacity-55",
        VARIANTS[variant],
        SIZES[size],
        fullWidth && "w-full",
        className,
      )}
      {...rest}
    >
      {loading ? <SpinnerIcon className="h-4 w-4" /> : null}
      {children}
    </button>
  );
}
