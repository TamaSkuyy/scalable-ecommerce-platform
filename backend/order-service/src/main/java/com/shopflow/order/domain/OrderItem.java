package com.shopflow.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Baris pesanan.
 *
 * <p>Harga &amp; nama disimpan sebagai snapshot. Kalau harga produk berubah besok,
 * pesanan hari ini harus tetap menampilkan angka yang dibayar user. Ini alasan kenapa
 * kita TIDAK melakukan JOIN ke product-service saat menampilkan riwayat pesanan.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_order_items_order"))
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    public OrderItem(Long productId, String sku, String name, String imageUrl, long unitPriceCents, int quantity) {
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.imageUrl = imageUrl;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public Long getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public int getQuantity() {
        return quantity;
    }
}
