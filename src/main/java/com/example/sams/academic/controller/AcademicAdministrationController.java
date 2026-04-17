package com.example.sams.academic.controller;

import com.example.sams.academic.dto.AcademicTermRequest;
import com.example.sams.academic.dto.AcademicTermResponse;
import com.example.sams.academic.dto.DepartmentRequest;
import com.example.sams.academic.dto.DepartmentResponse;
import com.example.sams.academic.dto.ProgramRequest;
import com.example.sams.academic.dto.ProgramResponse;
import com.example.sams.academic.dto.SectionRequest;
import com.example.sams.academic.dto.SectionResponse;
import com.example.sams.academic.dto.SubjectPrerequisiteRequest;
import com.example.sams.academic.dto.SubjectRequest;
import com.example.sams.academic.dto.SubjectResponse;
import com.example.sams.academic.service.AcademicAdministrationService;
import com.example.sams.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @PostMapping("/terms")
    public ApiResponse<AcademicTermResponse> createAcademicTerm(@Valid @RequestBody AcademicTermRequest request) {
        return ApiResponse.success("Academic term created successfully", academicAdministrationService.createAcademicTerm(request));
    }

    @PutMapping("/terms/{termId}")
    public ApiResponse<AcademicTermResponse> updateAcademicTerm(
            @PathVariable Long termId,
            @Valid @RequestBody AcademicTermRequest request
    ) {
        return ApiResponse.success("Academic term updated successfully", academicAdministrationService.updateAcademicTerm(termId, request));
    }

    @GetMapping("/terms/{termId}")
    public ApiResponse<AcademicTermResponse> getAcademicTerm(@PathVariable Long termId) {
        return ApiResponse.success("Academic term fetched successfully", academicAdministrationService.getAcademicTermById(termId));
    }

    @GetMapping("/terms")
    public ApiResponse<Page<AcademicTermResponse>> listAcademicTerms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ApiResponse.success("Academic terms fetched successfully", academicAdministrationService.listAcademicTerms(pageable));
    }

    @PostMapping("/sections")
    public ApiResponse<SectionResponse> createSection(@Valid @RequestBody SectionRequest request) {
        return ApiResponse.success("Section created successfully", academicAdministrationService.createSection(request));
    }

    @PutMapping("/sections/{sectionId}")
    public ApiResponse<SectionResponse> updateSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionRequest request
    ) {
        return ApiResponse.success("Section updated successfully", academicAdministrationService.updateSection(sectionId, request));
    }

    @GetMapping("/sections/{sectionId}")
    public ApiResponse<SectionResponse> getSection(@PathVariable Long sectionId) {
        return ApiResponse.success("Section fetched successfully", academicAdministrationService.getSectionById(sectionId));
    }

    @GetMapping("/sections")
    public ApiResponse<Page<SectionResponse>> listSections(
            @RequestParam(required = false) Long programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Sections fetched successfully",
                academicAdministrationService.listSections(programId, pageable)
        );
    }

    @PostMapping("/subjects")
    public ApiResponse<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success("Subject created successfully", academicAdministrationService.createSubject(request));
    }

    @PutMapping("/subjects/{subjectId}")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable Long subjectId,
            @Valid @RequestBody SubjectRequest request
    ) {
        return ApiResponse.success("Subject updated successfully", academicAdministrationService.updateSubject(subjectId, request));
    }

    @GetMapping("/subjects/{subjectId}")
    public ApiResponse<SubjectResponse> getSubject(@PathVariable Long subjectId) {
        return ApiResponse.success("Subject fetched successfully", academicAdministrationService.getSubjectById(subjectId));
    }

    @GetMapping("/subjects")
    public ApiResponse<Page<SubjectResponse>> listSubjects(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ApiResponse.success(
                "Subjects fetched successfully",
                academicAdministrationService.listSubjects(departmentId, pageable)
        );
    }

    @PostMapping("/subjects/{subjectId}/prerequisites")
    public ApiResponse<SubjectResponse> addPrerequisite(
            @PathVariable Long subjectId,
            @Valid @RequestBody SubjectPrerequisiteRequest request
    ) {
        return ApiResponse.success(
                "Subject prerequisite added successfully",
                academicAdministrationService.addPrerequisite(subjectId, request)
        );
    }

    @DeleteMapping("/subjects/{subjectId}/prerequisites/{prerequisiteSubjectId}")
    public ApiResponse<SubjectResponse> removePrerequisite(
            @PathVariable Long subjectId,
            @PathVariable Long prerequisiteSubjectId
    ) {
        return ApiResponse.success(
                "Subject prerequisite removed successfully",
                academicAdministrationService.removePrerequisite(subjectId, prerequisiteSubjectId)
        );
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}
