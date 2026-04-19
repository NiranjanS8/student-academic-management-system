package com.example.sams.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actionType,
        Long actorUserId,
        String actorUsername,
        String actorRole,
        String entityType,
        String entityId,
        String summary,
        Instant createdAt
) {
}
