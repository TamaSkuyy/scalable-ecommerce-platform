import type { NextConfig } from "next";

/**
 * next.config.ts
 *
 * `output: "standalone"` is what makes the Dockerfile small: Next.js traces the exact files
 * the server needs and emits a self-contained `.next/standalone` folder (plus a `server.js`
 * entrypoint) that runs without a `node_modules` install and without the Next CLI.
 *
 * `images.unoptimized: true` is a deliberate trade-off for this learning project:
 *  - product images come from arbitrary, per-environment hosts,
 *  - the demo data uses local SVG placeholders,
 *  - it removes any dependency on the `sharp` binary (which cannot be built in some sandboxes
 *    and would need an extra install step in the image).
 * In a production storefront you would instead list your CDN in `images.remotePatterns` and
 * let Next.js optimize and serve modern formats.
 */
const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,

  images: {
    unoptimized: true,
    remotePatterns: [
      // Allow any http(s) host so a product `imageUrl` coming from the gateway always renders.
      { protocol: "https", hostname: "**" },
      { protocol: "http", hostname: "**" },
    ],
  },

  // Nicer server logs while learning: print the full URL of every cached fetch.
  logging: {
    fetches: {
      fullUrl: true,
    },
  },
};

export default nextConfig;
