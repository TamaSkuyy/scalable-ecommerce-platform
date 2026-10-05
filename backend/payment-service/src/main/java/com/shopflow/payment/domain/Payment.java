package com.shopflow.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Catatan transaksi pembayaran.
 *
 * <p>Constraint {@code uk_payments_order_number} bukan hiasan: ia adalah
 * <b>jaminan idempotensi</b>. Kalau event {@code order.created} terkirim dua kali
 * (hal yang normal di RabbitMQ - at-least-once delivery), percobaan insert kedua akan
 * ditolak database. Jadi "charge dua kali" tidak mungkin terjadi walau kode kita lalai.
 */
@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_order_number", columnNames = "order_number"),
        @UniqueConstraint(name = "uk_payments_payment_id", columnNames = "payment_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID yang kita tampilkan ke klien, mis. {@code PAY-7F3A91C2}. */
    @Column(name = "payment_id", nullable = false, length = 40)
    private String paymentId;

    @Column(name = "order_number", nullable = false, length = 32)
    private String orderNumber;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "IDR";

    @Column(name = "method", nullable = false, length = 32)
    private String method = "SIMULATED_CARD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** ID transaksi dari payment gateway (Stripe: {@code pi_...}). */
    @Column(name = "provider_reference", length = 64)
    private String providerReference;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(String paymentId, String orderNumber, Long userId, long amountCents, String currency, String method) {
        this.paymentId = paymentId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.method = method;
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

    public void markSucceeded(String providerReference) {
        this.status = PaymentStatus.SUCCEEDED;
        this.providerReference = providerReference;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
