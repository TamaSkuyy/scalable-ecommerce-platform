"use client";

/**
 * LoginForm.tsx — Client Component.
 *
 * It posts to `/api/auth/login` — a **route handler in this same Next.js app**, not the
 * gateway. That handler talks to the backend, receives the JWT, and stores it in an
 * httpOnly cookie. The JSON that comes back to this component contains the user only.
 *
 * Consequence: `localStorage.setItem("token", ...)` appears nowhere in this codebase, and
 * no XSS payload could read the session.
 */

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { authApi } from "@/lib/api";
import { toApiError } from "@/lib/api-error";
import { emitCartUpdated } from "@/lib/cart-events";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { Field, Input } from "@/components/ui/Field";

export interface LoginFormProps {
  /** Where to land after a successful sign-in (the `redirect` query param). */
  redirectTo?: string;
  /** Shows a success banner after a fresh registration. */
  justRegistered?: boolean;
}

export function LoginForm({ redirectTo = "/account", justRegistered = false }: LoginFormProps) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFormError(null);

    // Client-side guard rails: the BFF validates again, and so does the gateway.
    const local: Record<string, string> = {};
    if (!email.trim()) local.email = "Email wajib diisi.";
    if (!password) local.password = "Password wajib diisi.";
    if (Object.keys(local).length) {
      setFieldErrors(local);
      return;
    }

    setSubmitting(true);
    try {
      await authApi.login({ email: email.trim().toLowerCase(), password });
      emitCartUpdated();
      // replace() so the browser Back button does not return to the login form.
      router.replace(redirectTo);
      router.refresh(); // re-render Server Components (e.g. /account) with the new session
    } catch (cause) {
      const apiError = toApiError(cause);
      if (apiError.violations.length) {
        setFieldErrors(Object.fromEntries(apiError.violations.map((v) => [v.field, v.message])));
      } else {
        setFormError(apiError.message);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      {justRegistered ? (
        <Callout tone="success" title="Akun berhasil dibuat">
          Silakan masuk dengan email dan password yang baru Anda daftarkan.
        </Callout>
      ) : null}

      {formError ? (
        <Callout tone="danger" title="Gagal masuk">
          {formError}
        </Callout>
      ) : null}

      <Field label="Email" htmlFor="login-email" required error={fieldErrors.email}>
        <Input
          id="login-email"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          invalid={Boolean(fieldErrors.email)}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="nama@email.com"
        />
      </Field>

      <Field label="Password" htmlFor="login-password" required error={fieldErrors.password}>
        <Input
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          invalid={Boolean(fieldErrors.password)}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••••"
        />
      </Field>

      <Button type="submit" size="lg" loading={submitting} fullWidth>
        Masuk
      </Button>

      <p className="text-center text-sm text-foreground/65">
        Belum punya akun?{" "}
        <Link href="/register" className="font-medium text-brand underline underline-offset-2">
          Daftar sekarang
        </Link>
      </p>
    </form>
  );
}
