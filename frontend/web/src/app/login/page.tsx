/**
 * `/login` — Server Component shell around the client `LoginForm`.
 *
 * It reads the `redirect` and `registered` query params so that:
 *   - `src/middleware.ts` can bounce an anonymous visitor to `/login?redirect=/cart`;
 *   - the register flow can land here with a success banner.
 *
 * `searchParams` is a Promise in the App Router (Next.js 15+), so it must be awaited.
 */

import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/lib/api-client";
import { firstParam } from "@/lib/types";
import { LoginForm } from "@/components/LoginForm";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";

export const metadata: Metadata = {
  title: "Masuk",
  description: "Masuk ke akun ShopFlow Anda untuk melihat keranjang, pesanan, dan notifikasi.",
  robots: { index: false, follow: false },
};

/** Only allow same-site relative redirects — never an attacker-supplied absolute URL. */
function safeRedirect(value: string): string {
  if (!value.startsWith("/") || value.startsWith("//")) return "/account";
  return value;
}

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const params = await searchParams;
  const redirectTo = safeRedirect(firstParam(params.redirect) || "/account");
  const justRegistered = firstParam(params.registered) === "1";

  // Already signed in? Skip the form. (Fails open when the gateway is unreachable.)
  const user = await getCurrentUser().catch(() => null);
  if (user) redirect(redirectTo);

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6 py-4">
      <header className="flex flex-col gap-2 text-center">
        <h1 className="text-2xl font-bold tracking-tight">Masuk ke akun Anda</h1>
        <p className="text-sm text-foreground/65">
          Token disimpan sebagai cookie <code className="rounded bg-surface-muted px-1.5 py-0.5">httpOnly</code>, jadi
          tidak bisa dibaca JavaScript.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle>Kredensial</CardTitle>
        </CardHeader>
        <CardContent>
          <LoginForm redirectTo={redirectTo} justRegistered={justRegistered} />
        </CardContent>
      </Card>
    </div>
  );
}
