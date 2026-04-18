package com.example.sams.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExamResponse(
        Long id,
        Long courseOfferingId,
        String title,
        String examType,
        BigDecimal maxMarks,
        BigDecimal weightage,
        Instant scheduledAt,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {
}
