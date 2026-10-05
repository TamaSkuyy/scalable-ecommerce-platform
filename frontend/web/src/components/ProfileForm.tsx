"use client";

/**
 * ProfileForm.tsx — inline profile editing on `/account`.
 *
 * Demonstrates `PATCH /api/v1/users/me` (a write, so no demo fallback: if the gateway is
 * down the shopper must see that the change did NOT persist).
 */

import { useState } from "react";
import { useRouter } from "next/navigation";

import { userApi } from "@/lib/api";
import { toApiError } from "@/lib/api-error";
import type { UserDto } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { Field, Input } from "@/components/ui/Field";

export function ProfileForm({ user }: { user: UserDto }) {
  const router = useRouter();
  const [fullName, setFullName] = useState(user.fullName);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const dirty = fullName.trim() !== user.fullName;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaved(false);
    setError(null);
    setFieldError(null);

    if (fullName.trim().length < 3) {
      setFieldError("Nama lengkap minimal 3 karakter.");
      return;
    }

    setPending(true);
    try {
      await userApi.updateProfile({ fullName: fullName.trim() });
      setSaved(true);
      router.refresh();
    } catch (cause) {
      const apiError = toApiError(cause);
      const violation = apiError.violations.find((item) => item.field.endsWith("fullName"));
      if (violation) setFieldError(violation.message);
      else setError(apiError.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      {saved ? (
        <Callout tone="success" title="Profil diperbarui">
          Perubahan nama sudah tersimpan.
        </Callout>
      ) : null}
      {error ? (
        <Callout tone="danger" title="Gagal menyimpan">
          {error}
        </Callout>
      ) : null}

      <Field label="Nama lengkap" htmlFor="profile-fullname" required error={fieldError}>
        <Input
          id="profile-fullname"
          name="fullName"
          autoComplete="name"
          value={fullName}
          invalid={Boolean(fieldError)}
          onChange={(event) => {
            setFullName(event.target.value);
            setSaved(false);
          }}
        />
      </Field>

      <Field label="Email" htmlFor="profile-email" hint="Email tidak dapat diubah.">
        <Input id="profile-email" value={user.email} disabled readOnly />
      </Field>

      <div className="flex items-center gap-3">
        <Button type="submit" loading={pending} disabled={!dirty}>
          Simpan perubahan
        </Button>
        {dirty ? (
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setFullName(user.fullName);
              setFieldError(null);
            }}
          >
            Batalkan
          </Button>
        ) : null}
      </div>
    </form>
  );
}
