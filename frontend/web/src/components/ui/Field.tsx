/**
 * Field.tsx — Form primitives (label + control + inline error).
 *
 * A form field is three things that must stay together: a `<label>` wired to the control,
 * the control itself, and the error message announced via `aria-describedby`. Bundling them
 * means no screen reader ever gets a label-less input.
 *
 * `Input` and `Textarea` forward `ref` implicitly: React 19 passes `ref` as a normal prop,
 * so no `forwardRef` wrapper is needed.
 */

import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

const CONTROL_BASE =
  "w-full rounded-lg border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-foreground/40 " +
  "transition-colors focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-brand " +
  "disabled:cursor-not-allowed disabled:opacity-60";

const CONTROL_TONE = {
  normal: "border-border-subtle",
  invalid: "border-danger focus-visible:outline-danger",
};

export interface FieldProps {
  label: string;
  htmlFor: string;
  error?: string | null;
  hint?: string;
  required?: boolean;
  children: ReactNode;
  className?: string;
}

export function Field({ label, htmlFor, error, hint, required, children, className }: FieldProps) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={htmlFor} className="text-sm font-medium">
        {label}
        {required ? <span className="ml-0.5 text-danger">*</span> : null}
      </label>
      {children}
      {hint && !error ? (
        <p id={`${htmlFor}-hint`} className="text-xs text-foreground/55">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={`${htmlFor}-error`} role="alert" className="text-xs font-medium text-danger">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export function Input({ className, invalid, ...rest }: InputProps) {
  return (
    <input
      aria-invalid={invalid || undefined}
      className={cn(CONTROL_BASE, invalid ? CONTROL_TONE.invalid : CONTROL_TONE.normal, "h-11", className)}
      {...rest}
    />
  );
}

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export function Textarea({ className, invalid, ...rest }: TextareaProps) {
  return (
    <textarea
      aria-invalid={invalid || undefined}
      className={cn(CONTROL_BASE, invalid ? CONTROL_TONE.invalid : CONTROL_TONE.normal, "min-h-24", className)}
      {...rest}
    />
  );
}

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

export function Select({ className, invalid, children, ...rest }: SelectProps) {
  return (
    <select
      aria-invalid={invalid || undefined}
      className={cn(CONTROL_BASE, invalid ? CONTROL_TONE.invalid : CONTROL_TONE.normal, "h-11 pr-8", className)}
      {...rest}
    >
      {children}
    </select>
  );
}
