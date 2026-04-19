package com.example.sams.audit.projection;

import java.time.Instant;

public interface AuditLogView {

    Long getId();

    String getActionType();

    Long getActorUserId();

    String getActorUsername();

    String getActorRole();

    String getEntityType();

    String getEntityId();

    String getSummary();

    Instant getCreatedAt();
}
