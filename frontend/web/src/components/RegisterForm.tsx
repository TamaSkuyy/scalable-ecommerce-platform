"use client";

/**
 * RegisterForm.tsx — Client Component.
 *
 * `POST /api/auth/register` is our BFF handler. The gateway's register endpoint does not
 * return a token, so the handler immediately performs a login and plants the same httpOnly
 * cookie — one round trip from the shopper's point of view. If that silent sign-in fails we
 * still created the account, so we send the shopper to `/login` instead of showing an error.
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

const MIN_PASSWORD_LENGTH = 8;

export function RegisterForm({ redirectTo = "/account" }: { redirectTo?: string }) {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFormError(null);

    const local: Record<string, string> = {};
    if (fullName.trim().length < 3) local.fullName = "Nama lengkap minimal 3 karakter.";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email.trim())) local.email = "Format email tidak valid.";
    if (password.length < MIN_PASSWORD_LENGTH) local.password = `Password minimal ${MIN_PASSWORD_LENGTH} karakter.`;
    if (password !== confirmPassword) local.confirmPassword = "Konfirmasi password tidak cocok.";
    if (Object.keys(local).length) {
      setFieldErrors(local);
      return;
    }

    setSubmitting(true);
    try {
      const result = await authApi.register({
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        password,
      });

      if (result.sessionEstablished) {
        emitCartUpdated();
        router.replace(redirectTo);
        router.refresh();
      } else {
        router.replace("/login?registered=1");
      }
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
      {formError ? (
        <Callout tone="danger" title="Pendaftaran gagal">
          {formError}
        </Callout>
      ) : null}

      <Field label="Nama lengkap" htmlFor="register-fullname" required error={fieldErrors.fullName}>
        <Input
          id="register-fullname"
          name="fullName"
          autoComplete="name"
          value={fullName}
          invalid={Boolean(fieldErrors.fullName)}
          onChange={(event) => setFullName(event.target.value)}
          placeholder="Budi Santoso"
        />
      </Field>

      <Field label="Email" htmlFor="register-email" required error={fieldErrors.email}>
        <Input
          id="register-email"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          invalid={Boolean(fieldErrors.email)}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="nama@email.com"
        />
      </Field>

      <Field
        label="Password"
        htmlFor="register-password"
        required
        error={fieldErrors.password}
        hint={`Minimal ${MIN_PASSWORD_LENGTH} karakter.`}
      >
        <Input
          id="register-password"
          name="password"
          type="password"
          autoComplete="new-password"
          value={password}
          invalid={Boolean(fieldErrors.password)}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••••"
        />
      </Field>

      <Field label="Konfirmasi password" htmlFor="register-confirm" required error={fieldErrors.confirmPassword}>
        <Input
          id="register-confirm"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          value={confirmPassword}
          invalid={Boolean(fieldErrors.confirmPassword)}
          onChange={(event) => setConfirmPassword(event.target.value)}
          placeholder="••••••••"
        />
      </Field>

      <Button type="submit" size="lg" loading={submitting} fullWidth>
        Buat akun
      </Button>

      <p className="text-center text-sm text-foreground/65">
        Sudah punya akun?{" "}
        <Link href="/login" className="font-medium text-brand underline underline-offset-2">
          Masuk di sini
        </Link>
      </p>
    </form>
  );
}
