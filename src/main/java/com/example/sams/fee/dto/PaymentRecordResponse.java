package com.example.sams.fee.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRecordResponse(
        Long id,
        Long semesterFeeId,
        String paymentReference,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus,
        Instant paidAt,
        String remarks,
        Instant createdAt,
        Instant updatedAt
) {
}
