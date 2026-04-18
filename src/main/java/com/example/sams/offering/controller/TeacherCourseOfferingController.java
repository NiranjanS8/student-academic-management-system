package com.example.sams.offering.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.offering.service.TeacherCourseOfferingService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/offerings")
public class TeacherCourseOfferingController {

    private final TeacherCourseOfferingService teacherCourseOfferingService;

    public TeacherCourseOfferingController(TeacherCourseOfferingService teacherCourseOfferingService) {
        this.teacherCourseOfferingService = teacherCourseOfferingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CourseOfferingResponse>> listAssignedOfferings(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Assigned course offerings fetched successfully",
                PageResponse.from(teacherCourseOfferingService.listAssignedOfferings(
                        termId,
                        sectionId,
                        subjectId,
                        status,
                        pageable
                ))
        );
    }

    @GetMapping("/{offeringId}")
    public ApiResponse<CourseOfferingResponse> getAssignedOffering(@PathVariable Long offeringId) {
        return ApiResponse.success(
                "Assigned course offering fetched successfully",
                teacherCourseOfferingService.getAssignedOfferingById(offeringId)
        );
    }
}
