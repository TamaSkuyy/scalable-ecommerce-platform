package com.shopflow.notification.api.dto;

import com.shopflow.notification.domain.Notification;
import com.shopflow.notification.domain.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String subject,
        String body,
        String orderNumber,
        String channel,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getSubject(),
                notification.getBody(),
                notification.getOrderNumber(),
                notification.getChannel(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
