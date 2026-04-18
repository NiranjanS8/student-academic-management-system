package com.example.sams.exam.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.exam.dto.ExamRequest;
import com.example.sams.exam.dto.ExamResponse;
import com.example.sams.exam.dto.MarkEntryRequest;
import com.example.sams.exam.dto.MarkEntryResponse;
import com.example.sams.exam.service.TeacherExamService;
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
@RequestMapping("/api/v1/teacher/exams")
public class TeacherExamController {

    private final TeacherExamService teacherExamService;

    public TeacherExamController(TeacherExamService teacherExamService) {
        this.teacherExamService = teacherExamService;
    }

    @PostMapping
    public ApiResponse<ExamResponse> createExam(@Valid @RequestBody ExamRequest request) {
        return ApiResponse.success("Exam created successfully", teacherExamService.createExam(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ExamResponse>> listAssignedExams(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Assigned exams fetched successfully",
                PageResponse.from(teacherExamService.listAssignedExams(offeringId, pageable))
        );
    }

    @GetMapping("/{examId}")
    public ApiResponse<ExamResponse> getAssignedExam(@PathVariable Long examId) {
        return ApiResponse.success("Assigned exam fetched successfully", teacherExamService.getAssignedExam(examId));
    }

    @PostMapping("/{examId}/marks")
    public ApiResponse<MarkEntryResponse> createMarkEntry(
            @PathVariable Long examId,
            @Valid @RequestBody MarkEntryRequest request
    ) {
        return ApiResponse.success("Mark entry created successfully", teacherExamService.createMarkEntry(examId, request));
    }

    @PutMapping("/{examId}/marks/{markEntryId}")
    public ApiResponse<MarkEntryResponse> updateMarkEntry(
            @PathVariable Long examId,
            @PathVariable Long markEntryId,
            @Valid @RequestBody MarkEntryRequest request
    ) {
        return ApiResponse.success("Mark entry updated successfully", teacherExamService.updateMarkEntry(examId, markEntryId, request));
    }

    @GetMapping("/{examId}/marks")
    public ApiResponse<PageResponse<MarkEntryResponse>> listMarkEntries(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Mark entries fetched successfully",
                PageResponse.from(teacherExamService.listMarkEntries(examId, pageable))
        );
    }
}
