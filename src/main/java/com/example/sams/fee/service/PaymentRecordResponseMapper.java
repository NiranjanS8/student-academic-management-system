package com.example.sams.fee.service;

import com.example.sams.fee.domain.PaymentRecord;
import com.example.sams.fee.dto.PaymentRecordResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentRecordResponseMapper {

    public PaymentRecordResponse toResponse(PaymentRecord paymentRecord) {
        return new PaymentRecordResponse(
                paymentRecord.getId(),
                paymentRecord.getSemesterFee().getId(),
                paymentRecord.getPaymentReference(),
                paymentRecord.getAmount(),
                paymentRecord.getPaymentMethod().name(),
                paymentRecord.getPaymentStatus().name(),
                paymentRecord.getPaidAt(),
                paymentRecord.getRemarks(),
                paymentRecord.getCreatedAt(),
                paymentRecord.getUpdatedAt()
        );
    }
}
