package com.example.sams.academic.dto;

import java.time.Instant;

public record ProgramResponse(
        Long id,
        String code,
        String name,
        DepartmentSummary department,
        Instant createdAt,
        Instant updatedAt
) {
    public record DepartmentSummary(
            Long id,
            String code,
            String name
    ) {
    }
}
