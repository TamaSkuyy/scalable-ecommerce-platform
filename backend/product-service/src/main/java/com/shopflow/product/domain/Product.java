package com.shopflow.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Produk.
 *
 * <p>Detail penting:
 * <ul>
 *   <li>{@code priceCents} = harga dalam satuan minor (sen). Integer, bukan double,
 *       supaya tidak ada error pembulatan. 150000 = Rp 1.500,00.</li>
 *   <li>{@code @Version} = <b>optimistic locking</b>. Kalau dua request mengubah stok
 *       yang sama bersamaan, yang kedua gagal dan harus dicoba ulang - ini yang mencegah
 *       "oversell" (stok minus) tanpa harus mengunci baris terus-menerus.</li>
 *   <li>{@code active} = soft delete. Produk yang sudah pernah dipesan tidak boleh
 *       dihapus keras, karena pesanan lama masih merujuk padanya.</li>
 * </ul>
 */
@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
        @UniqueConstraint(name = "uk_products_slug", columnNames = "slug")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "IDR";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_products_category"))
    private Category category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String sku, String slug, String name, Category category, long priceCents, int stockQuantity) {
        this.sku = sku;
        this.slug = slug;
        this.name = name;
        this.category = category;
        this.priceCents = priceCents;
        this.stockQuantity = stockQuantity;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isInStock() {
        return active && stockQuantity > 0;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(long priceCents) {
        this.priceCents = priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public long getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
