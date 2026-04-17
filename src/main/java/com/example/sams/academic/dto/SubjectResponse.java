package com.example.sams.academic.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SubjectResponse(
        Long id,
        String code,
        String name,
        BigDecimal credits,
        boolean active,
        DepartmentSummary department,
        List<PrerequisiteSummary> prerequisites,
        Instant createdAt,
        Instant updatedAt
) {
    public record DepartmentSummary(
            Long id,
            String code,
            String name
    ) {
    }

    public record PrerequisiteSummary(
            Long id,
            String code,
            String name
    ) {
    }
}
