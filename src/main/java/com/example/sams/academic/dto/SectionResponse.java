package com.example.sams.academic.dto;

import java.time.Instant;

public record SectionResponse(
        Long id,
        String name,
        ProgramSummary program,
        AcademicTermSummary currentTerm,
        Instant createdAt,
        Instant updatedAt
) {
    public record ProgramSummary(
            Long id,
            String code,
            String name
    ) {
    }

    public record AcademicTermSummary(
            Long id,
            String name,
            String academicYear,
            String status
    ) {
    }
}
