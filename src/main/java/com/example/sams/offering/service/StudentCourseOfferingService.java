package com.example.sams.offering.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.offering.repository.CourseOfferingRepository;
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
public class StudentCourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentRepository studentRepository;
    private final CourseOfferingResponseMapper courseOfferingResponseMapper;

    public StudentCourseOfferingService(
            CourseOfferingRepository courseOfferingRepository,
            StudentRepository studentRepository,
            CourseOfferingResponseMapper courseOfferingResponseMapper
    ) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.studentRepository = studentRepository;
        this.courseOfferingResponseMapper = courseOfferingResponseMapper;
    }

    @Transactional(readOnly = true)
    public Page<CourseOfferingResponse> listAvailableOfferings(Long subjectId, String status, Pageable pageable) {
        Student student = getAuthenticatedStudent();
        CourseOfferingStatus parsedStatus = (status == null || status.isBlank())
                ? CourseOfferingStatus.OPEN
                : parseStatus(status);

        return courseOfferingRepository.searchVisibleToStudent(
                getRequiredSectionId(student),
                getRequiredTermId(student),
                subjectId,
                parsedStatus,
                pageable
        ).map(courseOfferingResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseOfferingResponse getAvailableOfferingById(Long offeringId) {
        Student student = getAuthenticatedStudent();
        CourseOffering offering = courseOfferingRepository.findVisibleToStudent(
                        offeringId,
                        getRequiredSectionId(student),
                        getRequiredTermId(student)
                )
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));
        return courseOfferingResponseMapper.toResponse(offering);
    }

    private Student getAuthenticatedStudent() {
        Long userId = getAuthenticatedUserId();
        return studentRepository.findByUserId(userId)
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

    private CourseOfferingStatus parseStatus(String rawStatus) {
        try {
            return CourseOfferingStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid status. Allowed values: DRAFT, OPEN, CLOSED, ARCHIVED");
        }
    }
}
