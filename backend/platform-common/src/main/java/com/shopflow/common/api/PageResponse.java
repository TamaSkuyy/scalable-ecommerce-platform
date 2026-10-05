package com.shopflow.common.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Bentuk halaman (pagination) yang seragam.
 *
 * <p>Kita sengaja TIDAK mengembalikan {@code Page} milik Spring Data secara langsung
 * ke klien, karena format JSON-nya tidak stabil antar versi dan membocorkan detail
 * internal (mis. {@code pageable}, {@code sort}). Lihat: "API contract first".
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast());
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages, page == 0, page >= totalPages - 1);
    }
}
