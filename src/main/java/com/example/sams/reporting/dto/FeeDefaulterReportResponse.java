package com.example.sams.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeDefaulterReportResponse(
        Long semesterFeeId,
        Long studentId,
        String studentCode,
        String studentUsername,
        Long termId,
        String termName,
        String academicYear,
        String status,
        BigDecimal totalPayable,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDate dueDate
) {
}
