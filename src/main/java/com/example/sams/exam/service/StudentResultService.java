package com.example.sams.exam.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.dto.StudentResultHistoryResponse;
import com.example.sams.exam.dto.StudentResultSummaryResponse;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.user.service.AppUserDetails;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentResultService {

    private final EnrollmentRepository enrollmentRepository;
    private final MarkEntryRepository markEntryRepository;
    private final ResultCalculationService resultCalculationService;

    public StudentResultService(
            EnrollmentRepository enrollmentRepository,
            MarkEntryRepository markEntryRepository,
            ResultCalculationService resultCalculationService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.markEntryRepository = markEntryRepository;
        this.resultCalculationService = resultCalculationService;
    }

    @Transactional(readOnly = true)
    public Page<StudentResultHistoryResponse> listPublishedResults(Long termId, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findStudentResultEnrollments(getAuthenticatedUserId(), termId, pageable);
        List<StudentResultHistoryResponse> content = enrollments.stream()
                .map(this::toCourseResult)
                .toList();
        return new PageImpl<>(content, pageable, enrollments.getTotalElements());
    }

    @Transactional(readOnly = true)
    public StudentResultSummaryResponse getResultSummary() {
        List<StudentResultHistoryResponse> allResults = enrollmentRepository
                .findStudentResultEnrollments(getAuthenticatedUserId(), null, Pageable.unpaged())
                .stream()
                .map(this::toCourseResult)
                .toList();
        return resultCalculationService.buildSummary(allResults);
    }

    private StudentResultHistoryResponse toCourseResult(Enrollment enrollment) {
        List<MarkEntry> publishedMarkEntries = markEntryRepository.findAllByStudentIdAndExamCourseOfferingIdAndExamPublishedTrue(
                enrollment.getStudent().getId(),
                enrollment.getCourseOffering().getId()
        );
        if (publishedMarkEntries.isEmpty()) {
            throw new ResourceNotFoundException("Published results not found");
        }
        return resultCalculationService.buildCourseResult(enrollment, publishedMarkEntries);
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return principal.getUserId();
    }
}
