package com.example.sams.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarkEntryResponse(
        Long id,
        Long examId,
        StudentSummary student,
        BigDecimal marksObtained,
        BigDecimal maxMarks,
        String remarks,
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
}
