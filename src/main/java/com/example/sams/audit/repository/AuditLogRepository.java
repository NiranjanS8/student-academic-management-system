package com.example.sams.audit.repository;

import com.example.sams.audit.domain.AuditActionType;
import com.example.sams.audit.domain.AuditLog;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select al from AuditLog al
            where (:actionType is null or al.actionType = :actionType)
              and (:actorUserId is null or al.actorUserId = :actorUserId)
              and (:entityType is null or lower(al.entityType) = lower(:entityType))
              and (:entityId is null or al.entityId = :entityId)
              and (:createdFrom is null or al.createdAt >= :createdFrom)
              and (:createdTo is null or al.createdAt <= :createdTo)
            """)
    Page<AuditLog> search(
            @Param("actionType") AuditActionType actionType,
            @Param("actorUserId") Long actorUserId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            Pageable pageable
    );
}
