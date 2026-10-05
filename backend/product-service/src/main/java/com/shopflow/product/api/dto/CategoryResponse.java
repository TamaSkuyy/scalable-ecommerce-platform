package com.shopflow.product.api.dto;

import com.shopflow.product.domain.Category;

public record CategoryResponse(Long id, String slug, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getSlug(), category.getName());
    }
}
