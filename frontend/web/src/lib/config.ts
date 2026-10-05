/**
 * config.ts — Runtime configuration, read from the environment in one place.
 *
 * TWO DIFFERENT KINDS OF ENV VAR LIVE HERE:
 *
 * 1. `API_BASE_URL` — **server-side only**. It is the address of the API gateway as seen
 *    *from the Next.js server*. Inside Docker Compose that is `http://api-gateway:8080`,
 *    while on a developer laptop it is `http://localhost:8080`. Because it is not prefixed
 *    with `NEXT_PUBLIC_`, Next.js will never inline it into a browser bundle — the browser
 *    has no idea this host exists. That is exactly what we want for the BFF pattern.
 *
 * 2. `NEXT_PUBLIC_SITE_URL` — the public origin of *this* app (`http://localhost:3000`).
 *    Because of the `NEXT_PUBLIC_` prefix it *is* shipped to the browser and is safe to use
 *    for absolute links, `metadataBase`, canonical URLs and Open Graph tags.
 */

/** Address of the API gateway, as reachable from the Next.js server. */
export const API_BASE_URL = (process.env.API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");

/** Public origin of the storefront itself. */
export const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/+$/, "");

/** Every gateway endpoint lives under this prefix. */
export const API_PREFIX = "/api/v1";

/**
 * Name of the httpOnly cookie holding the JWT.
 * Kept in a module (not a literal) so the BFF route handlers, the middleware and the
 * logout flow can never drift apart.
 */
export const TOKEN_COOKIE = "shopflow_token";

/** Catalog data is public; this is the ISR window in seconds. */
export const CATALOG_REVALIDATE_SECONDS = 60;

/** Routes that require a signed-in session. Used by `src/middleware.ts`. */
export const PROTECTED_ROUTES = ["/account", "/cart", "/checkout", "/orders"] as const;

/** Store identity used across metadata and the header/footer. */
export const STORE = {
  name: "ShopFlow",
  tagline: "Belanja cepat, harga jujur.",
  description:
    "ShopFlow adalah storefront contoh yang dibangun dengan Next.js App Router: Server Components, ISR, dan pola Backend-for-Frontend dengan cookie httpOnly.",
  supportEmail: "halo@shopflow.id",
} as const;
