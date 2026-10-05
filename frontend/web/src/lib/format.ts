/**
 * format.ts — Presentation helpers (money + dates).
 *
 * MONEY RULE: the API always sends **integer minor units** (sen for IDR). We divide by
 * 100 exactly once, right here, and hand the result to `Intl.NumberFormat('id-ID', ...)`.
 * Nothing else in the app should ever divide a price.
 */

/** Default currency used when the API omits one. */
export const DEFAULT_CURRENCY = "IDR";

/**
 * Formats an integer amount of minor units as a localized currency string.
 *
 * ```ts
 * formatMoney(14900000) // "Rp 149.000"
 * formatMoney(1999, "USD") // "US$19,99"
 * ```
 *
 * IDR has no practical use for decimals in a storefront, so we drop the fraction digits
 * for currencies whose smallest coin is not used in pricing (IDR, JPY, KRW).
 */
export function formatMoney(minorUnits: number | null | undefined, currency: string = DEFAULT_CURRENCY): string {
  const amount = typeof minorUnits === "number" && Number.isFinite(minorUnits) ? minorUnits : 0;
  const code = (currency || DEFAULT_CURRENCY).toUpperCase();
  const zeroDecimal = code === "IDR" || code === "JPY" || code === "KRW";

  try {
    return new Intl.NumberFormat("id-ID", {
      style: "currency",
      currency: code,
      minimumFractionDigits: zeroDecimal ? 0 : 2,
      maximumFractionDigits: zeroDecimal ? 0 : 2,
    }).format(amount / 100);
  } catch {
    // Unknown currency code — fall back to a plain number so the UI never crashes.
    return `Rp ${(amount / 100).toLocaleString("id-ID")}`;
  }
}

/** "12 Mar 2025, 14.30" — Indonesian locale, safe for SSR (fixed time zone to avoid hydration drift). */
export function formatDateTime(value: string | Date | null | undefined): string {
  const date = toDate(value);
  if (!date) return "—";
  return new Intl.DateTimeFormat("id-ID", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Jakarta",
  }).format(date);
}

/** "12 Mar 2025" */
export function formatDate(value: string | Date | null | undefined): string {
  const date = toDate(value);
  if (!date) return "—";
  return new Intl.DateTimeFormat("id-ID", {
    dateStyle: "medium",
    timeZone: "Asia/Jakarta",
  }).format(date);
}

/** Relative-ish label used in lists: "Hari ini", "Kemarin", or an absolute date. */
export function formatRelative(value: string | Date | null | undefined): string {
  const date = toDate(value);
  if (!date) return "—";
  const diffDays = Math.floor((Date.now() - date.getTime()) / 86_400_000);
  if (diffDays <= 0) return "Hari ini";
  if (diffDays === 1) return "Kemarin";
  if (diffDays < 7) return `${diffDays} hari lalu`;
  return formatDate(date);
}

/** Strips a leading "/" and enumerates an unknown-ish identifier for display. */
export function truncate(value: string, max = 80): string {
  if (value.length <= max) return value;
  return `${value.slice(0, max - 1)}…`;
}

function toDate(value: string | Date | null | undefined): Date | null {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
