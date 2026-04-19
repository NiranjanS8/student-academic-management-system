package com.example.sams.reporting.service;

import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.reporting.dto.StudentDistributionReportResponse;
import com.example.sams.reporting.dto.TeacherWorkloadReportResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportCsvExportService {

    private static final int EXPORT_PAGE_SIZE = 10_000;

    private final AdminReportingService adminReportingService;

    public AdminReportCsvExportService(AdminReportingService adminReportingService) {
        this.adminReportingService = adminReportingService;
    }

    @Transactional(readOnly = true)
    public byte[] exportFeeDefaulters(Long termId, Long programId, Long sectionId, String query) {
        List<FeeDefaulterReportResponse> rows = adminReportingService
                .getFeeDefaulters(
                        termId,
                        programId,
                        sectionId,
                        query,
                        PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("dueDate").ascending())
                )
                .getContent();

        return toCsv(
                List.of("semesterFeeId", "studentId", "studentCode", "studentUsername", "termId", "termName", "academicYear", "status", "totalPayable", "paidAmount", "outstandingAmount", "dueDate"),
                rows.stream()
                        .map(row -> List.of(
                                row.semesterFeeId(),
                                row.studentId(),
                                row.studentCode(),
                                row.studentUsername(),
                                row.termId(),
                                row.termName(),
                                row.academicYear(),
                                row.status(),
                                row.totalPayable(),
                                row.paidAmount(),
                                row.outstandingAmount(),
                                row.dueDate()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportAttendanceShortages(Long termId, Long programId, Long sectionId, String query) {
        List<AttendanceShortageReportResponse> rows = adminReportingService
                .getAttendanceShortages(
                        termId,
                        programId,
                        sectionId,
                        query,
                        PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("studentId").ascending())
                )
                .getContent();

        return toCsv(
                List.of("studentId", "studentCode", "studentUsername", "courseOfferingId", "subjectCode", "subjectName", "totalSessions", "presentSessions", "attendancePercentage"),
                rows.stream()
                        .map(row -> Arrays.asList(
                                row.studentId(),
                                row.studentCode(),
                                row.studentUsername(),
                                row.courseOfferingId(),
                                row.subjectCode(),
                                row.subjectName(),
                                row.totalSessions(),
                                row.presentSessions(),
                                row.attendancePercentage()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportTeacherWorkloads(Long departmentId, Long termId, String query) {
        List<TeacherWorkloadReportResponse> rows = adminReportingService
                .getTeacherWorkloads(
                        departmentId,
                        termId,
                        query,
                        PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("employeeCode").ascending())
                )
                .getContent();

        return toCsv(
                List.of("teacherId", "employeeCode", "teacherUsername", "teacherEmail", "departmentName", "designation", "totalOfferings", "openOfferings", "closedOfferings", "archivedOfferings", "totalAssignedStudents", "publishedExamCount", "averageCapacityUtilization"),
                rows.stream()
                        .map(row -> Arrays.asList(
                                row.teacherId(),
                                row.employeeCode(),
                                row.teacherUsername(),
                                row.teacherEmail(),
                                row.departmentName(),
                                row.designation(),
                                row.totalOfferings(),
                                row.openOfferings(),
                                row.closedOfferings(),
                                row.archivedOfferings(),
                                row.totalAssignedStudents(),
                                row.publishedExamCount(),
                                row.averageCapacityUtilization()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportStudentDistribution(Long departmentId, Long programId, Long termId, Long sectionId, String academicStatus) {
        List<StudentDistributionReportResponse> rows = adminReportingService
                .getStudentDistribution(
                        departmentId,
                        programId,
                        termId,
                        sectionId,
                        academicStatus,
                        PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("studentCount").descending())
                )
                .getContent();

        return toCsv(
                List.of("departmentId", "departmentName", "programId", "programName", "termId", "termName", "academicYear", "sectionId", "sectionName", "academicStatus", "studentCount"),
                rows.stream()
                        .map(row -> Arrays.asList(
                                row.departmentId(),
                                row.departmentName(),
                                row.programId(),
                                row.programName(),
                                row.termId(),
                                row.termName(),
                                row.academicYear(),
                                row.sectionId(),
                                row.sectionName(),
                                row.academicStatus(),
                                row.studentCount()
                        ))
                        .toList()
        );
    }

    private byte[] toCsv(List<String> headers, List<? extends List<?>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", headers)).append(System.lineSeparator());
        for (List<?> row : rows) {
            builder.append(row.stream().map(this::escape).collect(java.util.stream.Collectors.joining(",")))
                    .append(System.lineSeparator());
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String escaped = text.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
