package com.shopflow.cart.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartStateTest {

    private static CartItem item(long productId, long priceCents, int quantity) {
        return new CartItem(productId, "SKU-" + productId, "slug-" + productId,
                "Produk " + productId, null, priceCents, quantity);
    }

    @Test
    @DisplayName("subtotal dihitung dari harga x jumlah untuk setiap baris")
    void computesSubtotal() {
        CartState cart = CartState.empty(1L);
        cart = new CartState(1L, List.of(item(1, 1_500_000, 2), item(2, 250_000, 4)), cart.updatedAt());

        assertThat(cart.subtotalCents()).isEqualTo(1_500_000L * 2 + 250_000L * 4);
    }

    @Test
    @DisplayName("itemCount menjumlahkan kuantitas, bukan jumlah baris")
    void countsQuantityNotRows() {
        CartState cart = new CartState(1L, List.of(item(1, 100, 2), item(2, 100, 3)), null);

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.itemCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("keranjang kosong menghasilkan 0, bukan error")
    void emptyCartIsZero() {
        CartState cart = CartState.empty(7L);

        assertThat(cart.subtotalCents()).isZero();
        assertThat(cart.itemCount()).isZero();
        assertThat(cart.currency()).isEqualTo("IDR");
        assertThat(cart.userId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("perkalian memakai long sehingga tidak overflow untuk kuantitas besar")
    void multiplicationUsesLong() {
        CartState cart = new CartState(1L, List.of(item(1, 99_000_000L, 20)), null);

        assertThat(cart.subtotalCents()).isEqualTo(1_980_000_000L);
    }

    @Test
    @DisplayName("daftar item tidak bisa diubah dari luar record")
    void itemsAreDefensivelyCopied() {
        var original = new java.util.ArrayList<>(List.of(item(1, 100, 1)));
        CartState cart = new CartState(1L, original, null);

        original.clear();

        assertThat(cart.items()).hasSize(1);
    }
}
