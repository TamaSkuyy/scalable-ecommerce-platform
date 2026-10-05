/**
 * http.ts — CLIENT-SIDE fetch wrapper.
 *
 * ── THE KEY IDEA ──────────────────────────────────────────────────────────────
 * Browser code NEVER talks to the API gateway and never sees a token. It calls
 * **same-origin relative paths** such as `/api/v1/cart`:
 *
 *     Browser ──fetch("/api/v1/cart")──▶ Next.js middleware ──▶ API gateway
 *                                          │  injects Authorization: Bearer <httpOnly cookie>
 *
 * `src/middleware.ts` performs that proxy hop, reading the cookie on the server. That is
 * the entire Backend-for-Frontend trick: the JWT stays in an httpOnly cookie, so an XSS
 * bug cannot exfiltrate it, and no client component needs any auth plumbing.
 *
 * This module is intentionally free of `next/headers` so it is safe in the browser bundle.
 */

import { ApiError } from "./api-error";

/** Requests sent from the browser only ever target this prefix (proxied by the middleware). */
export const API_PREFIX = "/api/v1";

export interface ClientFetchOptions extends Omit<RequestInit, "body"> {
  /** Plain object serialised to JSON, or a pre-built `FormData`/string body. */
  body?: unknown;
}

/**
 * Thin typed `fetch` wrapper for client components.
 * Throws a typed `ApiError` on any non-2xx response.
 */
export async function apiFetch<T>(path: string, options: ClientFetchOptions = {}): Promise<T> {
  const { body, headers, ...init } = options;

  const requestHeaders = new Headers(headers);
  if (!requestHeaders.has("accept")) requestHeaders.set("accept", "application/json");

  let payload: BodyInit | undefined;
  if (body instanceof FormData || typeof body === "string" || body instanceof Blob) {
    payload = body;
  } else if (body !== undefined) {
    requestHeaders.set("content-type", "application/json");
    payload = JSON.stringify(body);
  }

  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: requestHeaders,
      body: payload,
      // Client-side data is always per-session; never let the browser HTTP cache serve it.
      cache: "no-store",
    });
  } catch {
    throw new ApiError("Tidak dapat menghubungi server. Periksa koneksi Anda lalu coba lagi.", 0);
  }

  if (response.status === 204 || response.status === 205) {
    return undefined as T;
  }

  const raw = await response.text();
  let decoded: unknown;
  try {
    decoded = raw ? JSON.parse(raw) : undefined;
  } catch {
    decoded = undefined;
  }

  if (!response.ok) {
    throw ApiError.fromResponse(decoded, response.status);
  }

  return decoded as T;
}
