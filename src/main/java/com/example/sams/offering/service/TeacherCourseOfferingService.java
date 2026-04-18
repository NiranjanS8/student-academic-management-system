package com.example.sams.offering.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.service.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherCourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseOfferingResponseMapper courseOfferingResponseMapper;

    public TeacherCourseOfferingService(
            CourseOfferingRepository courseOfferingRepository,
            CourseOfferingResponseMapper courseOfferingResponseMapper
    ) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseOfferingResponseMapper = courseOfferingResponseMapper;
    }

    @Transactional(readOnly = true)
    public Page<CourseOfferingResponse> listAssignedOfferings(
            Long termId,
            Long sectionId,
            Long subjectId,
            String status,
            Pageable pageable
    ) {
        CourseOfferingStatus parsedStatus = (status == null || status.isBlank()) ? null : parseStatus(status);
        return courseOfferingRepository.searchAssignedToTeacher(
                getAuthenticatedUserId(),
                termId,
                sectionId,
                subjectId,
                parsedStatus,
                pageable
        ).map(courseOfferingResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseOfferingResponse getAssignedOfferingById(Long offeringId) {
        CourseOffering offering = courseOfferingRepository.findByIdAndTeacherUserId(offeringId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));
        return courseOfferingResponseMapper.toResponse(offering);
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return principal.getUserId();
    }

    private CourseOfferingStatus parseStatus(String rawStatus) {
        try {
            return CourseOfferingStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid status. Allowed values: DRAFT, OPEN, CLOSED, ARCHIVED");
        }
    }
}
