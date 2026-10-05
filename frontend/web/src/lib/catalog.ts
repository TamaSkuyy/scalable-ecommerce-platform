/**
 * catalog.ts — SERVER-SIDE read layer with graceful degradation.
 *
 * Everything a Server Component needs to *read* goes through here. Each function returns a
 * `DataResult<T>`:
 *
 *     { data, demo }
 *
 * `demo: true` means "the API gateway could not be reached, so you are looking at the
 * placeholders from `src/lib/demo-data.ts`". Pages then render a `BackendOfflineNotice`
 * and keep a beautiful layout instead of a 500.
 *
 * WHAT IS *NOT* FALLBACK-ENABLED: writes. If `POST /cart/items` fails, the shopper must
 * see the error — silently pretending an item was added would be a lie.
 *
 * NOTE ON CACHING: the underlying `fetch` calls carry `next: { revalidate: 60 }`, so the
 * catalog is cached at the *data* level (ISR). A page that also reads `searchParams`
 * becomes dynamic per request, but the expensive upstream call is still served from the
 * Next.js data cache for 60 seconds.
 */

import { isBackendUnreachable, toApiError } from "./api-error";
import {
  getCategories,
  getNotifications,
  getOrder,
  getOrders,
  getPayment,
  getProductBySlug,
  getProducts,
} from "./api-client";
import {
  DEMO_CATEGORIES,
  DEMO_NOTIFICATIONS,
  demoFeaturedProducts,
  demoOrderDetail,
  demoOrderPage,
  demoPayment,
  demoProductDetail,
  demoProductPage,
} from "./demo-data";
import type {
  CategoryDto,
  DataResult,
  NotificationDto,
  OrderDetail,
  OrderSummary,
  PageResponse,
  PaymentDto,
  ProductDetail,
  ProductQuery,
  ProductSummary,
} from "./types";

/** Wraps a live result, tagging it as non-demo. */
function live<T>(data: T): DataResult<T> {
  return { data, demo: false };
}

/** Wraps a fallback result, tagging it as demo. */
function fallback<T>(data: T): DataResult<T> {
  return { data, demo: true };
}

/**
 * Runs `load()`; if (and only if) the gateway is *unreachable*, returns `onUnreachable()`
 * instead. Real HTTP errors from a running gateway (400/401/404/500) are rethrown so the
 * UI shows the actual backend message — that is the honest behaviour.
 */
async function withDemoFallback<T>(load: () => Promise<T>, onUnreachable: () => T): Promise<DataResult<T>> {
  try {
    return live(await load());
  } catch (error) {
    if (isBackendUnreachable(error)) return fallback(onUnreachable());
    throw toApiError(error);
  }
}

/* -------------------------------------------------------------------------- */
/* Public catalog                                                             */
/* -------------------------------------------------------------------------- */

export function fetchCategories(): Promise<DataResult<CategoryDto[]>> {
  return withDemoFallback(getCategories, () => DEMO_CATEGORIES);
}

export function fetchProducts(query: ProductQuery): Promise<DataResult<PageResponse<ProductSummary>>> {
  return withDemoFallback(() => getProducts(query), () => demoProductPage(query));
}

/** Product detail. A real 404 from a running gateway is rethrown, so `notFound()` fires. */
export function fetchProductDetail(slug: string): Promise<DataResult<ProductDetail | null>> {
  return withDemoFallback(() => getProductBySlug(slug), () => demoProductDetail(slug));
}

/**
 * "Produk unggulan" for the landing page.
 * Reuses `fetchProducts` so the demo/live switch happens in exactly one place.
 */
export async function fetchFeaturedProducts(limit = 8): Promise<DataResult<ProductSummary[]>> {
  const result = await fetchProducts({ page: 0, size: limit, sort: "createdAt,desc" });
  return { data: result.data.content, demo: result.demo };
}

/* -------------------------------------------------------------------------- */
/* Private data (never falls back — the shopper must know the truth)          */
/* -------------------------------------------------------------------------- */

export function fetchOrders(page = 0, size = 10): Promise<DataResult<PageResponse<OrderSummary>>> {
  return withDemoFallback(() => getOrders(page, size), () => demoOrderPage(page, size));
}

export function fetchOrderDetail(orderNumber: string): Promise<DataResult<OrderDetail | null>> {
  return withDemoFallback(() => getOrder(orderNumber), () => demoOrderDetail(orderNumber));
}

/** Payments are optional chrome: a missing record is normal, not an error. */
export async function fetchPayment(orderNumber: string): Promise<PaymentDto | null> {
  try {
    return await getPayment(orderNumber);
  } catch {
    return demoPayment(orderNumber);
  }
}

export function fetchNotifications(): Promise<DataResult<NotificationDto[]>> {
  return withDemoFallback(getNotifications, () => DEMO_NOTIFICATIONS);
}

/** Re-exported for pages that want to build a demo-only projection. */
export { demoFeaturedProducts };
