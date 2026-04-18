package com.example.sams.fee.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentFeeEligibilityResponse(
        boolean enrollmentAllowed,
        boolean hallTicketEligible,
        BigDecimal totalOutstandingAmount,
        BigDecimal overdueOutstandingAmount,
        List<BlockerSummary> blockers
) {
    public record BlockerSummary(
            Long semesterFeeId,
            Long termId,
            String termName,
            String academicYear,
            String status,
            BigDecimal outstandingAmount,
            String reason
    ) {
    }
}
