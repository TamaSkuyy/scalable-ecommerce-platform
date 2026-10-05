/**
 * `/register` — Server Component shell around the client `RegisterForm`.
 */

import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/lib/api-client";
import { RegisterForm } from "@/components/RegisterForm";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";

export const metadata: Metadata = {
  title: "Daftar",
  description: "Buat akun ShopFlow untuk mulai berbelanja dan memantau pesanan Anda.",
  robots: { index: false, follow: false },
};

export default async function RegisterPage() {
  // A signed-in shopper has no business on the register form.
  const user = await getCurrentUser().catch(() => null);
  if (user) redirect("/account");

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6 py-4">
      <header className="flex flex-col gap-2 text-center">
        <h1 className="text-2xl font-bold tracking-tight">Buat akun ShopFlow</h1>
        <p className="text-sm text-foreground/65">
          Gratis, tanpa biaya langganan. Setelah mendaftar Anda langsung masuk ke akun baru.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle>Data akun</CardTitle>
        </CardHeader>
        <CardContent>
          <RegisterForm />
        </CardContent>
      </Card>
    </div>
  );
}
