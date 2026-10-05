"use client";

/**
 * HeaderSession.tsx — "Masuk / Daftar" vs. the signed-in shopper's name.
 *
 * Reads `GET /api/auth/session`, our BFF endpoint. That endpoint returns the safe user
 * projection only — the JWT lives in an httpOnly cookie this component cannot (and must
 * not) read.
 *
 * The initial render is a neutral skeleton so server-rendered HTML and the first client
 * render match exactly (no hydration mismatch).
 */

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { authApi } from "@/lib/api";
import { emitCartUpdated } from "@/lib/cart-events";
import type { UserDto } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { UserIcon } from "@/components/icons";

export function HeaderSession() {
  const router = useRouter();
  const [user, setUser] = useState<UserDto | null>(null);
  const [status, setStatus] = useState<"loading" | "ready">("loading");
  const [signingOut, setSigningOut] = useState(false);

  /** Pure loader — resolves to the session payload; never sets state itself. */
  const fetchSession = useCallback(() => authApi.session(), []);

  const applySession = useCallback((nextUser: UserDto | null) => {
    setUser(nextUser);
    setStatus("ready");
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchSession()
      .then((session) => {
        if (!controller.signal.aborted) applySession(session.user);
      })
      .catch(() => {
        // Anonymous visitor or gateway down — both mean "no session to show".
        if (!controller.signal.aborted) applySession(null);
      });
    return () => controller.abort();
  }, [fetchSession, applySession]);

  async function handleSignOut() {
    setSigningOut(true);
    try {
      await authApi.logout();
    } catch {
      // Even if the call fails the cookie is gone from the browser's perspective on reload;
      // we still refresh so the UI reflects the server state.
    } finally {
      setSigningOut(false);
      setUser(null);
      emitCartUpdated();
      router.refresh();
    }
  }

  if (status === "loading") {
    return <Skeleton className="h-9 w-28 rounded-lg" />;
  }

  if (!user) {
    return (
      <div className="flex items-center gap-1.5">
        <Link
          href="/login"
          className="rounded-lg px-3 py-2 text-sm font-medium text-foreground/75 transition-colors hover:bg-surface-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Masuk
        </Link>
        <Link
          href="/register"
          className="hidden rounded-lg bg-brand px-3 py-2 text-sm font-medium text-brand-foreground transition-opacity hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:inline-flex"
        >
          Daftar
        </Link>
      </div>
    );
  }

  const firstName = user.fullName.split(" ")[0] || user.email;

  return (
    <div className="flex items-center gap-1">
      <Link
        href="/account"
        className="inline-flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-medium text-foreground/80 transition-colors hover:bg-surface-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
      >
        <UserIcon className="h-4 w-4" />
        <span className="hidden max-w-24 truncate sm:inline">{firstName}</span>
      </Link>
      <Button variant="ghost" size="sm" onClick={handleSignOut} loading={signingOut}>
        Keluar
      </Button>
    </div>
  );
}
