package com.example.sams.academic.dto;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
