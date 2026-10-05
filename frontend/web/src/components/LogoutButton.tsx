"use client";

/**
 * LogoutButton.tsx — used on `/account`.
 * Signing out is a server-side cookie deletion, so the UI must refresh to pick up the new
 * (anonymous) state.
 */

import { useState } from "react";
import { useRouter } from "next/navigation";

import { authApi } from "@/lib/api";
import { emitCartUpdated } from "@/lib/cart-events";
import { Button } from "@/components/ui/Button";

export function LogoutButton() {
  const router = useRouter();
  const [pending, setPending] = useState(false);

  async function handleLogout() {
    setPending(true);
    try {
      await authApi.logout();
    } finally {
      setPending(false);
      emitCartUpdated();
      router.replace("/");
      router.refresh();
    }
  }

  return (
    <Button variant="outline" onClick={handleLogout} loading={pending}>
      Keluar dari akun
    </Button>
  );
}
