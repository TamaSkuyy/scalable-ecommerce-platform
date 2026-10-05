package com.shopflow.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Notifikasi yang sudah "dikirim".
 *
 * <p>{@code eventId} (unik) adalah kunci idempotensi: event yang sama boleh dikirim
 * berkali-kali oleh broker, tapi user hanya menerima SATU email. Ini contoh bagus
 * bahwa idempotensi bisa ditegakkan oleh database, bukan hanya oleh kode.
 */
@Entity
@Table(name = "notifications", uniqueConstraints =
        @UniqueConstraint(name = "uk_notifications_event_id", columnNames = "event_id"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false, length = 2000)
    private String body;

    @Column(name = "order_number", length = 32)
    private String orderNumber;

    @Column(name = "channel", nullable = false, length = 16)
    private String channel = "EMAIL";

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(String eventId, Long userId, NotificationType type,
                        String subject, String body, String orderNumber) {
        this.eventId = eventId;
        this.userId = userId;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.orderNumber = orderNumber;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getChannel() {
        return channel;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
