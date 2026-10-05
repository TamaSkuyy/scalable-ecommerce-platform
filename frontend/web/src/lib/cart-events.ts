/**
 * cart-events.ts — A 6-line pub/sub so the header cart badge stays in sync.
 *
 * The header badge and the cart page live in different React trees, so they cannot share
 * state through props. Instead of pulling in a state library we broadcast a DOM event:
 * the cart mutations emit it, the badge listens for it.
 *
 * This is deliberately the *simplest* thing that works; a real app with more shared state
 * would reach for a store (Zustand, Redux) or React Context.
 */

export const CART_UPDATED_EVENT = "shopflow:cart-updated";

/** Announce that the cart changed so listeners (header badge) can refetch. */
export function emitCartUpdated(): void {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(CART_UPDATED_EVENT));
}

/** Subscribe to cart changes. Returns an unsubscribe function. */
export function onCartUpdated(listener: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener(CART_UPDATED_EVENT, listener);
  return () => window.removeEventListener(CART_UPDATED_EVENT, listener);
}
