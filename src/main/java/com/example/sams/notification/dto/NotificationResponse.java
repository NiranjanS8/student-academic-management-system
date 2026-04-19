package com.example.sams.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
