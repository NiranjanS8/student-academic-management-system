package com.example.sams.audit.service;

import com.example.sams.audit.domain.AuditActionType;
import com.example.sams.audit.domain.AuditLog;
import com.example.sams.audit.dto.AuditLogResponse;
import com.example.sams.audit.repository.AuditLogRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.user.service.AppUserDetails;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final int EXPORT_PAGE_SIZE = 10_000;

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(AuditActionType actionType, String entityType, Object entityId, String summary) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(actionType);
        auditLog.setEntityType(normalizeRequired(entityType, "entityType"));
        auditLog.setEntityId(entityId == null ? null : String.valueOf(entityId));
        auditLog.setSummary(normalizeRequired(summary, "summary"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails principal) {
            auditLog.setActorUserId(principal.getUserId());
            auditLog.setActorUsername(principal.getUsername());
            auditLog.setActorRole(resolveActorRole(principal));
        }

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String actionType,
            Long actorUserId,
            String entityType,
            String entityId,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        AuditActionType parsedActionType = parseActionType(actionType);
        String normalizedEntityType = normalize(entityType);
        String normalizedEntityId = normalize(entityId);
        validateDateRange(createdFrom, createdTo);

        return auditLogRepository.search(
                        parsedActionType,
                        actorUserId,
                        normalizedEntityType,
                        normalizedEntityId,
                        toStartOfDay(createdFrom),
                        toEndOfDay(createdTo),
                        pageable
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(
            String actionType,
            Long actorUserId,
            String entityType,
            String entityId,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        List<AuditLogResponse> rows = search(
                actionType,
                actorUserId,
                entityType,
                entityId,
                createdFrom,
                createdTo,
                PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("createdAt").descending())
        ).getContent();

        StringBuilder builder = new StringBuilder();
        builder.append("id,actionType,actorUserId,actorUsername,actorRole,entityType,entityId,summary,createdAt")
                .append(System.lineSeparator());
        for (AuditLogResponse row : rows) {
            builder.append(csv(row.id())).append(',')
                    .append(csv(row.actionType())).append(',')
                    .append(csv(row.actorUserId())).append(',')
                    .append(csv(row.actorUsername())).append(',')
                    .append(csv(row.actorRole())).append(',')
                    .append(csv(row.entityType())).append(',')
                    .append(csv(row.entityId())).append(',')
                    .append(csv(row.summary())).append(',')
                    .append(csv(row.createdAt()))
                    .append(System.lineSeparator());
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActionType().name(),
                auditLog.getActorUserId(),
                auditLog.getActorUsername(),
                auditLog.getActorRole(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getSummary(),
                auditLog.getCreatedAt()
        );
    }

    private AuditActionType parseActionType(String rawActionType) {
        if (rawActionType == null || rawActionType.isBlank()) {
            return null;
        }
        try {
            return AuditActionType.valueOf(rawActionType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid actionType. Allowed values: USER_CREATED, ENROLLMENT_ENROLLED, ENROLLMENT_DROPPED, EXAM_PUBLISHED, ATTENDANCE_SESSION_CREATED, ATTENDANCE_SESSION_UPDATED, SEMESTER_FEE_GENERATED, PAYMENT_RECORDED, ANNOUNCEMENT_PUBLISHED");
        }
    }

    private String resolveActorRole(AppUserDetails principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ConflictException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private void validateDateRange(LocalDate createdFrom, LocalDate createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ConflictException("createdFrom cannot be after createdTo");
        }
    }

    private Instant toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndOfDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String escaped = text.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
