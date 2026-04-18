package com.example.sams.fee.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.fee.dto.PaymentRecordResponse;
import com.example.sams.fee.dto.SemesterFeeResponse;
import com.example.sams.fee.dto.StudentFeeEligibilityResponse;
import com.example.sams.fee.repository.PaymentRecordRepository;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.user.domain.Student;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.service.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentFeeService {

    private final StudentRepository studentRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final FeePolicyService feePolicyService;
    private final SemesterFeeResponseMapper semesterFeeResponseMapper;
    private final PaymentRecordResponseMapper paymentRecordResponseMapper;

    public StudentFeeService(
            StudentRepository studentRepository,
            SemesterFeeRepository semesterFeeRepository,
            PaymentRecordRepository paymentRecordRepository,
            FeePolicyService feePolicyService,
            SemesterFeeResponseMapper semesterFeeResponseMapper,
            PaymentRecordResponseMapper paymentRecordResponseMapper
    ) {
        this.studentRepository = studentRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.feePolicyService = feePolicyService;
        this.semesterFeeResponseMapper = semesterFeeResponseMapper;
        this.paymentRecordResponseMapper = paymentRecordResponseMapper;
    }

    @Transactional
    public Page<SemesterFeeResponse> listOwnFees(Pageable pageable) {
        return semesterFeeRepository.findAllByStudentUserId(getAuthenticatedUserId(), pageable)
                .map(feePolicyService::synchronizeFeeState)
                .map(semesterFeeResponseMapper::toResponse);
    }

    @Transactional
    public StudentFeeEligibilityResponse getEligibility(Long termId) {
        return feePolicyService.evaluateEligibility(getAuthenticatedStudent(), termId);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRecordResponse> listOwnPaymentHistory(Long semesterFeeId, Pageable pageable) {
        semesterFeeRepository.findById(semesterFeeId)
                .filter(semesterFee -> semesterFee.getStudent().getUser().getId().equals(getAuthenticatedUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Semester fee not found"));
        return paymentRecordRepository.findAllBySemesterFeeId(semesterFeeId, pageable)
                .map(paymentRecordResponseMapper::toResponse);
    }

    private Student getAuthenticatedStudent() {
        return studentRepository.findByUserId(getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return principal.getUserId();
    }
}
