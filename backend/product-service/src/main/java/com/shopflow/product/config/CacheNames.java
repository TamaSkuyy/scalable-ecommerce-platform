package com.shopflow.product.config;

/** Nama-nama cache di satu tempat supaya tidak ada typo antara {@code @Cacheable} dan {@code @CacheEvict}. */
public final class CacheNames {

    public static final String PRODUCT_DETAIL = "product-detail";
    public static final String CATEGORIES = "categories";

    private CacheNames() {
    }
}
