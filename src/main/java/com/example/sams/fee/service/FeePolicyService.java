package com.example.sams.fee.service;

import com.example.sams.common.exception.ConflictException;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.domain.SemesterFeeStatus;
import com.example.sams.fee.dto.StudentFeeEligibilityResponse;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.user.domain.Student;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeePolicyService {

    private static final BigDecimal LATE_FINE_RATE = new BigDecimal("0.10");

    private final SemesterFeeRepository semesterFeeRepository;

    public FeePolicyService(SemesterFeeRepository semesterFeeRepository) {
        this.semesterFeeRepository = semesterFeeRepository;
    }

    @Transactional
    public SemesterFee synchronizeFeeState(SemesterFee semesterFee) {
        LocalDate today = LocalDate.now();
        boolean outstanding = semesterFee.getPaidAmount().compareTo(semesterFee.getTotalPayable()) < 0;

        if (outstanding && today.isAfter(semesterFee.getDueDate()) && semesterFee.getFineAmount().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal fineAmount = semesterFee.getBaseAmount()
                    .multiply(LATE_FINE_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            semesterFee.setFineAmount(fineAmount);
            semesterFee.setTotalPayable(semesterFee.getBaseAmount().add(fineAmount));
        }

        semesterFee.setStatus(resolveStatus(semesterFee, today));
        return semesterFee;
    }

    @Transactional
    public void synchronizeStudentFees(Long studentId) {
        semesterFeeRepository.findAllByStudentId(studentId).forEach(this::synchronizeFeeState);
    }

    @Transactional
    public void assertEnrollmentAllowed(Student student, Long targetTermId) {
        StudentFeeEligibilityResponse eligibility = evaluateEligibility(student, targetTermId);
        if (!eligibility.enrollmentAllowed()) {
            throw new ConflictException("Enrollment blocked due to unpaid dues");
        }
    }

    @Transactional
    public StudentFeeEligibilityResponse evaluateEligibility(Student student, Long targetTermId) {
        List<SemesterFee> blockers = semesterFeeRepository.findBlockingDues(student.getId(), targetTermId).stream()
                .map(this::synchronizeFeeState)
                .toList();

        BigDecimal totalOutstandingAmount = blockers.stream()
                .map(this::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal overdueOutstandingAmount = blockers.stream()
                .filter(semesterFee -> semesterFee.getStatus() == SemesterFeeStatus.OVERDUE)
                .map(this::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new StudentFeeEligibilityResponse(
                blockers.isEmpty(),
                blockers.isEmpty(),
                totalOutstandingAmount,
                overdueOutstandingAmount,
                blockers.stream()
                        .map(semesterFee -> new StudentFeeEligibilityResponse.BlockerSummary(
                                semesterFee.getId(),
                                semesterFee.getTerm().getId(),
                                semesterFee.getTerm().getName(),
                                semesterFee.getTerm().getAcademicYear(),
                                semesterFee.getStatus().name(),
                                outstandingAmount(semesterFee),
                                semesterFee.getStatus() == SemesterFeeStatus.OVERDUE
                                        ? "Overdue semester fee blocks academic actions"
                                        : "Outstanding semester fee blocks academic actions"
                        ))
                        .toList()
        );
    }

    private SemesterFeeStatus resolveStatus(SemesterFee semesterFee, LocalDate today) {
        if (semesterFee.getPaidAmount().compareTo(semesterFee.getTotalPayable()) >= 0) {
            return SemesterFeeStatus.PAID;
        }
        if (today.isAfter(semesterFee.getDueDate())) {
            return SemesterFeeStatus.OVERDUE;
        }
        if (semesterFee.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            return SemesterFeeStatus.PARTIALLY_PAID;
        }
        return SemesterFeeStatus.DUE;
    }

    private BigDecimal outstandingAmount(SemesterFee semesterFee) {
        return semesterFee.getTotalPayable()
                .subtract(semesterFee.getPaidAmount())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
