"use client";

/**
 * Root error boundary.
 *
 * RULES FOR `error.tsx`:
 *  1. It MUST be a Client Component (`"use client"`) — error boundaries only exist in React's
 *     client runtime.
 *  2. It receives `{ error, reset }`. `reset()` re-renders the failed segment, which is
 *     usually enough for a transient gateway hiccup.
 *  3. It does NOT catch errors thrown by the root layout (use `global-error.tsx` for that).
 *
 * The catalog pages handle a down gateway themselves (demo fallback), so reaching this
 * boundary means something genuinely unexpected happened.
 */

import { useEffect } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    // In a real deployment this is where you would ship the error to Sentry/Datadog.
    console.error("[ShopFlow] route error:", error);
  }, [error]);

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-5 py-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight">Terjadi kesalahan</h1>
        <p className="text-sm text-foreground/65">
          Kami tidak dapat menyelesaikan permintaan Anda saat ini. Coba muat ulang sebentar lagi.
        </p>
      </header>

      <Callout tone="danger" title="Detail teknis">
        <p className="break-words font-mono text-xs">{error.message || "Kesalahan tidak diketahui."}</p>
        {error.digest ? <p className="mt-1 font-mono text-xs text-foreground/60">digest: {error.digest}</p> : null}
      </Callout>

      <div className="flex flex-wrap gap-3">
        <Button onClick={reset}>Coba lagi</Button>
        <Link href="/">
          <Button variant="outline">Kembali ke beranda</Button>
        </Link>
      </div>
    </div>
  );
}
