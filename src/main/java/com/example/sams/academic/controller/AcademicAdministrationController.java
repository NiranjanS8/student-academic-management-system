package com.example.sams.academic.controller;

import com.example.sams.academic.dto.DepartmentRequest;
import com.example.sams.academic.dto.DepartmentResponse;
import com.example.sams.academic.dto.ProgramRequest;
import com.example.sams.academic.dto.ProgramResponse;
import com.example.sams.academic.service.AcademicAdministrationService;
import com.example.sams.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/academic")
public class AcademicAdministrationController {

    private final AcademicAdministrationService academicAdministrationService;

    public AcademicAdministrationController(AcademicAdministrationService academicAdministrationService) {
        this.academicAdministrationService = academicAdministrationService;
    }

    @PostMapping("/departments")
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created successfully", academicAdministrationService.createDepartment(request));
    }

    @PutMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.success("Department updated successfully", academicAdministrationService.updateDepartment(departmentId, request));
    }

    @GetMapping("/departments/{departmentId}")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable Long departmentId) {
        return ApiResponse.success("Department fetched successfully", academicAdministrationService.getDepartmentById(departmentId));
    }

    @GetMapping("/departments")
    public ApiResponse<Page<DepartmentResponse>> listDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ApiResponse.success("Departments fetched successfully", academicAdministrationService.listDepartments(pageable));
    }

    @PostMapping("/programs")
    public ApiResponse<ProgramResponse> createProgram(@Valid @RequestBody ProgramRequest request) {
        return ApiResponse.success("Program created successfully", academicAdministrationService.createProgram(request));
    }

    @PutMapping("/programs/{programId}")
    public ApiResponse<ProgramResponse> updateProgram(
            @PathVariable Long programId,
            @Valid @RequestBody ProgramRequest request
    ) {
        return ApiResponse.success("Program updated successfully", academicAdministrationService.updateProgram(programId, request));
    }

    @GetMapping("/programs/{programId}")
    public ApiResponse<ProgramResponse> getProgram(@PathVariable Long programId) {
        return ApiResponse.success("Program fetched successfully", academicAdministrationService.getProgramById(programId));
    }

    @GetMapping("/programs")
    public ApiResponse<Page<ProgramResponse>> listPrograms(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Programs fetched successfully",
                academicAdministrationService.listPrograms(departmentId, pageable)
        );
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}
