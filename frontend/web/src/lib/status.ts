/**
 * status.ts — Maps backend enums to human labels (Indonesian) and visual tones.
 *
 * Keeping this in one place means a new status added on the backend shows up as a single
 * edit here, and the UI never invents its own wording for a domain concept.
 */

import type { OrderStatus, PaymentStatus } from "./types";

export type Tone = "neutral" | "success" | "warning" | "danger" | "brand";

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "Menunggu Pembayaran",
  PAID: "Dibayar",
  PAYMENT_FAILED: "Pembayaran Gagal",
  CANCELLED: "Dibatalkan",
  FULFILLED: "Selesai",
};

export const ORDER_STATUS_TONES: Record<OrderStatus, Tone> = {
  PENDING_PAYMENT: "warning",
  PAID: "success",
  PAYMENT_FAILED: "danger",
  CANCELLED: "neutral",
  FULFILLED: "brand",
};

const ORDER_STATUS_HINTS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "Selesaikan pembayaran agar pesanan segera diproses.",
  PAID: "Pembayaran diterima. Pesanan sedang disiapkan.",
  PAYMENT_FAILED: "Pembayaran tidak berhasil. Silakan buat pesanan baru.",
  CANCELLED: "Pesanan ini sudah dibatalkan.",
  FULFILLED: "Pesanan sudah diterima pelanggan.",
};

/** Human label for an order status; unknown values are prettified instead of crashing. */
export function orderStatusLabel(status: OrderStatus | string): string {
  return ORDER_STATUS_LABELS[status as OrderStatus] ?? prettifyEnum(status);
}

export function orderStatusTone(status: OrderStatus | string): Tone {
  return ORDER_STATUS_TONES[status as OrderStatus] ?? "neutral";
}

export function orderStatusHint(status: OrderStatus | string): string {
  return ORDER_STATUS_HINTS[status as OrderStatus] ?? "";
}

/** An order can only be cancelled while we are still waiting for the money. */
export function canCancelOrder(status: OrderStatus | string): boolean {
  return status === "PENDING_PAYMENT";
}

export const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Menunggu Pembayaran",
  SUCCEEDED: "Berhasil",
  SETTLED: "Berhasil",
  FAILED: "Gagal",
  EXPIRED: "Kedaluwarsa",
  REFUNDED: "Dana Dikembalikan",
  CANCELLED: "Dibatalkan",
};

export const PAYMENT_STATUS_TONES: Record<string, Tone> = {
  PENDING: "warning",
  SUCCEEDED: "success",
  SETTLED: "success",
  FAILED: "danger",
  EXPIRED: "danger",
  REFUNDED: "brand",
  CANCELLED: "neutral",
};

export function paymentStatusLabel(status: PaymentStatus | null | undefined): string {
  if (!status) return "Belum ada pembayaran";
  return PAYMENT_STATUS_LABELS[status] ?? prettifyEnum(status);
}

export function paymentStatusTone(status: PaymentStatus | null | undefined): Tone {
  if (!status) return "neutral";
  return PAYMENT_STATUS_TONES[status] ?? "neutral";
}

const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  ORDER_CREATED: "Pesanan dibuat",
  ORDER_PAID: "Pembayaran diterima",
  PAYMENT_SUCCEEDED: "Pembayaran berhasil",
  PAYMENT_FAILED: "Pembayaran gagal",
  ORDER_CANCELLED: "Pesanan dibatalkan",
  ORDER_FULFILLED: "Pesanan selesai",
  WELCOME: "Selamat datang",
};

export function notificationTypeLabel(type: string): string {
  return NOTIFICATION_TYPE_LABELS[type] ?? prettifyEnum(type);
}

/** "PENDING_PAYMENT" -> "Pending payment" (last-resort display for unmapped enum values). */
export function prettifyEnum(value: string): string {
  const words = value.replace(/[_-]+/g, " ").toLowerCase().trim();
  if (!words) return "—";
  return words.charAt(0).toUpperCase() + words.slice(1);
}
