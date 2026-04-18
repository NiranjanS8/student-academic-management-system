package com.example.sams.offering.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.offering.service.StudentCourseOfferingService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/offerings")
public class StudentCourseOfferingController {

    private final StudentCourseOfferingService studentCourseOfferingService;

    public StudentCourseOfferingController(StudentCourseOfferingService studentCourseOfferingService) {
        this.studentCourseOfferingService = studentCourseOfferingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CourseOfferingResponse>> listAvailableOfferings(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Available course offerings fetched successfully",
                PageResponse.from(studentCourseOfferingService.listAvailableOfferings(subjectId, status, pageable))
        );
    }

    @GetMapping("/{offeringId}")
    public ApiResponse<CourseOfferingResponse> getAvailableOffering(@PathVariable Long offeringId) {
        return ApiResponse.success(
                "Available course offering fetched successfully",
                studentCourseOfferingService.getAvailableOfferingById(offeringId)
        );
    }
}
