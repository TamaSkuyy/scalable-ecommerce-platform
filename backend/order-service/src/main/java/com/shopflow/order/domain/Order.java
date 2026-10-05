package com.shopflow.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", uniqueConstraints =
        @UniqueConstraint(name = "uk_orders_number", columnNames = "order_number"))
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nomor yang ditampilkan ke user, mis. {@code ORD-20261005-7F3A}. */
    @Column(name = "order_number", nullable = false, length = 32)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", length = 180)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "IDR";

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "payment_reference", length = 64)
    private String paymentReference;

    @Column(name = "cancellation_reason", length = 200)
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String orderNumber, Long userId, String userEmail, ShippingAddress shippingAddress, String notes) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.userEmail = userEmail;
        this.shippingAddress = shippingAddress;
        this.notes = notes;
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

    /** Menjaga kedua sisi relasi tetap konsisten (kesalahan klasik JPA). */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
        this.totalCents = this.items.stream().mapToLong(OrderItem::lineTotalCents).sum();
    }

    public int itemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getCurrency() {
        return currency;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public String getNotes() {
        return notes;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
