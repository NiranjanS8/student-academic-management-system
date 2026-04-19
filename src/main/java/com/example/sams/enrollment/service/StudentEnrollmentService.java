package com.example.sams.enrollment.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.audit.domain.AuditActionType;
import com.example.sams.audit.service.AuditLogService;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.dto.EnrollmentRequest;
import com.example.sams.enrollment.dto.EnrollmentResponse;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.fee.service.FeePolicyService;
import com.example.sams.notification.service.NotificationService;
import com.example.sams.offering.domain.CourseOfferingStatus;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.Student;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.service.AppUserDetails;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentEnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentResponseMapper enrollmentResponseMapper;
    private final EnrollmentPrerequisiteValidator enrollmentPrerequisiteValidator;
    private final FeePolicyService feePolicyService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public StudentEnrollmentService(
            EnrollmentRepository enrollmentRepository,
            CourseOfferingRepository courseOfferingRepository,
            StudentRepository studentRepository,
            EnrollmentResponseMapper enrollmentResponseMapper,
            EnrollmentPrerequisiteValidator enrollmentPrerequisiteValidator,
            FeePolicyService feePolicyService,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.studentRepository = studentRepository;
        this.enrollmentResponseMapper = enrollmentResponseMapper;
        this.enrollmentPrerequisiteValidator = enrollmentPrerequisiteValidator;
        this.feePolicyService = feePolicyService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EnrollmentResponse enroll(EnrollmentRequest request) {
        Student student = getAuthenticatedStudent();
        CourseOffering offering = getVisibleOffering(student, request.courseOfferingId());
        validateOfferingIsEnrollAble(offering);
        feePolicyService.assertEnrollmentAllowed(student, offering.getTerm().getId());
        enrollmentPrerequisiteValidator.validate(student, offering);

        Enrollment existingEnrollment = enrollmentRepository.findByStudentIdAndCourseOfferingId(student.getId(), offering.getId())
                .orElse(null);

        if (existingEnrollment != null) {
            if (existingEnrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                throw new ConflictException("Student is already actively enrolled in this offering");
            }

            existingEnrollment.setStatus(EnrollmentStatus.ENROLLED);
            existingEnrollment.setEnrolledAt(Instant.now());
            existingEnrollment.setDroppedAt(null);
            notificationService.notifyEnrollmentConfirmed(existingEnrollment);
            auditLogService.log(
                    AuditActionType.ENROLLMENT_ENROLLED,
                    "ENROLLMENT",
                    existingEnrollment.getId(),
                    "Student %s enrolled in %s".formatted(
                            student.getStudentCode(),
                            offering.getSubject().getCode()
                    )
            );
            return enrollmentResponseMapper.toResponse(existingEnrollment);
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourseOffering(offering);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledAt(Instant.now());
        enrollmentRepository.save(enrollment);
        notificationService.notifyEnrollmentConfirmed(enrollment);
        auditLogService.log(
                AuditActionType.ENROLLMENT_ENROLLED,
                "ENROLLMENT",
                enrollment.getId(),
                "Student %s enrolled in %s".formatted(
                        student.getStudentCode(),
                        offering.getSubject().getCode()
                )
        );

        return enrollmentResponseMapper.toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse drop(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndStudentUserId(enrollmentId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new ConflictException("Enrollment is already dropped");
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setDroppedAt(Instant.now());
        auditLogService.log(
                AuditActionType.ENROLLMENT_DROPPED,
                "ENROLLMENT",
                enrollment.getId(),
                "Student %s dropped %s".formatted(
                        enrollment.getStudent().getStudentCode(),
                        enrollment.getCourseOffering().getSubject().getCode()
                )
        );
        return enrollmentResponseMapper.toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listCurrentEnrollments(Long termId, String status, Pageable pageable) {
        Student student = getAuthenticatedStudent();
        Long effectiveTermId = termId != null ? termId : getRequiredTermId(student);
        EnrollmentStatus parsedStatus = (status == null || status.isBlank())
                ? EnrollmentStatus.ENROLLED
                : parseStatus(status);

        return enrollmentRepository.searchStudentEnrollments(getAuthenticatedUserId(), effectiveTermId, parsedStatus, pageable)
                .map(enrollmentResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listEnrollmentHistory(Long termId, String status, Pageable pageable) {
        EnrollmentStatus parsedStatus = (status == null || status.isBlank()) ? null : parseStatus(status);
        return enrollmentRepository.searchStudentEnrollments(getAuthenticatedUserId(), termId, parsedStatus, pageable)
                .map(enrollmentResponseMapper::toResponse);
    }

    private void validateOfferingIsEnrollAble(CourseOffering offering) {
        Instant now = Instant.now();

        if (offering.getStatus() != CourseOfferingStatus.OPEN) {
            throw new ConflictException("Enrollment is only allowed for OPEN offerings");
        }
        if (offering.getEnrollmentOpenAt() != null && now.isBefore(offering.getEnrollmentOpenAt())) {
            throw new ConflictException("Enrollment has not opened for this offering yet");
        }
        if (offering.getEnrollmentCloseAt() != null && now.isAfter(offering.getEnrollmentCloseAt())) {
            throw new ConflictException("Enrollment window has closed for this offering");
        }

        long activeEnrollments = enrollmentRepository.countByCourseOfferingIdAndStatus(
                offering.getId(),
                EnrollmentStatus.ENROLLED
        );
        if (activeEnrollments >= offering.getCapacity()) {
            throw new ConflictException("Offering capacity has been reached");
        }
    }

    private CourseOffering getVisibleOffering(Student student, Long offeringId) {
        Long sectionId = getRequiredSectionId(student);
        Long termId = getRequiredTermId(student);
        return courseOfferingRepository.findVisibleToStudent(offeringId, sectionId, termId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));
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

    private Long getRequiredSectionId(Student student) {
        if (student.getSection() == null) {
            throw new ConflictException("Student is not assigned to a section");
        }
        return student.getSection().getId();
    }

    private Long getRequiredTermId(Student student) {
        if (student.getCurrentTerm() == null) {
            throw new ConflictException("Student does not have a current term assigned");
        }
        return student.getCurrentTerm().getId();
    }

    private EnrollmentStatus parseStatus(String rawStatus) {
        try {
            return EnrollmentStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid status. Allowed values: ENROLLED, DROPPED");
        }
    }
}
