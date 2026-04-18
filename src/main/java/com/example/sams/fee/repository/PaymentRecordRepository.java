package com.example.sams.fee.repository;

import com.example.sams.fee.domain.PaymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    boolean existsByPaymentReference(String paymentReference);

    Page<PaymentRecord> findAllBySemesterFeeId(Long semesterFeeId, Pageable pageable);
}
