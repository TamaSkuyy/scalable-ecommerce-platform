/**
 * `/account` — profile + notifications. Server Component.
 *
 * This is the clearest illustration of the BFF cookie flow end to end:
 *   1. The browser sends the httpOnly `shopflow_token` cookie with this navigation.
 *   2. `getCurrentUser()` (server-only) reads it via `next/headers` and calls the gateway
 *      with `Authorization: Bearer ...`.
 *   3. `getNotifications()` does exactly the same for a second endpoint — in parallel.
 * No token ever crosses into client JavaScript.
 */

import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/lib/api-client";
import { fetchNotifications } from "@/lib/catalog";
import { formatRelative } from "@/lib/format";
import { notificationTypeLabel } from "@/lib/status";
import { BackendOfflineNotice } from "@/components/BackendOfflineNotice";
import { LogoutButton } from "@/components/LogoutButton";
import { ProfileForm } from "@/components/ProfileForm";
import { Badge } from "@/components/ui/Badge";
import { Card, CardContent, CardHeader, CardTitle, DetailRow } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { BellIcon } from "@/components/icons";

export const metadata: Metadata = {
  title: "Akun Saya",
  description: "Kelola profil dan lihat notifikasi pesanan Anda.",
  robots: { index: false, follow: false },
};

export default async function AccountPage() {
  const user = await getCurrentUser();
  if (!user) redirect("/login?redirect=%2Faccount");

  // Two independent private reads -> fire them together.
  const notifications = await fetchNotifications();
  const unreadCount = notifications.data.filter((item) => !item.read).length;

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-2">
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Akun saya</h1>
          <p className="text-sm text-foreground/65">
            Kelola data profil dan pantau notifikasi terkait pesanan Anda.
          </p>
        </div>
        <LogoutButton />
      </header>

      {notifications.demo ? <BackendOfflineNotice /> : null}

      <div className="grid gap-6 lg:grid-cols-[1fr_1fr] lg:items-start">
        <Card>
          <CardHeader>
            <CardTitle>Profil</CardTitle>
            <Badge tone="brand">{user.roles.join(", ") || "CUSTOMER"}</Badge>
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <dl className="divide-y divide-border-subtle">
              <DetailRow label="ID pengguna" value={<span className="font-mono text-xs">{String(user.id)}</span>} />
              <DetailRow label="Bergabung" value={formatRelative(user.createdAt)} />
            </dl>

            <ProfileForm user={user} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Notifikasi</CardTitle>
            {unreadCount > 0 ? <Badge tone="warning">{unreadCount} belum dibaca</Badge> : <Badge tone="neutral">Semua terbaca</Badge>}
          </CardHeader>
          <CardContent>
            {notifications.data.length === 0 ? (
              <EmptyState
                icon={<BellIcon className="h-5 w-5" />}
                title="Belum ada notifikasi"
                description="Kabar tentang pesanan dan pembayaran Anda akan muncul di sini."
                className="border-0 bg-transparent py-6"
              />
            ) : (
              <ul className="flex flex-col divide-y divide-border-subtle">
                {notifications.data.map((item) => (
                  <li key={String(item.id)} className="flex flex-col gap-1.5 py-4 first:pt-0 last:pb-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge tone={item.read ? "neutral" : "brand"}>{notificationTypeLabel(item.type)}</Badge>
                      <span className="text-xs text-foreground/50">{formatRelative(item.createdAt)}</span>
                      {!item.read ? <span className="h-2 w-2 rounded-full bg-brand" aria-label="Belum dibaca" /> : null}
                    </div>
                    <p className="text-sm font-medium">{item.subject}</p>
                    <p className="text-sm text-foreground/70">{item.body}</p>
                    {item.orderNumber ? (
                      <Link
                        href={`/orders/${item.orderNumber}`}
                        className="w-fit rounded text-sm font-medium text-brand underline underline-offset-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                      >
                        Lihat pesanan {item.orderNumber}
                      </Link>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
