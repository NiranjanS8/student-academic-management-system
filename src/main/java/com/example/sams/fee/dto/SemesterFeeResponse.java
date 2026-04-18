package com.example.sams.fee.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SemesterFeeResponse(
        Long id,
        StudentSummary student,
        TermSummary term,
        BigDecimal baseAmount,
        BigDecimal fineAmount,
        BigDecimal totalPayable,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDate dueDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public record StudentSummary(
            Long id,
            String studentCode,
            String username,
            String email
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
