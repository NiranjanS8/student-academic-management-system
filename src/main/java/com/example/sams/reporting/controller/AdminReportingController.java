package com.example.sams.reporting.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.reporting.dto.AdminDashboardSummaryResponse;
import com.example.sams.reporting.dto.AdminStudentAnalyticsSummaryResponse;
import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.reporting.dto.PublishedResultSummaryResponse;
import com.example.sams.reporting.dto.StudentDistributionReportResponse;
import com.example.sams.reporting.dto.StudentAcademicSnapshotResponse;
import com.example.sams.reporting.dto.TeacherWorkloadReportResponse;
import com.example.sams.reporting.service.AdminReportCsvExportService;
import com.example.sams.reporting.service.AdminReportingService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportingController {

    private final AdminReportingService adminReportingService;
    private final AdminReportCsvExportService adminReportCsvExportService;

    public AdminReportingController(
            AdminReportingService adminReportingService,
            AdminReportCsvExportService adminReportCsvExportService
    ) {
        this.adminReportingService = adminReportingService;
        this.adminReportCsvExportService = adminReportCsvExportService;
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

    @GetMapping(value = "/fee-defaulters/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportFeeDefaulters(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String query
    ) {
        return csvResponse(
                "fee-defaulters",
                adminReportCsvExportService.exportFeeDefaulters(termId, programId, sectionId, query)
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

    @GetMapping(value = "/attendance-shortages/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAttendanceShortages(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String query
    ) {
        return csvResponse(
                "attendance-shortages",
                adminReportCsvExportService.exportAttendanceShortages(termId, programId, sectionId, query)
        );
    }

    @GetMapping("/results-summary")
    public ApiResponse<PublishedResultSummaryResponse> getPublishedResultSummary(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long courseOfferingId
    ) {
        return ApiResponse.success(
                "Published result summary fetched successfully",
                adminReportingService.getPublishedResultSummary(termId, courseOfferingId)
        );
    }

    @GetMapping("/students/{studentId}/academic-snapshot")
    public ApiResponse<StudentAcademicSnapshotResponse> getStudentAcademicSnapshot(@org.springframework.web.bind.annotation.PathVariable Long studentId) {
        return ApiResponse.success(
                "Student academic snapshot fetched successfully",
                adminReportingService.getStudentAcademicSnapshot(studentId)
        );
    }

    @GetMapping("/student-distribution")
    public ApiResponse<PageResponse<StudentDistributionReportResponse>> getStudentDistribution(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String academicStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCount") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Student distribution report fetched successfully",
                PageResponse.from(adminReportingService.getStudentDistribution(departmentId, programId, termId, sectionId, academicStatus, pageable))
        );
    }

    @GetMapping(value = "/student-distribution/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportStudentDistribution(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String academicStatus
    ) {
        return csvResponse(
                "student-distribution",
                adminReportCsvExportService.exportStudentDistribution(departmentId, programId, termId, sectionId, academicStatus)
        );
    }

    @GetMapping("/student-analytics")
    public ApiResponse<AdminStudentAnalyticsSummaryResponse> getStudentAnalytics() {
        return ApiResponse.success(
                "Student analytics summary fetched successfully",
                adminReportingService.getStudentAnalyticsSummary()
        );
    }

    @GetMapping("/teacher-workloads")
    public ApiResponse<PageResponse<TeacherWorkloadReportResponse>> getTeacherWorkloads(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeCode") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Teacher workload report fetched successfully",
                PageResponse.from(adminReportingService.getTeacherWorkloads(departmentId, termId, query, pageable))
        );
    }

    @GetMapping(value = "/teacher-workloads/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportTeacherWorkloads(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String query
    ) {
        return csvResponse(
                "teacher-workloads",
                adminReportCsvExportService.exportTeacherWorkloads(departmentId, termId, query)
        );
    }

    private ResponseEntity<byte[]> csvResponse(String reportName, byte[] content) {
        String filename = reportName + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(content);
    }
}
