package com.shopflow.product.service;

import com.shopflow.common.api.BadRequestException;
import com.shopflow.common.api.ConflictException;
import com.shopflow.common.api.PageResponse;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.product.api.dto.CreateProductRequest;
import com.shopflow.product.api.dto.ProductDetailResponse;
import com.shopflow.product.api.dto.ProductSnapshotResponse;
import com.shopflow.product.api.dto.ProductSummaryResponse;
import com.shopflow.product.api.dto.StockResponse;
import com.shopflow.product.config.CacheNames;
import com.shopflow.product.domain.Category;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> search(String q, String categorySlug, Pageable pageable) {
        // Normalisasi: string kosong = "tanpa filter". Lihat ProductRepository.search.
        String query = q == null ? "" : q.trim();
        String category = categorySlug == null ? "" : categorySlug.trim();

        Page<Product> page = productRepository.search(query, category, pageable);
        return PageResponse.from(page, ProductSummaryResponse::from);
    }

    /**
     * Detail produk, di-cache berdasarkan slug.
     *
     * <p>Halaman detail produk adalah trafik terbesar di e-commerce dan datanya hampir
     * tidak pernah berubah. Cache 2 menit sudah memotong hampir semua query ke database.
     */
    @Cacheable(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#slug")
    @Transactional(readOnly = true)
    public ProductDetailResponse findBySlug(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> ResourceNotFoundException.by("Produk", "slug", slug));
        return ProductDetailResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductSnapshotResponse findSnapshot(Long productId) {
        Product product = productRepository.findDetailById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Produk", productId));
        return ProductSnapshotResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PRODUCT_DETAIL, CacheNames.CATEGORIES}, allEntries = true)
    public ProductDetailResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySku(sku)) {
            throw new ConflictException("SKU " + sku + " sudah dipakai produk lain");
        }

        Category category = categoryService.requireBySlug(request.categorySlug().trim());

        Product product = new Product(
                sku,
                uniqueSlug(request.name()),
                request.name().trim(),
                category,
                request.priceCents(),
                request.stockQuantity());
        product.setDescription(request.description());
        product.setImageUrl(request.imageUrl());

        Product saved = productRepository.save(product);
        log.info("Produk baru dibuat: sku={} slug={}", saved.getSku(), saved.getSlug());
        return ProductDetailResponse.from(saved);
    }

    /**
     * Mengubah stok secara atomik.
     *
     * <p>Ini inti pencegahan <b>oversell</b>. Urutannya:
     * <ol>
     *   <li>Ambil baris dengan {@code SELECT ... FOR UPDATE} (kunci tulis tingkat baris)</li>
     *   <li>Hitung stok baru di memori</li>
     *   <li>Tolak kalau hasilnya negatif</li>
     *   <li>Commit -> update + bump {@code @Version}</li>
     * </ol>
     * Dua checkout bersamaan untuk produk yang sama akan berbaris, bukan saling menimpa.
     *
     * @param delta negatif untuk mengurangi (reserve), positif untuk menambah (release)
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, allEntries = true)
    public StockResponse adjustStock(Long productId, int delta, String reason) {
        if (delta == 0) {
            throw new BadRequestException("delta stok tidak boleh 0");
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Produk", productId));

        int before = product.getStockQuantity();
        int after = before + delta;
        if (after < 0) {
            throw new ConflictException("Stok " + product.getSku() + " tidak cukup. Tersedia "
                    + before + ", diminta " + Math.abs(delta));
        }

        product.setStockQuantity(after);
        Product saved = productRepository.save(product);
        log.info("Stok {}: {} -> {} ({})", saved.getSku(), before, after,
                reason == null ? "tanpa alasan" : reason);
        return StockResponse.from(saved);
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private String uniqueSlug(String name) {
        String base = slugify(name);
        if (base.isBlank()) {
            base = "produk";
        }
        String candidate = base;
        int counter = 2;
        while (productRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    /** "Kopi Gayo Arabica 250g" -> "kopi-gayo-arabica-250g". Penting untuk URL SEO-friendly. */
    static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");                      // buang aksen
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
