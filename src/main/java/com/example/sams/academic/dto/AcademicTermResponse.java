package com.example.sams.academic.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AcademicTermResponse(
        Long id,
        String name,
        String academicYear,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
