package com.example.sams.audit.controller;

import com.example.sams.audit.dto.AuditLogResponse;
import com.example.sams.audit.service.AuditLogService;
import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    public ApiResponse<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Audit logs fetched successfully",
                PageResponse.from(auditLogService.search(actionType, actorUserId, entityType, entityId, createdFrom, createdTo, pageable))
        );
    }

    @GetMapping(value = "/logs/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo
    ) {
        String filename = "audit-logs-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(auditLogService.exportCsv(actionType, actorUserId, entityType, entityId, createdFrom, createdTo));
    }
}
