package com.example.sams.offering.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.offering.dto.CourseOfferingRequest;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.offering.service.CourseOfferingAdministrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/offerings")
public class CourseOfferingAdministrationController {

    private final CourseOfferingAdministrationService courseOfferingAdministrationService;

    public CourseOfferingAdministrationController(CourseOfferingAdministrationService courseOfferingAdministrationService) {
        this.courseOfferingAdministrationService = courseOfferingAdministrationService;
    }

    @PostMapping
    public ApiResponse<CourseOfferingResponse> createCourseOffering(@Valid @RequestBody CourseOfferingRequest request) {
        return ApiResponse.success(
                "Course offering created successfully",
                courseOfferingAdministrationService.createCourseOffering(request)
        );
    }

    @PutMapping("/{offeringId}")
    public ApiResponse<CourseOfferingResponse> updateCourseOffering(
            @PathVariable Long offeringId,
            @Valid @RequestBody CourseOfferingRequest request
    ) {
        return ApiResponse.success(
                "Course offering updated successfully",
                courseOfferingAdministrationService.updateCourseOffering(offeringId, request)
        );
    }

    @GetMapping("/{offeringId}")
    public ApiResponse<CourseOfferingResponse> getCourseOffering(@PathVariable Long offeringId) {
        return ApiResponse.success(
                "Course offering fetched successfully",
                courseOfferingAdministrationService.getCourseOfferingById(offeringId)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<CourseOfferingResponse>> listCourseOfferings(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Course offerings fetched successfully",
                PageResponse.from(courseOfferingAdministrationService.listCourseOfferings(
                        termId,
                        sectionId,
                        teacherId,
                        subjectId,
                        status,
                        pageable
                ))
        );
    }
}
