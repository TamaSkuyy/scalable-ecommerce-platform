package com.shopflow.product.service;

import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.product.api.dto.CategoryResponse;
import com.shopflow.product.config.CacheNames;
import com.shopflow.product.domain.Category;
import com.shopflow.product.domain.CategoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(cacheNames = CacheNames.CATEGORIES)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public Category requireBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.by("Kategori", "slug", slug));
    }
}
