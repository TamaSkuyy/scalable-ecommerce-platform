package com.shopflow.cart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Salinan kontrak {@code ProductSnapshotResponse} milik product-service.
 *
 * <p>Kenapa tidak memakai class yang sama dari library bersama? Karena itu akan
 * menciptakan <b>coupling kompilasi</b> antar-service: setiap perubahan di product-service
 * memaksa semua konsumen rebuild. Dengan DTO sendiri, kontrak dijaga lewat JSON + test
 * kontrak, bukan lewat classpath bersama.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} membuat kita tetap kompatibel
 * kalau product-service menambah field baru.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSnapshot(
        Long id,
        String sku,
        String slug,
        String name,
        String imageUrl,
        long priceCents,
        String currency,
        int stockQuantity,
        boolean active) {
}
