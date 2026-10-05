/**
 * types.ts — Single source of truth for every DTO exposed by the ShopFlow API gateway.
 *
 * These types mirror the backend contract **exactly**. Keeping them in one module means
 * the Server Components, the BFF route handlers and the client components all agree on
 * the same shapes, and a contract change breaks the build instead of production.
 *
 * CONTRACT NOTES
 * - Every monetary amount is an **integer in minor units** (sen for IDR, cents for USD).
 *   Never do arithmetic on the formatted string; always convert at the edge with `formatMoney`.
 * - Every endpoint can fail with the same `ErrorEnvelope` (see below).
 */

/* -------------------------------------------------------------------------- */
/* Errors                                                                     */
/* -------------------------------------------------------------------------- */

/** A single bean-validation failure, e.g. `{ field: "email", message: "must be a well-formed email address" }`. */
export interface FieldViolation {
  field: string;
  message: string;
}

/**
 * The error envelope returned by *every* gateway endpoint.
 * `message` is what we surface to the shopper; `violations` drives inline field errors.
 */
export interface ErrorEnvelope {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  traceId?: string;
  violations?: FieldViolation[] | null;
}

/* -------------------------------------------------------------------------- */
/* Generic paging                                                             */
/* -------------------------------------------------------------------------- */

/** Spring Data style page wrapper returned by all list endpoints. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/* -------------------------------------------------------------------------- */
/* Auth & users                                                               */
/* -------------------------------------------------------------------------- */

export interface UserDto {
  id: number | string;
  email: string;
  fullName: string;
  roles: string[];
  createdAt?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Raw gateway login payload. The `accessToken` NEVER reaches client JavaScript — see `src/app/api/auth/login/route.ts`. */
export interface LoginResponse {
  accessToken: string;
  tokenType: "Bearer" | string;
  expiresInSeconds: number;
  user: UserDto;
}

export interface UpdateProfileRequest {
  fullName: string;
}

/** Shape of `GET /api/auth/session` (our own BFF endpoint, not the gateway). */
export interface SessionResponse {
  user: UserDto | null;
  /** true when the gateway could not be reached at all (screen shows a hint, not an error). */
  offline?: boolean;
}

/**
 * Shape of `POST /api/auth/login` (our own BFF endpoint).
 * Note what is **missing**: the access token. The BFF keeps it inside the httpOnly
 * cookie and only ever hands the browser the safe user projection.
 */
export interface LoginSessionResponse {
  user: UserDto;
}

/** Shape of `POST /api/auth/register` (our own BFF endpoint). */
export interface RegisterResponse {
  user: UserDto;
  /** true when the BFF managed to log the new account in straight away. */
  sessionEstablished: boolean;
}

/* -------------------------------------------------------------------------- */
/* Catalog                                                                    */
/* -------------------------------------------------------------------------- */

export interface CategoryDto {
  id: number | string;
  slug: string;
  name: string;
}

export interface ProductSummary {
  id: number | string;
  sku: string;
  slug: string;
  name: string;
  /** Integer minor units, e.g. 14900000 === Rp 149.000,00 */
  priceCents: number;
  currency: string;
  categoryName: string;
  categorySlug: string;
  imageUrl: string | null;
  inStock: boolean;
}

export interface ProductDetail extends ProductSummary {
  description: string | null;
  stockQuantity: number;
  createdAt: string;
}

/** Query string accepted by `GET /api/v1/products`. */
export interface ProductQuery {
  page?: number;
  size?: number;
  q?: string;
  category?: string;
  sort?: string;
}

/* -------------------------------------------------------------------------- */
/* Cart                                                                       */
/* -------------------------------------------------------------------------- */

export interface CartItemDto {
  productId: number | string;
  sku: string;
  slug: string;
  name: string;
  imageUrl: string | null;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
}

export interface CartDto {
  userId: number | string;
  items: CartItemDto[];
  subtotalCents: number;
  itemCount: number;
  currency: string;
  updatedAt: string;
}

export interface AddCartItemRequest {
  productId: number | string;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

/* -------------------------------------------------------------------------- */
/* Orders                                                                     */
/* -------------------------------------------------------------------------- */

export const ORDER_STATUSES = [
  "PENDING_PAYMENT",
  "PAID",
  "PAYMENT_FAILED",
  "CANCELLED",
  "FULFILLED",
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

export interface OrderItemDto {
  productId: number | string;
  sku: string;
  name: string;
  imageUrl: string | null;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
}

export interface ShippingAddressDto {
  recipientName: string;
  phone: string;
  line1: string;
  line2?: string | null;
  city: string;
  postalCode: string;
  country: string;
}

export interface OrderSummary {
  orderNumber: string;
  status: OrderStatus;
  totalCents: number;
  currency: string;
  itemCount: number;
  createdAt: string;
}

export interface OrderDetail extends OrderSummary {
  items: OrderItemDto[];
  shippingAddress: ShippingAddressDto;
  notes?: string | null;
  updatedAt: string;
}

export interface CreateOrderRequest {
  shippingAddress: ShippingAddressDto;
  notes?: string;
}

/* -------------------------------------------------------------------------- */
/* Payments & notifications                                                   */
/* -------------------------------------------------------------------------- */

export const PAYMENT_STATUSES = [
  "PENDING",
  "SUCCEEDED",
  "FAILED",
  "REFUNDED",
  "CANCELLED",
] as const;

export type PaymentStatus = (typeof PAYMENT_STATUSES)[number] | string;

export interface PaymentDto {
  paymentId: string;
  orderNumber: string;
  status: PaymentStatus;
  method: string;
  amountCents: number;
  currency: string;
  providerRef: string | null;
  createdAt: string;
}

export interface NotificationDto {
  id: number | string;
  type: string;
  subject: string;
  body: string;
  orderNumber: string | null;
  read: boolean;
  createdAt: string;
}

/* -------------------------------------------------------------------------- */
/* Helpers shared by Server Components                                        */
/* -------------------------------------------------------------------------- */

/** Next.js hands `searchParams` to a page as a plain record (values may repeat). */
export type SearchParams = Record<string, string | string[] | undefined>;

/** Reads the first value of a possibly-repeated query param, trimmed. */
export function firstParam(value: string | string[] | undefined): string {
  if (Array.isArray(value)) return (value[0] ?? "").trim();
  return (value ?? "").trim();
}

/**
 * Wraps a payload together with a flag telling the UI whether the gateway answered.
 * `demo: true` means "we could not reach the backend and rendered placeholder data".
 */
export interface DataResult<T> {
  data: T;
  demo: boolean;
}
