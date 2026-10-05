"use client";

/**
 * CheckoutForm.tsx — Client Component: shipping address form → create order.
 *
 * FLOW
 *  1. Read the cart (`GET /api/v1/cart`) so the shopper can confirm what they are buying.
 *  2. Validate the address locally — instant feedback, no wasted round trip.
 *  3. `POST /api/v1/orders` with `{ shippingAddress, notes }`.
 *  4. Read the payment record for the new order (`GET /api/v1/payments/{orderNumber}`) and
 *     show its status.
 *  5. `router.push()` to `/orders/{orderNumber}` — the order detail page, which renders the
 *     payment panel server-side.
 *
 * The gateway owns stock, pricing and the address rules; our local validation only exists
 * to avoid obvious mistakes, and the backend's own `violations` are displayed too.
 */

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { cartApi, ordersApi, paymentsApi } from "@/lib/api";
import { toApiError } from "@/lib/api-error";
import { emitCartUpdated } from "@/lib/cart-events";
import { formatMoney } from "@/lib/format";
import { paymentStatusLabel, paymentStatusTone } from "@/lib/status";
import type { CartDto, PaymentDto, ShippingAddressDto } from "@/lib/types";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field, Input, Textarea } from "@/components/ui/Field";
import { ListSkeleton } from "@/components/ui/Skeleton";
import { Price } from "@/components/ui/Price";
import { CartIcon } from "@/components/icons";

type FieldErrors = Partial<Record<keyof ShippingAddressDto | "notes" | "form", string>>;

const EMPTY_ADDRESS: ShippingAddressDto = {
  recipientName: "",
  phone: "",
  line1: "",
  line2: "",
  city: "",
  postalCode: "",
  country: "Indonesia",
};

export function CheckoutForm({ defaultRecipientName = "" }: { defaultRecipientName?: string }) {
  const router = useRouter();

  const [cart, setCart] = useState<CartDto | null>(null);
  const [loadingCart, setLoadingCart] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [address, setAddress] = useState<ShippingAddressDto>({ ...EMPTY_ADDRESS, recipientName: defaultRecipientName });
  const [notes, setNotes] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [payment, setPayment] = useState<PaymentDto | null>(null);
  const [createdOrder, setCreatedOrder] = useState<string | null>(null);

  /** Pure loader — returns the cart, never sets state. See `CartBadge` for the rationale. */
  const fetchCart = useCallback(() => cartApi.get(), []);

  const applyCart = useCallback((data: CartDto) => {
    setCart(data);
    setLoadingCart(false);
    setLoadError(null);
  }, []);

  const applyLoadError = useCallback((cause: unknown) => {
    setLoadError(toApiError(cause).message);
    setLoadingCart(false);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchCart()
      .then((data) => {
        if (!controller.signal.aborted) applyCart(data);
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) applyLoadError(cause);
      });
    return () => controller.abort();
  }, [fetchCart, applyCart, applyLoadError]);

  /** Manual retry from the "Coba lagi" button (an event handler, not an effect). */
  async function reloadCart() {
    setLoadingCart(true);
    try {
      applyCart(await fetchCart());
    } catch (cause) {
      applyLoadError(cause);
    }
  }

  // After the order exists, give the shopper a moment to read the payment status before
  // moving them to the order detail page.
  useEffect(() => {
    if (!createdOrder) return;
    const timer = setTimeout(() => router.push(`/orders/${createdOrder}`), 1500);
    return () => clearTimeout(timer);
  }, [createdOrder, router]);

  function update<K extends keyof ShippingAddressDto>(key: K, value: string) {
    setAddress((previous) => ({ ...previous, [key]: value }));
    setErrors((previous) => ({ ...previous, [key]: undefined, form: undefined }));
  }

  function validate(): FieldErrors {
    const next: FieldErrors = {};
    if (address.recipientName.trim().length < 3) next.recipientName = "Nama penerima minimal 3 karakter.";
    if (!/^[+()\-\s\d]{9,20}$/.test(address.phone.trim())) next.phone = "Nomor telepon tidak valid (9–20 digit).";
    if (address.line1.trim().length < 5) next.line1 = "Alamat minimal 5 karakter.";
    if (address.city.trim().length < 2) next.city = "Kota wajib diisi.";
    if (!/^\d{4,8}$/.test(address.postalCode.trim())) next.postalCode = "Kode pos harus 4–8 digit angka.";
    if (address.country.trim().length < 3) next.country = "Negara wajib diisi.";
    return next;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validation = validate();
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }

    setSubmitting(true);
    setErrors({});
    try {
      const order = await ordersApi.create({
        shippingAddress: {
          ...address,
          recipientName: address.recipientName.trim(),
          phone: address.phone.trim(),
          line1: address.line1.trim(),
          line2: address.line2?.trim() || undefined,
          city: address.city.trim(),
          postalCode: address.postalCode.trim(),
          country: address.country.trim(),
        },
        notes: notes.trim() || undefined,
      });

      // The cart is emptied by the order service; refresh the badge.
      emitCartUpdated();

      // Payment status is best-effort: the order exists even if this read fails.
      try {
        setPayment(await paymentsApi.byOrder(order.orderNumber));
      } catch {
        setPayment(null);
      }

      setCreatedOrder(order.orderNumber);
    } catch (cause) {
      const apiError = toApiError(cause);
      if (apiError.isUnauthorized) {
        router.push("/login?redirect=%2Fcheckout");
        return;
      }
      // Map bean-validation violations (e.g. "shippingAddress.phone") onto our fields.
      const fieldErrors: FieldErrors = {};
      for (const violation of apiError.violations) {
        const key = violation.field.split(".").pop() as keyof FieldErrors;
        if (key) fieldErrors[key] = violation.message;
      }
      setErrors({ ...fieldErrors, form: Object.keys(fieldErrors).length ? undefined : apiError.message });
    } finally {
      setSubmitting(false);
    }
  }

  /* ----------------------------- Success state ----------------------------- */
  if (createdOrder) {
    return (
      <Card className="flex flex-col gap-4 p-6">
        <div className="flex flex-col gap-1">
          <h2 className="text-lg font-semibold">Pesanan berhasil dibuat</h2>
          <p className="text-sm text-foreground/65">
            Nomor pesanan <span className="font-semibold text-foreground">{createdOrder}</span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 rounded-xl border border-border-subtle bg-surface-muted/50 p-4">
          <span className="text-sm font-medium">Status pembayaran</span>
          <Badge tone={paymentStatusTone(payment?.status)}>{paymentStatusLabel(payment?.status)}</Badge>
          {payment ? (
            <span className="text-sm text-foreground/65">
              {formatMoney(payment.amountCents, payment.currency)} · {payment.method}
            </span>
          ) : (
            <span className="text-sm text-foreground/65">Belum ada data pembayaran.</span>
          )}
        </div>

        <p className="text-sm text-foreground/60">Mengalihkan ke halaman detail pesanan…</p>

        <div className="flex flex-wrap gap-3">
          <Link href={`/orders/${createdOrder}`}>
            <Button>Lihat detail pesanan</Button>
          </Link>
          <Link href="/products">
            <Button variant="outline">Lanjut belanja</Button>
          </Link>
        </div>
      </Card>
    );
  }

  /* ------------------------------ Cart states ------------------------------ */
  if (loadingCart) {
    return (
      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <ListSkeleton rows={3} />
        <Card className="h-40 animate-pulse" />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="flex flex-col gap-4">
        <Callout tone="danger" title="Gagal memuat keranjang">
          {loadError}
        </Callout>
        <div>
          <Button variant="outline" onClick={() => void reloadCart()}>
            Coba lagi
          </Button>
        </div>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <EmptyState
        icon={<CartIcon className="h-5 w-5" />}
        title="Keranjang kosong"
        description="Tambahkan minimal satu produk sebelum melakukan checkout."
        action={
          <Link href="/products">
            <Button>Lihat katalog produk</Button>
          </Link>
        }
      />
    );
  }

  /* --------------------------------- Form --------------------------------- */
  return (
    <form onSubmit={handleSubmit} noValidate className="grid gap-6 lg:grid-cols-[1fr_20rem] lg:items-start">
      <div className="flex flex-col gap-4">
        {errors.form ? (
          <Callout tone="danger" title="Checkout gagal">
            {errors.form}
          </Callout>
        ) : null}

        <Card>
          <CardHeader>
            <CardTitle>Alamat pengiriman</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <Field label="Nama penerima" htmlFor="recipientName" required error={errors.recipientName} className="sm:col-span-2">
              <Input
                id="recipientName"
                name="recipientName"
                autoComplete="name"
                value={address.recipientName}
                invalid={Boolean(errors.recipientName)}
                aria-describedby={errors.recipientName ? "recipientName-error" : undefined}
                onChange={(event) => update("recipientName", event.target.value)}
                placeholder="Nama lengkap penerima"
              />
            </Field>

            <Field label="Nomor telepon" htmlFor="phone" required error={errors.phone}>
              <Input
                id="phone"
                name="phone"
                type="tel"
                autoComplete="tel"
                value={address.phone}
                invalid={Boolean(errors.phone)}
                onChange={(event) => update("phone", event.target.value)}
                placeholder="081234567890"
              />
            </Field>

            <Field label="Kode pos" htmlFor="postalCode" required error={errors.postalCode}>
              <Input
                id="postalCode"
                name="postalCode"
                inputMode="numeric"
                autoComplete="postal-code"
                value={address.postalCode}
                invalid={Boolean(errors.postalCode)}
                onChange={(event) => update("postalCode", event.target.value)}
                placeholder="10310"
              />
            </Field>

            <Field label="Alamat lengkap" htmlFor="line1" required error={errors.line1} className="sm:col-span-2">
              <Input
                id="line1"
                name="line1"
                autoComplete="address-line1"
                value={address.line1}
                invalid={Boolean(errors.line1)}
                onChange={(event) => update("line1", event.target.value)}
                placeholder="Jl. Merdeka No. 12, RT 02 / RW 05"
              />
            </Field>

            <Field label="Alamat tambahan" htmlFor="line2" hint="Opsional — patokan, nama gedung, dsb." className="sm:col-span-2">
              <Input
                id="line2"
                name="line2"
                autoComplete="address-line2"
                value={address.line2 ?? ""}
                onChange={(event) => update("line2", event.target.value)}
                placeholder="Kelurahan, kecamatan, patokan"
              />
            </Field>

            <Field label="Kota / Kabupaten" htmlFor="city" required error={errors.city}>
              <Input
                id="city"
                name="city"
                autoComplete="address-level2"
                value={address.city}
                invalid={Boolean(errors.city)}
                onChange={(event) => update("city", event.target.value)}
                placeholder="Jakarta Pusat"
              />
            </Field>

            <Field label="Negara" htmlFor="country" required error={errors.country}>
              <Input
                id="country"
                name="country"
                autoComplete="country-name"
                value={address.country}
                invalid={Boolean(errors.country)}
                onChange={(event) => update("country", event.target.value)}
              />
            </Field>

            <Field label="Catatan untuk penjual" htmlFor="notes" hint="Opsional" className="sm:col-span-2">
              <Textarea
                id="notes"
                name="notes"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Contoh: tolong dibungkus bubble wrap."
              />
            </Field>
          </CardContent>
        </Card>

        <div className="flex flex-wrap gap-3">
          <Button type="submit" size="lg" loading={submitting}>
            Buat pesanan
          </Button>
          <Link href="/cart">
            <Button type="button" variant="outline" size="lg">
              Kembali ke keranjang
            </Button>
          </Link>
        </div>
      </div>

      <Card className="flex flex-col gap-4 p-5 lg:sticky lg:top-24">
        <h2 className="text-base font-semibold">Ringkasan pesanan</h2>

        <ul className="flex flex-col gap-3">
          {cart.items.map((item) => (
            <li key={String(item.productId)} className="flex items-start justify-between gap-3 text-sm">
              <span className="min-w-0">
                <span className="line-clamp-2 font-medium">{item.name}</span>
                <span className="mt-0.5 block text-xs text-foreground/55">
                  {item.quantity} × {formatMoney(item.unitPriceCents, cart.currency)}
                </span>
              </span>
              <Price cents={item.lineTotalCents} currency={cart.currency} size="sm" />
            </li>
          ))}
        </ul>

        <div className="flex items-center justify-between border-t border-border-subtle pt-3">
          <span className="text-sm font-semibold">Total ({cart.itemCount} item)</span>
          <Price cents={cart.subtotalCents} currency={cart.currency} size="lg" />
        </div>

        <p className="text-xs text-foreground/55">
          Total akhir dihitung ulang oleh order service saat pesanan dibuat, termasuk ongkos kirim bila ada.
        </p>
      </Card>
    </form>
  );
}
