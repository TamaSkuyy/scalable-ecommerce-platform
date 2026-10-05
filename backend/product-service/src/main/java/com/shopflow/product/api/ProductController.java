package com.shopflow.product.api;

import com.shopflow.common.api.ForbiddenException;
import com.shopflow.common.api.PageResponse;
import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.product.api.dto.AdjustStockRequest;
import com.shopflow.product.api.dto.CreateProductRequest;
import com.shopflow.product.api.dto.ProductDetailResponse;
import com.shopflow.product.api.dto.ProductSummaryResponse;
import com.shopflow.product.api.dto.StockResponse;
import com.shopflow.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Katalog produk")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 60;

    /**
     * Daftar field yang boleh dipakai untuk sorting.
     *
     * <p>Whitelist, bukan meneruskan input user langsung ke {@code Sort.by()}. Kalau tidak,
     * klien bisa menyortir berdasarkan kolom internal (atau memicu error 500), dan itu
     * sekaligus membocorkan struktur database.
     */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "createdAt", "createdAt",
            "name", "name",
            "price", "priceCents",
            "priceCents", "priceCents");

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Cari & filter produk (publik)")
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        return productService.search(q, category, toPageable(page, size, sort));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detail produk berdasarkan slug (publik, di-cache)")
    public ProductDetailResponse detail(@PathVariable String slug) {
        return productService.findBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tambah produk (butuh role ADMIN)")
    public ProductDetailResponse create(CurrentUser currentUser, @Valid @RequestBody CreateProductRequest request) {
        requireAdmin(currentUser);
        return productService.create(request);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Koreksi stok manual (butuh role ADMIN)")
    public StockResponse adjustStock(CurrentUser currentUser,
                                     @PathVariable Long id,
                                     @Valid @RequestBody AdjustStockRequest request) {
        requireAdmin(currentUser);
        return productService.adjustStock(id, request.delta(), "admin: " + request.reason());
    }

    private void requireAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new UnauthorizedException("Kamu harus login untuk melakukan aksi ini");
        }
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Aksi ini hanya untuk admin");
        }
    }

    private Pageable toPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);

        String[] parts = (sort == null ? "createdAt,desc" : sort).split(",");
        String property = SORTABLE_FIELDS.getOrDefault(parts[0].trim(), "createdAt");
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
