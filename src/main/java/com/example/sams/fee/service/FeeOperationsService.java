package com.example.sams.fee.service;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.fee.domain.FeeStructure;
import com.example.sams.fee.domain.PaymentMethod;
import com.example.sams.fee.domain.PaymentRecord;
import com.example.sams.fee.domain.PaymentStatus;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.domain.SemesterFeeStatus;
import com.example.sams.fee.dto.PaymentRecordRequest;
import com.example.sams.fee.dto.PaymentRecordResponse;
import com.example.sams.fee.dto.SemesterFeeGenerationRequest;
import com.example.sams.fee.dto.SemesterFeeResponse;
import com.example.sams.fee.dto.StudentFeeEligibilityResponse;
import com.example.sams.fee.repository.FeeStructureRepository;
import com.example.sams.fee.repository.PaymentRecordRepository;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.user.domain.Student;
import com.example.sams.user.repository.StudentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeOperationsService {

    private final FeeStructureRepository feeStructureRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final StudentRepository studentRepository;
    private final AcademicTermRepository academicTermRepository;
    private final SemesterFeeResponseMapper semesterFeeResponseMapper;
    private final PaymentRecordResponseMapper paymentRecordResponseMapper;
    private final FeePolicyService feePolicyService;

    public FeeOperationsService(
            FeeStructureRepository feeStructureRepository,
            SemesterFeeRepository semesterFeeRepository,
            PaymentRecordRepository paymentRecordRepository,
            StudentRepository studentRepository,
            AcademicTermRepository academicTermRepository,
            SemesterFeeResponseMapper semesterFeeResponseMapper,
            PaymentRecordResponseMapper paymentRecordResponseMapper,
            FeePolicyService feePolicyService
    ) {
        this.feeStructureRepository = feeStructureRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.studentRepository = studentRepository;
        this.academicTermRepository = academicTermRepository;
        this.semesterFeeResponseMapper = semesterFeeResponseMapper;
        this.paymentRecordResponseMapper = paymentRecordResponseMapper;
        this.feePolicyService = feePolicyService;
    }

    @Transactional
    public SemesterFeeResponse generateSemesterFee(SemesterFeeGenerationRequest request) {
        Student student = getStudent(request.studentId());
        AcademicTerm term = getTerm(request.termId());

        semesterFeeRepository.findByStudentIdAndTermId(student.getId(), term.getId()).ifPresent(existing -> {
            throw new ConflictException("Semester fee already exists for this student and term");
        });

        List<FeeStructure> feeStructures = feeStructureRepository.findAllByProgramIdAndTermIdAndActiveTrue(
                student.getProgram().getId(),
                term.getId()
        );
        if (feeStructures.isEmpty()) {
            throw new ConflictException("No active fee structures found for the student's program and term");
        }

        BigDecimal baseAmount = feeStructures.stream()
                .map(FeeStructure::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int earliestDueDays = feeStructures.stream()
                .map(FeeStructure::getDueDaysFromTermStart)
                .min(Comparator.naturalOrder())
                .orElse(0);

        SemesterFee semesterFee = new SemesterFee();
        semesterFee.setStudent(student);
        semesterFee.setTerm(term);
        semesterFee.setFeeStructure(feeStructures.size() == 1 ? feeStructures.get(0) : null);
        semesterFee.setBaseAmount(baseAmount);
        semesterFee.setFineAmount(BigDecimal.ZERO.setScale(2));
        semesterFee.setTotalPayable(baseAmount);
        semesterFee.setPaidAmount(BigDecimal.ZERO.setScale(2));
        semesterFee.setDueDate(term.getStartDate().plusDays(earliestDueDays));
        semesterFee.setStatus(SemesterFeeStatus.DUE);
        semesterFeeRepository.save(semesterFee);
        feePolicyService.synchronizeFeeState(semesterFee);

        return semesterFeeResponseMapper.toResponse(semesterFee);
    }

    @Transactional
    public SemesterFeeResponse getSemesterFee(Long semesterFeeId) {
        SemesterFee semesterFee = getSemesterFeeEntity(semesterFeeId);
        feePolicyService.synchronizeFeeState(semesterFee);
        return semesterFeeResponseMapper.toResponse(semesterFee);
    }

    @Transactional
    public Page<SemesterFeeResponse> listSemesterFees(Long studentId, Long termId, String status, Pageable pageable) {
        SemesterFeeStatus parsedStatus = parseSemesterFeeStatus(status);
        return semesterFeeRepository.search(studentId, termId, parsedStatus, pageable)
                .map(feePolicyService::synchronizeFeeState)
                .map(semesterFeeResponseMapper::toResponse);
    }

    @Transactional
    public PaymentRecordResponse recordPayment(Long semesterFeeId, PaymentRecordRequest request) {
        SemesterFee semesterFee = getSemesterFeeEntity(semesterFeeId);
        feePolicyService.synchronizeFeeState(semesterFee);
        if (paymentRecordRepository.existsByPaymentReference(request.paymentReference().trim())) {
            throw new ConflictException("Payment reference already exists");
        }

        BigDecimal outstanding = semesterFee.getTotalPayable().subtract(semesterFee.getPaidAmount());
        if (request.amount().compareTo(outstanding) > 0) {
            throw new ConflictException("Payment amount cannot exceed the outstanding balance");
        }

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setSemesterFee(semesterFee);
        paymentRecord.setPaymentReference(request.paymentReference().trim());
        paymentRecord.setAmount(request.amount());
        paymentRecord.setPaymentMethod(parsePaymentMethod(request.paymentMethod()));
        paymentRecord.setPaymentStatus(PaymentStatus.RECORDED);
        paymentRecord.setPaidAt(request.paidAt());
        paymentRecord.setRemarks(normalize(request.remarks()));
        paymentRecordRepository.save(paymentRecord);

        semesterFee.setPaidAmount(semesterFee.getPaidAmount().add(request.amount()));
        feePolicyService.synchronizeFeeState(semesterFee);

        return paymentRecordResponseMapper.toResponse(paymentRecord);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRecordResponse> listPaymentHistory(Long semesterFeeId, Pageable pageable) {
        getSemesterFeeEntity(semesterFeeId);
        return paymentRecordRepository.findAllBySemesterFeeId(semesterFeeId, pageable)
                .map(paymentRecordResponseMapper::toResponse);
    }

    @Transactional
    public StudentFeeEligibilityResponse getStudentEligibility(Long studentId, Long termId) {
        Student student = getStudent(studentId);
        return feePolicyService.evaluateEligibility(student, termId);
    }

    @Transactional(readOnly = true)
    private SemesterFee getSemesterFeeEntity(Long semesterFeeId) {
        return semesterFeeRepository.findById(semesterFeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester fee not found"));
    }

    private Student getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private AcademicTerm getTerm(Long termId) {
        return academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }

    private SemesterFeeStatus parseSemesterFeeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return SemesterFeeStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid status. Allowed values: DUE, PARTIALLY_PAID, PAID, OVERDUE");
        }
    }

    private PaymentMethod parsePaymentMethod(String rawMethod) {
        try {
            return PaymentMethod.valueOf(rawMethod.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid paymentMethod. Allowed values: CASH, CARD, UPI, BANK_TRANSFER, SCHOLARSHIP");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
