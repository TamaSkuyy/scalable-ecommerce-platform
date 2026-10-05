/**
 * demo-data.ts — THE ONE AND ONLY fallback module.
 *
 * ── WHY THIS EXISTS ───────────────────────────────────────────────────────────
 * The API gateway is a separate service (Spring Boot + Docker Compose) and may simply not
 * be running while you are working on the frontend. A storefront that shows a stack trace
 * in that situation teaches nothing and demos badly.
 *
 * So every *public catalog* read goes through `src/lib/catalog.ts`, which catches transport
 * failures and falls back to the static data in this file. The UI then renders a friendly
 * "Backend belum berjalan" callout plus a handful of placeholder products, and both
 * `pnpm build` and `pnpm preview` always look right.
 *
 * ── RULES OF THE ROAD ─────────────────────────────────────────────────────────
 *  - This fallback is for READ paths of the **public catalog** (plus a couple of empty-ish
 *    states). It is NEVER used for writes: adding to a cart must fail loudly if the gateway
 *    is down, otherwise the shopper would believe an order was placed when it was not.
 *  - All demo identifiers, prices and categories are shaped exactly like the real DTOs, so
 *    switching between demo and live data is invisible to the components.
 *  - Images are local SVGs under `/public/images`, so nothing here needs the network.
 */

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
} from "./types";
import { DEFAULT_CURRENCY } from "./format";

/** Copy shown to the shopper whenever the gateway could not be reached. */
export const BACKEND_OFFLINE_TITLE = "Backend belum berjalan";

export const BACKEND_OFFLINE_NOTICE =
  "Kami tidak dapat menghubungi API gateway, jadi halaman ini menampilkan data contoh. " +
  "Jalankan backend (docker compose up) lalu muat ulang halaman untuk melihat data asli.";

/** Handy marker for demo records so the UI can badge them. */
export const DEMO_BADGE_LABEL = "Data contoh";

/* -------------------------------------------------------------------------- */
/* Categories                                                                 */
/* -------------------------------------------------------------------------- */

export const DEMO_CATEGORIES: CategoryDto[] = [
  { id: 1, slug: "fashion", name: "Fashion" },
  { id: 2, slug: "elektronik", name: "Elektronik" },
  { id: 3, slug: "rumah-tangga", name: "Rumah Tangga" },
  { id: 4, slug: "olahraga", name: "Olahraga" },
  { id: 5, slug: "kecantikan", name: "Kecantikan" },
];

/* -------------------------------------------------------------------------- */
/* Products                                                                   */
/* -------------------------------------------------------------------------- */

interface DemoProductSeed {
  id: number;
  sku: string;
  slug: string;
  name: string;
  priceCents: number;
  categorySlug: string;
  stockQuantity: number;
  description: string;
}

const DEMO_SEEDS: DemoProductSeed[] = [
  {
    id: 1,
    sku: "SF-FSH-001",
    slug: "kaos-katun-premium",
    name: "Kaos Katun Premium",
    priceCents: 14_900_000,
    categorySlug: "fashion",
    stockQuantity: 42,
    description:
      "Kaos katun combed 30s dengan potongan regular fit. Jahitan rapi, tidak mudah melar, dan nyaman dipakai harian.",
  },
  {
    id: 2,
    sku: "SF-FSH-002",
    slug: "kemeja-flanel-linen",
    name: "Kemeja Flanel Linen",
    priceCents: 27_500_000,
    categorySlug: "fashion",
    stockQuantity: 18,
    description:
      "Kemeja lengan panjang berbahan linen campuran yang adem. Cocok untuk acara semi-formal maupun santai.",
  },
  {
    id: 3,
    sku: "SF-FSH-003",
    slug: "tote-bag-kanvas",
    name: "Tote Bag Kanvas",
    priceCents: 9_900_000,
    categorySlug: "fashion",
    stockQuantity: 0,
    description:
      "Tas kanvas tebal 12 oz dengan tali jahit ganda. Muat laptop 14 inci dan tetap ringan dibawa seharian.",
  },
  {
    id: 4,
    sku: "SF-ELK-001",
    slug: "headphone-nirkabel-pro",
    name: "Headphone Nirkabel Pro",
    priceCents: 189_000_000,
    categorySlug: "elektronik",
    stockQuantity: 7,
    description:
      "Headphone over-ear dengan active noise cancelling dan daya tahan hingga 40 jam. Dilengkapi mode transparansi.",
  },
  {
    id: 5,
    sku: "SF-ELK-002",
    slug: "smartwatch-sport-s2",
    name: "Smartwatch Sport S2",
    priceCents: 125_000_000,
    categorySlug: "elektronik",
    stockQuantity: 23,
    description:
      "Jam pintar dengan pemantau detak jantung, SpO2, dan 100+ mode olahraga. Tahan air hingga 5 ATM.",
  },
  {
    id: 6,
    sku: "SF-ELK-003",
    slug: "speaker-bluetooth-mini",
    name: "Speaker Bluetooth Mini",
    priceCents: 45_000_000,
    categorySlug: "elektronik",
    stockQuantity: 35,
    description:
      "Speaker portabel 10 W dengan bass pasif ganda. Baterai 12 jam dan sertifikasi tahan cipratan air IPX5.",
  },
  {
    id: 7,
    sku: "SF-RMH-001",
    slug: "lampu-meja-minimalis",
    name: "Lampu Meja Minimalis",
    priceCents: 32_000_000,
    categorySlug: "rumah-tangga",
    stockQuantity: 12,
    description:
      "Lampu meja dengan tiga tingkat suhu warna dan penyesuaian kecerahan. Desain ramping untuk meja kerja.",
  },
  {
    id: 8,
    sku: "SF-RMH-002",
    slug: "set-piring-keramik-4pcs",
    name: "Set Piring Keramik (4 pcs)",
    priceCents: 21_500_000,
    categorySlug: "rumah-tangga",
    stockQuantity: 26,
    description:
      "Empat piring makan keramik diameter 26 cm, aman untuk microwave dan dishwasher. Finishing glossy.",
  },
  {
    id: 9,
    sku: "SF-OLH-001",
    slug: "matras-yoga-anti-slip",
    name: "Matras Yoga Anti-Slip",
    priceCents: 24_000_000,
    categorySlug: "olahraga",
    stockQuantity: 31,
    description:
      "Matras TPE setebal 6 mm dengan permukaan anti-slip dua sisi. Ringan, mudah digulung, dan bebas bau karet.",
  },
  {
    id: 10,
    sku: "SF-OLH-002",
    slug: "botol-minum-termal-750ml",
    name: "Botol Minum Termal 750 ml",
    priceCents: 17_500_000,
    categorySlug: "olahraga",
    stockQuantity: 54,
    description:
      "Botol stainless steel 316 berinsulasi ganda. Menjaga suhu dingin 24 jam atau panas 12 jam.",
  },
  {
    id: 11,
    sku: "SF-KCN-001",
    slug: "serum-vitamin-c-20ml",
    name: "Serum Vitamin C 20 ml",
    priceCents: 13_500_000,
    categorySlug: "kecantikan",
    stockQuantity: 48,
    description:
      "Serum 10% vitamin C dengan ferulic acid untuk mencerahkan dan menyamarkan noda hitam. Tekstur ringan.",
  },
  {
    id: 12,
    sku: "SF-KCN-002",
    slug: "sunscreen-spf-50",
    name: "Sunscreen SPF 50 PA++++",
    priceCents: 11_500_000,
    categorySlug: "kecantikan",
    stockQuantity: 60,
    description:
      "Tabir surya ringan tanpa white cast, SPF 50 PA++++. Tidak lengket dan cocok untuk kulit berminyak.",
  },
];

/** ISO timestamps are fixed so server-rendered and client-rendered markup always match. */
function demoCreatedAt(index: number): string {
  const day = String(28 - index).padStart(2, "0");
  return `2025-03-${day}T08:00:00Z`;
}

function categoryNameFor(slug: string): string {
  return DEMO_CATEGORIES.find((category) => category.slug === slug)?.name ?? "Lainnya";
}

/** The static catalog, shaped exactly like `GET /api/v1/products`. */
export const DEMO_PRODUCTS: ProductDetail[] = DEMO_SEEDS.map((seed, index) => ({
  id: seed.id,
  sku: seed.sku,
  slug: seed.slug,
  name: seed.name,
  priceCents: seed.priceCents,
  currency: DEFAULT_CURRENCY,
  categoryName: categoryNameFor(seed.categorySlug),
  categorySlug: seed.categorySlug,
  imageUrl: `/images/placeholder-${(index % 6) + 1}.svg`,
  inStock: seed.stockQuantity > 0,
  description: seed.description,
  stockQuantity: seed.stockQuantity,
  createdAt: demoCreatedAt(index),
}));

/** Product summaries (everything except the detail-only fields). */
export const DEMO_PRODUCT_SUMMARIES: ProductSummary[] = DEMO_PRODUCTS.map(toSummary);

/** Explicit mapper (rather than a rest-destructure) to keep the DTO projection auditable. */
function toSummary(product: ProductDetail): ProductSummary {
  return {
    id: product.id,
    sku: product.sku,
    slug: product.slug,
    name: product.name,
    priceCents: product.priceCents,
    currency: product.currency,
    categoryName: product.categoryName,
    categorySlug: product.categorySlug,
    imageUrl: product.imageUrl,
    inStock: product.inStock,
  };
}

/* -------------------------------------------------------------------------- */
/* Catalog fallbacks                                                          */
/* -------------------------------------------------------------------------- */

export const DEMO_SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Terbaru" },
  { value: "price,asc", label: "Harga terendah" },
  { value: "price,desc", label: "Harga tertinggi" },
  { value: "name,asc", label: "Nama A–Z" },
] as const;

export const DEFAULT_SORT = DEMO_SORT_OPTIONS[0].value;

/**
 * Applies the same filtering/sorting/paging the gateway would, so the demo experience
 * exercises the very same UI (search box, category filter, pagination).
 */
export function demoProductPage(query: ProductQuery = {}): PageResponse<ProductSummary> {
  const page = Math.max(0, query.page ?? 0);
  const size = Math.max(1, query.size ?? 12);
  const needle = (query.q ?? "").trim().toLowerCase();
  const category = (query.category ?? "").trim();

  let rows = DEMO_PRODUCT_SUMMARIES.slice();

  if (needle) {
    rows = rows.filter(
      (product) =>
        product.name.toLowerCase().includes(needle) ||
        product.sku.toLowerCase().includes(needle) ||
        product.categoryName.toLowerCase().includes(needle),
    );
  }
  if (category) {
    rows = rows.filter((product) => product.categorySlug === category);
  }

  rows = sortDemoProducts(rows, query.sort ?? DEFAULT_SORT);

  const totalElements = rows.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const safePage = Math.min(page, totalPages - 1);
  const start = safePage * size;
  const content = rows.slice(start, start + size);

  return {
    content,
    page: safePage,
    size,
    totalElements,
    totalPages,
    first: safePage === 0,
    last: safePage >= totalPages - 1,
  };
}

/**
 * Because `ProductSummary` does not carry `createdAt`, the demo sorter looks the timestamp
 * up by slug. Keeping this honest (instead of comparing array positions) means the demo
 * ordering matches what the gateway would return for `sort=createdAt,desc`.
 */
const CREATED_AT_MS_BY_SLUG = new Map(DEMO_PRODUCTS.map((product) => [product.slug, Date.parse(product.createdAt)]));

function sortDemoProducts(rows: ProductSummary[], sort: string): ProductSummary[] {
  const [field, direction = "asc"] = sort.split(",");
  const factor = direction === "desc" ? -1 : 1;
  return rows.sort((a, b) => {
    switch (field) {
      case "price":
        return (a.priceCents - b.priceCents) * factor;
      case "name":
        return a.name.localeCompare(b.name, "id-ID") * factor;
      case "createdAt":
      default:
        return ((CREATED_AT_MS_BY_SLUG.get(a.slug) ?? 0) - (CREATED_AT_MS_BY_SLUG.get(b.slug) ?? 0)) * factor;
    }
  });
}

/** Returns the demo product matching a slug, or `null` (mirrors a 404 from the gateway). */
export function demoProductDetail(slug: string): ProductDetail | null {
  return DEMO_PRODUCTS.find((product) => product.slug === slug) ?? null;
}

/** A few "featured" products for the landing page. */
export function demoFeaturedProducts(limit = 8): ProductSummary[] {
  return DEMO_PRODUCT_SUMMARIES.slice(0, limit);
}

/* -------------------------------------------------------------------------- */
/* Order / account fallbacks (read-only, so the preview never looks broken)    */
/* -------------------------------------------------------------------------- */

export const DEMO_ORDERS: OrderDetail[] = [
  {
    orderNumber: "SF-2025-0001",
    status: "PENDING_PAYMENT",
    totalCents: 189_000_000 + 45_000_000,
    currency: DEFAULT_CURRENCY,
    itemCount: 2,
    createdAt: "2025-03-20T09:15:00Z",
    updatedAt: "2025-03-20T09:15:00Z",
    notes: "Tolong dibungkus rapi.",
    shippingAddress: {
      recipientName: "Budi Santoso",
      phone: "081234567890",
      line1: "Jl. Merdeka No. 12, RT 02 / RW 05",
      line2: "Kelurahan Menteng",
      city: "Jakarta Pusat",
      postalCode: "10310",
      country: "Indonesia",
    },
    items: [
      {
        productId: 4,
        sku: "SF-ELK-001",
        name: "Headphone Nirkabel Pro",
        imageUrl: "/images/placeholder-4.svg",
        unitPriceCents: 189_000_000,
        quantity: 1,
        lineTotalCents: 189_000_000,
      },
      {
        productId: 6,
        sku: "SF-ELK-003",
        name: "Speaker Bluetooth Mini",
        imageUrl: "/images/placeholder-6.svg",
        unitPriceCents: 45_000_000,
        quantity: 1,
        lineTotalCents: 45_000_000,
      },
    ],
  },
  {
    orderNumber: "SF-2025-0002",
    status: "PAID",
    totalCents: 24_000_000 * 2,
    currency: DEFAULT_CURRENCY,
    itemCount: 2,
    createdAt: "2025-03-12T03:40:00Z",
    updatedAt: "2025-03-12T04:02:00Z",
    notes: null,
    shippingAddress: {
      recipientName: "Budi Santoso",
      phone: "081234567890",
      line1: "Jl. Merdeka No. 12, RT 02 / RW 05",
      line2: null,
      city: "Jakarta Pusat",
      postalCode: "10310",
      country: "Indonesia",
    },
    items: [
      {
        productId: 9,
        sku: "SF-OLH-001",
        name: "Matras Yoga Anti-Slip",
        imageUrl: "/images/placeholder-3.svg",
        unitPriceCents: 24_000_000,
        quantity: 2,
        lineTotalCents: 48_000_000,
      },
    ],
  },
];

export const DEMO_ORDER_SUMMARIES: OrderSummary[] = DEMO_ORDERS.map(toOrderSummary);

function toOrderSummary(order: OrderDetail): OrderSummary {
  return {
    orderNumber: order.orderNumber,
    status: order.status,
    totalCents: order.totalCents,
    currency: order.currency,
    itemCount: order.itemCount,
    createdAt: order.createdAt,
  };
}

export function demoOrderPage(page = 0, size = 10): PageResponse<OrderSummary> {
  const totalElements = DEMO_ORDER_SUMMARIES.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const safePage = Math.min(Math.max(0, page), totalPages - 1);
  const start = safePage * size;
  return {
    content: DEMO_ORDER_SUMMARIES.slice(start, start + size),
    page: safePage,
    size,
    totalElements,
    totalPages,
    first: safePage === 0,
    last: safePage >= totalPages - 1,
  };
}

export function demoOrderDetail(orderNumber: string): OrderDetail | null {
  return DEMO_ORDERS.find((order) => order.orderNumber === orderNumber) ?? null;
}

export function demoPayment(orderNumber: string): PaymentDto | null {
  const order = demoOrderDetail(orderNumber);
  if (!order) return null;
  return {
    paymentId: `PAY-${orderNumber}`,
    orderNumber,
    status: order.status === "PAID" ? "SUCCEEDED" : order.status === "PENDING_PAYMENT" ? "PENDING" : "FAILED",
    method: "BANK_TRANSFER",
    amountCents: order.totalCents,
    currency: order.currency,
    providerRef: `DEMO-${orderNumber}`,
    createdAt: order.createdAt,
  };
}

export const DEMO_NOTIFICATIONS: NotificationDto[] = [
  {
    id: 1,
    type: "ORDER_CREATED",
    subject: "Pesanan SF-2025-0001 berhasil dibuat",
    body: "Terima kasih! Selesaikan pembayaran dalam 24 jam agar pesanan tidak otomatis dibatalkan.",
    orderNumber: "SF-2025-0001",
    read: false,
    createdAt: "2025-03-20T09:15:05Z",
  },
  {
    id: 2,
    type: "PAYMENT_SUCCEEDED",
    subject: "Pembayaran SF-2025-0002 diterima",
    body: "Pembayaran sebesar Rp 480.000 telah kami terima. Pesanan sedang disiapkan.",
    orderNumber: "SF-2025-0002",
    read: true,
    createdAt: "2025-03-12T04:02:11Z",
  },
  {
    id: 3,
    type: "WELCOME",
    subject: "Selamat datang di ShopFlow",
    body: "Lengkapi profil Anda dan mulai jelajahi katalog produk pilihan kami.",
    orderNumber: null,
    read: true,
    createdAt: "2025-03-01T01:00:00Z",
  },
];

export const DEMO_USER = {
  id: 1,
  email: "demo@shopflow.id",
  fullName: "Budi Santoso",
  roles: ["CUSTOMER"],
  createdAt: "2025-03-01T01:00:00Z",
};

/** An empty, well-formed cart used when the gateway is down on `/cart`. */
export function demoCart(): CartDto {
  return {
    userId: DEMO_USER.id,
    items: [],
    subtotalCents: 0,
    itemCount: 0,
    currency: DEFAULT_CURRENCY,
    updatedAt: new Date(0).toISOString(),
  };
}
