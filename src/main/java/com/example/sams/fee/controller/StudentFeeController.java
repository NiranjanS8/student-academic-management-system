package com.example.sams.fee.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.fee.dto.PaymentRecordResponse;
import com.example.sams.fee.dto.SemesterFeeResponse;
import com.example.sams.fee.dto.StudentFeeEligibilityResponse;
import com.example.sams.fee.service.StudentFeeService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/fees")
public class StudentFeeController {

    private final StudentFeeService studentFeeService;

    public StudentFeeController(StudentFeeService studentFeeService) {
        this.studentFeeService = studentFeeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SemesterFeeResponse>> listOwnFees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Student fees fetched successfully",
                PageResponse.from(studentFeeService.listOwnFees(pageable))
        );
    }

    @GetMapping("/eligibility")
    public ApiResponse<StudentFeeEligibilityResponse> getEligibility(@RequestParam(required = false) Long termId) {
        return ApiResponse.success("Student fee eligibility fetched successfully", studentFeeService.getEligibility(termId));
    }

    @GetMapping("/{semesterFeeId}/payments")
    public ApiResponse<PageResponse<PaymentRecordResponse>> listOwnPaymentHistory(
            @PathVariable Long semesterFeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paidAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Student payment history fetched successfully",
                PageResponse.from(studentFeeService.listOwnPaymentHistory(semesterFeeId, pageable))
        );
    }
}
