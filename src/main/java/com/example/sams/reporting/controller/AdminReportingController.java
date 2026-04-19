package com.example.sams.reporting.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.reporting.dto.AdminDashboardSummaryResponse;
import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.reporting.service.AdminReportingService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportingController {

    private final AdminReportingService adminReportingService;

    public AdminReportingController(AdminReportingService adminReportingService) {
        this.adminReportingService = adminReportingService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardSummaryResponse> getDashboardSummary() {
        return ApiResponse.success("Admin dashboard summary fetched successfully", adminReportingService.getDashboardSummary());
    }

    @GetMapping("/fee-defaulters")
    public ApiResponse<PageResponse<FeeDefaulterReportResponse>> getFeeDefaulters(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Fee defaulter report fetched successfully",
                PageResponse.from(adminReportingService.getFeeDefaulters(termId, programId, sectionId, query, pageable))
        );
    }

    @GetMapping("/attendance-shortages")
    public ApiResponse<PageResponse<AttendanceShortageReportResponse>> getAttendanceShortages(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Attendance shortage report fetched successfully",
                PageResponse.from(adminReportingService.getAttendanceShortages(termId, programId, sectionId, query, pageable))
        );
    }
}
