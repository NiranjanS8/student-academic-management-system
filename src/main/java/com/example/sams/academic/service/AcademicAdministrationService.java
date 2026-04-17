package com.example.sams.academic.service;

import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.dto.DepartmentRequest;
import com.example.sams.academic.dto.DepartmentResponse;
import com.example.sams.academic.dto.ProgramRequest;
import com.example.sams.academic.dto.ProgramResponse;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicAdministrationService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;

    public AcademicAdministrationService(DepartmentRepository departmentRepository, ProgramRepository programRepository) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        validateDepartmentUniqueness(request.code(), request.name(), null);

        Department department = new Department();
        department.setCode(normalizeCode(request.code()));
        department.setName(request.name().trim());
        departmentRepository.save(department);

        return toDepartmentResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, DepartmentRequest request) {
        Department department = getDepartment(departmentId);
        validateDepartmentUniqueness(request.code(), request.name(), departmentId);

        department.setCode(normalizeCode(request.code()));
        department.setName(request.name().trim());
        return toDepartmentResponse(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long departmentId) {
        return toDepartmentResponse(getDepartment(departmentId));
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> listDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(this::toDepartmentResponse);
    }

    @Transactional
    public ProgramResponse createProgram(ProgramRequest request) {
        validateProgramCodeUniqueness(request.code(), null);
        Department department = getDepartment(request.departmentId());

        Program program = new Program();
        program.setCode(normalizeCode(request.code()));
        program.setName(request.name().trim());
        program.setDepartment(department);
        programRepository.save(program);

        return toProgramResponse(program);
    }

    @Transactional
    public ProgramResponse updateProgram(Long programId, ProgramRequest request) {
        Program program = getProgram(programId);
        validateProgramCodeUniqueness(request.code(), programId);
        Department department = getDepartment(request.departmentId());

        program.setCode(normalizeCode(request.code()));
        program.setName(request.name().trim());
        program.setDepartment(department);
        return toProgramResponse(program);
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(Long programId) {
        return toProgramResponse(getProgram(programId));
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> listPrograms(Long departmentId, Pageable pageable) {
        Page<Program> page = departmentId == null
                ? programRepository.findAll(pageable)
                : programRepository.findAllByDepartmentId(departmentId, pageable);
        return page.map(this::toProgramResponse);
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private Program getProgram(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    private void validateDepartmentUniqueness(String code, String name, Long currentDepartmentId) {
        departmentRepository.findByCodeIgnoreCase(code.trim())
                .filter(existing -> !existing.getId().equals(currentDepartmentId))
                .ifPresent(existing -> {
                    throw new ConflictException("Department with code already exists");
                });

        departmentRepository.findAll().stream()
                .filter(existing -> existing.getName().equalsIgnoreCase(name.trim()))
                .filter(existing -> !existing.getId().equals(currentDepartmentId))
                .findAny()
                .ifPresent(existing -> {
                    throw new ConflictException("Department with name already exists");
                });
    }

    private void validateProgramCodeUniqueness(String code, Long currentProgramId) {
        programRepository.findAll().stream()
                .filter(existing -> existing.getCode().equalsIgnoreCase(code.trim()))
                .filter(existing -> !existing.getId().equals(currentProgramId))
                .findAny()
                .ifPresent(existing -> {
                    throw new ConflictException("Program with code already exists");
                });
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private DepartmentResponse toDepartmentResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }

    private ProgramResponse toProgramResponse(Program program) {
        Department department = program.getDepartment();
        return new ProgramResponse(
                program.getId(),
                program.getCode(),
                program.getName(),
                new ProgramResponse.DepartmentSummary(
                        department.getId(),
                        department.getCode(),
                        department.getName()
                ),
                program.getCreatedAt(),
                program.getUpdatedAt()
        );
    }
}
