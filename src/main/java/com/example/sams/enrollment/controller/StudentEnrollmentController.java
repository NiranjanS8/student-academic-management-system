package com.example.sams.enrollment.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.enrollment.dto.EnrollmentRequest;
import com.example.sams.enrollment.dto.EnrollmentResponse;
import com.example.sams.enrollment.service.StudentEnrollmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/enrollments")
public class StudentEnrollmentController {

    private final StudentEnrollmentService studentEnrollmentService;

    public StudentEnrollmentController(StudentEnrollmentService studentEnrollmentService) {
        this.studentEnrollmentService = studentEnrollmentService;
    }

    @PostMapping
    public ApiResponse<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentRequest request) {
        return ApiResponse.success("Enrolled successfully", studentEnrollmentService.enroll(request));
    }

    @PostMapping("/{enrollmentId}/drop")
    public ApiResponse<EnrollmentResponse> drop(@PathVariable Long enrollmentId) {
        return ApiResponse.success("Enrollment dropped successfully", studentEnrollmentService.drop(enrollmentId));
    }

    @GetMapping
    public ApiResponse<PageResponse<EnrollmentResponse>> listCurrentEnrollments(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrolledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Current enrollments fetched successfully",
                PageResponse.from(studentEnrollmentService.listCurrentEnrollments(termId, status, pageable))
        );
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<EnrollmentResponse>> listEnrollmentHistory(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrolledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Enrollment history fetched successfully",
                PageResponse.from(studentEnrollmentService.listEnrollmentHistory(termId, status, pageable))
        );
    }
}
