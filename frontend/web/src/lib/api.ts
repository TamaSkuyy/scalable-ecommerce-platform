/**
 * api.ts — Typed client-side API surface used by Client Components.
 *
 * Every function here goes through `src/middleware.ts`, which proxies `/api/v1/*` to the
 * gateway and injects the Bearer token from the httpOnly cookie. Client code therefore
 * contains **zero** auth logic.
 *
 * The `/api/auth/*` calls are our own BFF route handlers (also in this Next.js app) — they
 * are the only place where a JWT is ever written to a cookie.
 */

import { apiFetch } from "./http";
import type {
  AddCartItemRequest,
  CartDto,
  CreateOrderRequest,
  LoginRequest,
  LoginSessionResponse,
  NotificationDto,
  OrderDetail,
  OrderSummary,
  PageResponse,
  PaymentDto,
  RegisterRequest,
  RegisterResponse,
  SessionResponse,
  UpdateCartItemRequest,
  UpdateProfileRequest,
  UserDto,
} from "./types";

/* -------------------------------------------------------------------------- */
/* BFF auth endpoints (same origin)                                           */
/* -------------------------------------------------------------------------- */

export const authApi = {
  /** Sets the `shopflow_token` httpOnly cookie. The response deliberately omits the JWT. */
  login: (payload: LoginRequest) =>
    apiFetch<LoginSessionResponse>("/api/auth/login", { method: "POST", body: payload }),

  /** Registers the account; the BFF then tries to establish a session in one round trip. */
  register: (payload: RegisterRequest) =>
    apiFetch<RegisterResponse>("/api/auth/register", { method: "POST", body: payload }),

  /** Clears the cookie server-side. */
  logout: () => apiFetch<void>("/api/auth/logout", { method: "POST" }),

  /** `{ user }` or `{ user: null }` — never throws for an anonymous visitor. */
  session: () => apiFetch<SessionResponse>("/api/auth/session"),
};

/* -------------------------------------------------------------------------- */
/* Gateway endpoints (proxied by the middleware)                              */
/* -------------------------------------------------------------------------- */

export const cartApi = {
  get: () => apiFetch<CartDto>(`/api/v1/cart`),
  addItem: (payload: AddCartItemRequest) =>
    apiFetch<CartDto>("/api/v1/cart/items", { method: "POST", body: payload }),
  updateItem: (productId: number | string, payload: UpdateCartItemRequest) =>
    apiFetch<CartDto>(`/api/v1/cart/items/${encodeURIComponent(String(productId))}`, {
      method: "PATCH",
      body: payload,
    }),
  removeItem: (productId: number | string) =>
    apiFetch<void>(`/api/v1/cart/items/${encodeURIComponent(String(productId))}`, { method: "DELETE" }),
  clear: () => apiFetch<void>("/api/v1/cart", { method: "DELETE" }),
};

export const ordersApi = {
  create: (payload: CreateOrderRequest) =>
    apiFetch<OrderDetail>("/api/v1/orders", { method: "POST", body: payload }),
  list: (page = 0, size = 10) =>
    apiFetch<PageResponse<OrderSummary>>(`/api/v1/orders?page=${page}&size=${size}`),
  get: (orderNumber: string) =>
    apiFetch<OrderDetail>(`/api/v1/orders/${encodeURIComponent(orderNumber)}`),
  cancel: (orderNumber: string) =>
    apiFetch<OrderDetail>(`/api/v1/orders/${encodeURIComponent(orderNumber)}/cancel`, {
      method: "POST",
    }),
};

export const paymentsApi = {
  byOrder: (orderNumber: string) =>
    apiFetch<PaymentDto>(`/api/v1/payments/${encodeURIComponent(orderNumber)}`),
};

export const notificationsApi = {
  list: () => apiFetch<NotificationDto[]>("/api/v1/notifications/me"),
};

export const userApi = {
  me: () => apiFetch<UserDto>("/api/v1/users/me"),
  updateProfile: (payload: UpdateProfileRequest) =>
    apiFetch<UserDto>("/api/v1/users/me", { method: "PATCH", body: payload }),
};
