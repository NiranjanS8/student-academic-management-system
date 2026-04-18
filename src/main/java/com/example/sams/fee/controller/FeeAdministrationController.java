package com.example.sams.fee.controller;

import com.example.sams.common.api.ApiResponse;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.api.PaginationUtils;
import com.example.sams.fee.dto.FeeStructureRequest;
import com.example.sams.fee.dto.FeeStructureResponse;
import com.example.sams.fee.service.FeeStructureAdministrationService;
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
@RequestMapping("/api/v1/admin/fees")
public class FeeAdministrationController {

    private final FeeStructureAdministrationService feeStructureAdministrationService;

    public FeeAdministrationController(FeeStructureAdministrationService feeStructureAdministrationService) {
        this.feeStructureAdministrationService = feeStructureAdministrationService;
    }

    @PostMapping("/structures")
    public ApiResponse<FeeStructureResponse> createFeeStructure(@Valid @RequestBody FeeStructureRequest request) {
        return ApiResponse.success("Fee structure created successfully", feeStructureAdministrationService.createFeeStructure(request));
    }

    @PutMapping("/structures/{feeStructureId}")
    public ApiResponse<FeeStructureResponse> updateFeeStructure(
            @PathVariable Long feeStructureId,
            @Valid @RequestBody FeeStructureRequest request
    ) {
        return ApiResponse.success("Fee structure updated successfully", feeStructureAdministrationService.updateFeeStructure(feeStructureId, request));
    }

    @PostMapping("/structures/{feeStructureId}/deactivate")
    public ApiResponse<FeeStructureResponse> deactivateFeeStructure(@PathVariable Long feeStructureId) {
        return ApiResponse.success(
                "Fee structure deactivated successfully",
                feeStructureAdministrationService.deactivateFeeStructure(feeStructureId)
        );
    }

    @GetMapping("/structures/{feeStructureId}")
    public ApiResponse<FeeStructureResponse> getFeeStructure(@PathVariable Long feeStructureId) {
        return ApiResponse.success("Fee structure fetched successfully", feeStructureAdministrationService.getFeeStructureById(feeStructureId));
    }

    @GetMapping("/structures")
    public ApiResponse<PageResponse<FeeStructureResponse>> listFeeStructures(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PaginationUtils.buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Fee structures fetched successfully",
                PageResponse.from(feeStructureAdministrationService.listFeeStructures(programId, termId, active, pageable))
        );
    }
}
