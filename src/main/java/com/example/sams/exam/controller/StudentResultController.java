package com.example.sams.exam.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.exam.dto.StudentResultHistoryResponse;
import com.example.sams.exam.dto.StudentResultSummaryResponse;
import com.example.sams.exam.service.StudentResultService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/results")
public class StudentResultController {

    private final StudentResultService studentResultService;

    public StudentResultController(StudentResultService studentResultService) {
        this.studentResultService = studentResultService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StudentResultHistoryResponse>> listPublishedResults(
            @RequestParam(required = false) Long termId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Published results fetched successfully",
                PageResponse.from(studentResultService.listPublishedResults(termId, pageable))
        );
    }

    @GetMapping("/summary")
    public ApiResponse<StudentResultSummaryResponse> getResultSummary() {
        return ApiResponse.success("Result summary fetched successfully", studentResultService.getResultSummary());
    }
}
