package com.shopflow.cart.domain;

import java.io.Serializable;

/**
 * Satu baris di keranjang.
 *
 * <p>Nama, gambar, dan harga di-<i>snapshot</i> saat item ditambahkan, tapi akan
 * disegarkan setiap kali keranjang diubah. Harga final tetap ditentukan ulang
 * oleh order-service saat checkout - keranjang TIDAK boleh jadi sumber kebenaran harga.
 */
public record CartItem(
        Long productId,
        String sku,
        String slug,
        String name,
        String imageUrl,
        long unitPriceCents,
        int quantity) implements Serializable {

    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, sku, slug, name, imageUrl, unitPriceCents, newQuantity);
    }
}
