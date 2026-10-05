package com.shopflow.product.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slug adalah bagian dari URL publik, jadi harus stabil dan aman.
 * Kalau slug berubah, link yang sudah dibagikan ke user akan mati (404).
 */
class ProductServiceSlugTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "Kopi Gayo Arabica 250g,        kopi-gayo-arabica-250g",
            "TWS ShopFlow Buds Pro,         tws-shopflow-buds-pro",
            "Power Bank 20.000mAh PD 30W,   power-bank-20-000mah-pd-30w",
            "Keyboard Mekanik 65% Hot-Swap, keyboard-mekanik-65-hot-swap",
            "'  Spasi  Berlebih  ',         spasi-berlebih",
            "Kopi---Gayo,                   kopi-gayo",
            "Café Latte,                    cafe-latte"
    })
    @DisplayName("slugify menghasilkan slug yang bersih")
    void slugifyProducesCleanSlugs(String input, String expected) {
        assertThat(ProductService.slugify(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("input yang hanya berisi simbol tidak menghasilkan slug kosong yang aneh")
    void slugifyHandlesSymbolOnlyInput() {
        assertThat(ProductService.slugify("!!!")).isEmpty();
    }

    @Test
    @DisplayName("hasil slugify selalu huruf kecil dan tanpa spasi")
    void slugifyIsUrlSafe() {
        String slug = ProductService.slugify("Roti Bakar Keju Spesial!!!");

        assertThat(slug).isEqualTo(slug.toLowerCase());
        assertThat(slug).doesNotContain(" ");
        assertThat(slug).matches("[a-z0-9-]+");
    }
}
