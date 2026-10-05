/**
 * api-error.ts — One error type shared by the server API client, the BFF route
 * handlers and the browser fetch wrapper.
 *
 * WHY A CUSTOM ERROR?
 * `fetch` only tells you "not ok" via a boolean. The gateway, however, already sends a
 * rich `ErrorEnvelope` ({ message, violations, traceId, ... }). We parse it once, wrap it
 * in `ApiError`, and every layer above can then simply read `error.message` to show the
 * shopper exactly what the backend said — instead of a generic "something went wrong".
 */

import type { ErrorEnvelope, FieldViolation } from "./types";

/** Shown whenever the gateway cannot be reached at all (connection refused, DNS, timeout...). */
export const BACKEND_OFFLINE_MESSAGE =
  "Backend belum berjalan. Menampilkan data contoh (demo) agar halaman tetap bisa dilihat.";

export class ApiError extends Error {
  /** HTTP status code, or 0 when the request never reached the server. */
  readonly status: number;
  /** The parsed error envelope, when the gateway sent one. */
  readonly envelope?: ErrorEnvelope;
  /** Bean-validation failures, ready to be mapped onto individual form fields. */
  readonly violations: FieldViolation[];
  readonly traceId?: string;

  constructor(message: string, status: number, envelope?: ErrorEnvelope) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.envelope = envelope;
    this.violations = envelope?.violations ?? [];
    this.traceId = envelope?.traceId;
    // Required so `instanceof ApiError` keeps working after TS downlevels the class.
    Object.setPrototypeOf(this, ApiError.prototype);
  }

  /** The request never made it to the gateway (offline, DNS failure, connection refused). */
  get isNetworkError(): boolean {
    return this.status === 0;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }

  /** Builds an `ApiError` from a decoded error envelope. */
  static fromEnvelope(envelope: ErrorEnvelope): ApiError {
    return new ApiError(
      envelope.message || envelope.error || "Terjadi kesalahan pada server.",
      envelope.status ?? 500,
      envelope,
    );
  }

  /**
   * Builds an `ApiError` from whatever the response happened to contain.
   * The gateway normally answers with a full envelope; when it does not (a proxy, a
   * crash, an HTML error page) we still want a readable Indonesian message.
   */
  static fromResponse(body: unknown, status: number): ApiError {
    if (isErrorEnvelope(body)) return ApiError.fromEnvelope(body);
    return new ApiError(defaultMessageForStatus(status), status);
  }

  /** The gateway is unreachable — used by the demo/fallback paths. */
  static network(message: string = BACKEND_OFFLINE_MESSAGE): ApiError {
    return new ApiError(message, 0);
  }
}

/** Narrowing helper: safe to call with an `unknown` caught value. */
export function isApiError(value: unknown): value is ApiError {
  return value instanceof ApiError;
}

/** Normalises any thrown value into an `ApiError` so callers never deal with raw `unknown`. */
export function toApiError(value: unknown): ApiError {
  if (isApiError(value)) return value;
  if (value instanceof Error) return new ApiError(value.message, 0);
  return new ApiError("Terjadi kesalahan yang tidak terduga.", 0);
}

/** True when the failure means "the gateway is down / unreachable", not "the gateway said no". */
export function isBackendUnreachable(value: unknown): boolean {
  const error = toApiError(value);
  return error.isNetworkError || error.status === 502 || error.status === 503 || error.status === 504;
}

/** Runtime guard for the gateway's error envelope. */
export function isErrorEnvelope(value: unknown): value is ErrorEnvelope {
  if (typeof value !== "object" || value === null) return false;
  const candidate = value as Record<string, unknown>;
  return typeof candidate.message === "string" && typeof candidate.status === "number";
}

function defaultMessageForStatus(status: number): string {
  switch (status) {
    case 400:
      return "Permintaan tidak valid.";
    case 401:
      return "Sesi Anda sudah berakhir. Silakan masuk kembali.";
    case 403:
      return "Anda tidak memiliki akses ke sumber daya ini.";
    case 404:
      return "Data yang diminta tidak ditemukan.";
    case 409:
      return "Terjadi konflik data. Silakan coba lagi.";
    case 422:
      return "Data yang dikirim tidak lolos validasi.";
    case 429:
      return "Terlalu banyak permintaan. Coba lagi beberapa saat.";
    default:
      if (status >= 500) return "Server sedang bermasalah. Silakan coba lagi.";
      return "Terjadi kesalahan pada permintaan.";
  }
}
