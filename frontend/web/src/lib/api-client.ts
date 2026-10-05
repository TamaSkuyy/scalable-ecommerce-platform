/**
 * api-client.ts — SERVER-ONLY gateway client used by Server Components and BFF route handlers.
 *
 * ── WHY THIS FILE MUST NOT BE IMPORTED BY A CLIENT COMPONENT ──────────────────
 * It reads the JWT out of the httpOnly cookie via `next/headers`. `next/headers` is a
 * server-only API; if a client component ever imported this module the build would fail
 * loudly (which is the point — it is a guard rail, not a limitation).
 *
 * ── HOW AUTH WORKS HERE ───────────────────────────────────────────────────────
 *  1. The browser holds `shopflow_token` as an httpOnly cookie (JavaScript cannot read it).
 *  2. A Server Component calls `apiFetchServer(path, { auth: true })`.
 *  3. We pull the token from `cookies()` and attach `Authorization: Bearer <token>`.
 *  4. The gateway validates the JWT and answers with the user-specific payload.
 *
 * ── CACHING ───────────────────────────────────────────────────────────────────
 *  - Public catalog  -> `next: { revalidate: 60 }`  (ISR, shared across all visitors)
 *  - Anything user-specific -> `cache: 'no-store'`  (never cached, never shared)
 *  Next.js refuses to set both at once, so the helper picks exactly one.
 */

import { cookies } from "next/headers";

import { ApiError, isBackendUnreachable } from "./api-error";
import { API_BASE_URL, API_PREFIX, TOKEN_COOKIE } from "./config";
import type {
  CartDto,
  CategoryDto,
  NotificationDto,
  OrderDetail,
  OrderSummary,
  PageResponse,
  PaymentDto,
  ProductDetail,
  ProductQuery,
  ProductSummary,
  UserDto,
} from "./types";

export interface ServerFetchOptions extends Omit<RequestInit, "cache"> {
  /** Attach `Authorization: Bearer <cookie>`. */
  auth?: boolean;
  /** ISR window in seconds. Mutually exclusive with `cache: 'no-store'`. */
  revalidate?: number | false;
  /** Cache tags for on-demand revalidation. */
  tags?: string[];
  /** Explicit Next.js fetch cache policy; defaults to `no-store` for private data. */
  cache?: RequestCache;
}

/** Reads the JWT from the httpOnly cookie. Returns `undefined` when the shopper is anonymous. */
export async function getAuthToken(): Promise<string | undefined> {
  const store = await cookies();
  return store.get(TOKEN_COOKIE)?.value;
}

/** Builds an absolute gateway URL. Accepts either `/products` or a full URL. */
export function gatewayUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${API_PREFIX}${suffix}`;
}

/**
 * The one and only server-side gateway call.
 *
 * Throws a typed `ApiError` carrying the parsed error envelope, so callers can render
 * `error.message` directly to the shopper.
 */
export async function apiFetchServer<T>(path: string, options: ServerFetchOptions = {}): Promise<T> {
  const { auth = false, revalidate, tags, cache, headers, ...init } = options;

  const requestHeaders = new Headers(headers);
  if (!requestHeaders.has("accept")) requestHeaders.set("accept", "application/json");
  if (init.body !== undefined && !requestHeaders.has("content-type")) {
    requestHeaders.set("content-type", "application/json");
  }

  if (auth) {
    const token = await getAuthToken();
    if (!token) {
      throw new ApiError("Anda belum masuk. Silakan login terlebih dahulu.", 401);
    }
    requestHeaders.set("authorization", `Bearer ${token}`);
  }

  // Next.js rejects `cache` combined with `next.revalidate`, so build the fetch options
  // carefully: a revalidate window means "cacheable", everything else means "no-store".
  const useIsr = revalidate !== undefined && revalidate !== false;
  const nextOptions =
    useIsr || tags?.length
      ? { ...(useIsr ? { revalidate: revalidate as number } : {}), ...(tags?.length ? { tags } : {}) }
      : undefined;

  let response: Response;
  try {
    response = await fetch(gatewayUrl(path), {
      ...init,
      headers: requestHeaders,
      ...(nextOptions ? { next: nextOptions } : { cache: cache ?? "no-store" }),
    });
  } catch (cause) {
    // `fetch` throws only for transport-level failures: the gateway is not running,
    // DNS failed, TLS failed, the connection was reset...
    throw new ApiError(
      cause instanceof Error ? `Tidak dapat menghubungi API gateway (${API_BASE_URL}).` : "Gagal menghubungi API gateway.",
      0,
    );
  }

  return parseResponse<T>(response);
}

/**
 * Like `apiFetchServer`, but returns `null` instead of throwing.
 * Use it for *optional* chrome (a header badge, a "signed in as ..." label) where a
 * failure should degrade the UI instead of blowing up the whole route.
 */
export async function apiFetchServerSafe<T>(
  path: string,
  options: ServerFetchOptions = {},
): Promise<T | null> {
  try {
    return await apiFetchServer<T>(path, options);
  } catch {
    return null;
  }
}

/* -------------------------------------------------------------------------- */
/* Typed server-side gateway calls                                            */
/* -------------------------------------------------------------------------- */

/** `GET /users/me`. Returns `null` when anonymous; rethrows other failures. */
export async function getCurrentUser(): Promise<UserDto | null> {
  try {
    return await apiFetchServer<UserDto>("/users/me", { auth: true, cache: "no-store" });
  } catch (error) {
    if (error instanceof ApiError && (error.isUnauthorized || error.isNetworkError)) return null;
    throw error;
  }
}

/** `GET /cart` — private, never cached. */
export function getCart(): Promise<CartDto> {
  return apiFetchServer<CartDto>("/cart", { auth: true, cache: "no-store" });
}

/** `GET /orders` — private, never cached. */
export function getOrders(page = 0, size = 10): Promise<PageResponse<OrderSummary>> {
  return apiFetchServer<PageResponse<OrderSummary>>(`/orders?page=${page}&size=${size}`, {
    auth: true,
    cache: "no-store",
  });
}

/** `GET /orders/{orderNumber}` */
export function getOrder(orderNumber: string): Promise<OrderDetail> {
  return apiFetchServer<OrderDetail>(`/orders/${encodeURIComponent(orderNumber)}`, {
    auth: true,
    cache: "no-store",
  });
}

/** `GET /payments/{orderNumber}` — optional, so `apiFetchServerSafe` is the right tool. */
export function getPayment(orderNumber: string): Promise<PaymentDto | null> {
  return apiFetchServerSafe<PaymentDto>(`/payments/${encodeURIComponent(orderNumber)}`, {
    auth: true,
    cache: "no-store",
  });
}

/** `GET /notifications/me` */
export function getNotifications(): Promise<NotificationDto[]> {
  return apiFetchServer<NotificationDto[]>("/notifications/me", { auth: true, cache: "no-store" });
}

/** `GET /categories` — public + ISR. */
export function getCategories(): Promise<CategoryDto[]> {
  return apiFetchServer<CategoryDto[]>("/categories", { revalidate: 60, tags: ["categories"] });
}

/** `GET /products` — public + ISR. */
export function getProducts(query: ProductQuery = {}): Promise<PageResponse<ProductSummary>> {
  return apiFetchServer<PageResponse<ProductSummary>>(`/products${toProductQueryString(query)}`, {
    revalidate: 60,
    tags: ["products"],
  });
}

/** `GET /products/{slug}` — public + ISR. */
export function getProductBySlug(slug: string): Promise<ProductDetail> {
  return apiFetchServer<ProductDetail>(`/products/${encodeURIComponent(slug)}`, {
    revalidate: 60,
    tags: ["products", `product:${slug}`],
  });
}

/** Serialises a `ProductQuery` into a query string, dropping empty values. */
export function toProductQueryString(query: ProductQuery): string {
  const params = new URLSearchParams();
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 12));
  if (query.q) params.set("q", query.q);
  if (query.category) params.set("category", query.category);
  if (query.sort) params.set("sort", query.sort);
  return `?${params.toString()}`;
}

/**
 * Shared response decoding for every caller (server client + BFF route handlers).
 * Handles `204 No Content`, tolerates non-JSON bodies, and turns the gateway's error
 * envelope into an `ApiError`.
 */
export async function parseResponse<T>(response: Response): Promise<T> {
  if (response.status === 204 || response.status === 205) {
    return undefined as T;
  }

  const raw = await response.text();
  const payload = raw ? tryParseJson(raw) : undefined;

  if (!response.ok) {
    throw ApiError.fromResponse(payload, response.status);
  }

  return payload as T;
}

function tryParseJson(raw: string): unknown {
  try {
    return JSON.parse(raw);
  } catch {
    return undefined;
  }
}

/** Re-exported so route handlers can reuse the exact same fallback predicate. */
export { isBackendUnreachable };
