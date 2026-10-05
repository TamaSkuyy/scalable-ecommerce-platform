/**
 * middleware.ts — Where the Backend-for-Frontend pattern actually happens.
 *
 * Two responsibilities:
 *
 * 1. **Same-origin API proxy.**
 *    Any request to `/api/v1/:path*` is rewritten to `${API_BASE_URL}/api/v1/:path*` and
 *    gets `Authorization: Bearer <cookie>` injected when the cookie exists.
 *    Result: client components can call `fetch('/api/v1/cart')` — same origin, no CORS,
 *    no token handling, and the JWT never becomes readable by JavaScript.
 *
 *      Browser ──▶ /api/v1/cart ──▶ [middleware: +Authorization] ──▶ api-gateway/api/v1/cart
 *
 * 2. **Route protection.**
 *    `/account`, `/cart`, `/checkout` and `/orders*` redirect to
 *    `/login?redirect=<path>` when there is no session cookie.
 *
 * HOW IS THE JWT READ? With `request.cookies` — the middleware runs on the server, so it
 * can see httpOnly cookies that `document.cookie` could never expose.
 *
 * IMPORTANT: we do NOT cryptographically verify the JWT here. The gateway is the authority;
 * the middleware only checks "is a token present?" to give anonymous visitors a fast
 * redirect. Every API call is still validated by the backend.
 */

import { NextResponse, type NextRequest } from "next/server";

import { API_BASE_URL, API_PREFIX, PROTECTED_ROUTES, TOKEN_COOKIE } from "@/lib/config";

/** True when the path is the API surface we proxy (never `/api/auth/*`, which is ours). */
function isProxiedApiPath(pathname: string): boolean {
  return pathname === API_PREFIX || pathname.startsWith(`${API_PREFIX}/`);
}

/** True when the path needs a signed-in shopper. */
function isProtectedPath(pathname: string): boolean {
  return PROTECTED_ROUTES.some((route) => pathname === route || pathname.startsWith(`${route}/`));
}

export function middleware(request: NextRequest): NextResponse {
  const { pathname, search } = request.nextUrl;
  const token = request.cookies.get(TOKEN_COOKIE)?.value;

  /* ------------------------------------------------------------------ */
  /* 1. Proxy /api/v1/* to the gateway and inject the Bearer token       */
  /* ------------------------------------------------------------------ */
  if (isProxiedApiPath(pathname)) {
    const target = new URL(`${pathname}${search}`, API_BASE_URL);

    // Clone the incoming headers so we can safely add the Authorization header.
    const headers = new Headers(request.headers);
    if (token) {
      headers.set("authorization", `Bearer ${token}`);
    } else {
      // Never forward a stale/empty Authorization header from the browser.
      headers.delete("authorization");
    }
    // Let the gateway know which host the shopper actually asked for.
    headers.set("x-forwarded-host", request.headers.get("host") ?? request.nextUrl.host);
    headers.set("x-forwarded-proto", request.nextUrl.protocol.replace(":", ""));

    return NextResponse.rewrite(target, {
      request: { headers },
    });
  }

  /* ------------------------------------------------------------------ */
  /* 2. Protect the shopper's private pages                              */
  /* ------------------------------------------------------------------ */
  if (isProtectedPath(pathname) && !token) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("redirect", `${pathname}${search}`);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

/**
 * The matcher keeps the middleware off every static asset and off `/api/auth/*`
 * (our own BFF route handlers must never be proxied to the gateway).
 */
export const config = {
  matcher: [
    "/api/v1/:path*",
    "/account",
    "/account/:path*",
    "/cart",
    "/cart/:path*",
    "/checkout",
    "/checkout/:path*",
    "/orders",
    "/orders/:path*",
  ],
};
