package com.example.sams.enrollment.service;

import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.dto.EnrollmentResponse;
import com.example.sams.offering.service.CourseOfferingResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentResponseMapper {

    private final CourseOfferingResponseMapper courseOfferingResponseMapper;

    public EnrollmentResponseMapper(CourseOfferingResponseMapper courseOfferingResponseMapper) {
        this.courseOfferingResponseMapper = courseOfferingResponseMapper;
    }

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStatus().name(),
                enrollment.getEnrolledAt(),
                enrollment.getDroppedAt(),
                courseOfferingResponseMapper.toResponse(enrollment.getCourseOffering())
        );
    }
}
