package com.example.sams.fee.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeStructureResponse(
        Long id,
        String name,
        String feeCategory,
        BigDecimal amount,
        Integer dueDaysFromTermStart,
        String description,
        boolean active,
        ProgramSummary program,
        TermSummary term,
        Instant createdAt,
        Instant updatedAt
) {
    public record ProgramSummary(
            Long id,
            String code,
            String name
    ) {
    }

    public record TermSummary(
            Long id,
            String name,
            String academicYear,
            String status
    ) {
    }
}
