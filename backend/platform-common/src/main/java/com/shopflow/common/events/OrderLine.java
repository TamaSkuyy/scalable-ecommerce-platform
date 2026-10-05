package com.shopflow.common.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Satu baris pesanan yang "dibekukan" (snapshot) pada saat checkout.
 *
 * <p>Kenapa snapshot, bukan hanya productId? Karena harga & nama produk bisa berubah.
 * Pesanan lama harus tetap menampilkan harga saat dibeli — ini prinsip penting di
 * sistem e-commerce.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderLine(
        Long productId,
        String sku,
        String name,
        String imageUrl,
        long unitPriceCents,
        int quantity) implements Serializable {

    @JsonIgnore
    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }
}
