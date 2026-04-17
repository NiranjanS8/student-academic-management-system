package com.example.sams.academic.service;

import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.domain.Subject;
import com.example.sams.academic.domain.SubjectPrerequisite;
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
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.academic.repository.SubjectPrerequisiteRepository;
import com.example.sams.academic.repository.SubjectRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicAdministrationService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicTermRepository academicTermRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;

    public AcademicAdministrationService(
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            AcademicTermRepository academicTermRepository,
            SectionRepository sectionRepository,
            SubjectRepository subjectRepository,
            SubjectPrerequisiteRepository subjectPrerequisiteRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.academicTermRepository = academicTermRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.subjectPrerequisiteRepository = subjectPrerequisiteRepository;
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

    @Transactional
    public AcademicTermResponse createAcademicTerm(AcademicTermRequest request) {
        validateAcademicTerm(request.name(), request.academicYear(), request.startDate(), request.endDate(), null);

        AcademicTerm academicTerm = new AcademicTerm();
        academicTerm.setName(request.name().trim());
        academicTerm.setAcademicYear(request.academicYear().trim());
        academicTerm.setStartDate(request.startDate());
        academicTerm.setEndDate(request.endDate());
        academicTerm.setStatus(normalizeStatus(request.status()));
        academicTermRepository.save(academicTerm);

        return toAcademicTermResponse(academicTerm);
    }

    @Transactional
    public AcademicTermResponse updateAcademicTerm(Long academicTermId, AcademicTermRequest request) {
        AcademicTerm academicTerm = getAcademicTerm(academicTermId);
        validateAcademicTerm(request.name(), request.academicYear(), request.startDate(), request.endDate(), academicTermId);

        academicTerm.setName(request.name().trim());
        academicTerm.setAcademicYear(request.academicYear().trim());
        academicTerm.setStartDate(request.startDate());
        academicTerm.setEndDate(request.endDate());
        academicTerm.setStatus(normalizeStatus(request.status()));
        return toAcademicTermResponse(academicTerm);
    }

    @Transactional(readOnly = true)
    public AcademicTermResponse getAcademicTermById(Long academicTermId) {
        return toAcademicTermResponse(getAcademicTerm(academicTermId));
    }

    @Transactional(readOnly = true)
    public Page<AcademicTermResponse> listAcademicTerms(Pageable pageable) {
        return academicTermRepository.findAll(pageable).map(this::toAcademicTermResponse);
    }

    @Transactional
    public SectionResponse createSection(SectionRequest request) {
        Program program = getProgram(request.programId());
        AcademicTerm currentTerm = resolveAcademicTerm(request.currentTermId());
        validateSectionUniqueness(request.name(), program.getId(), null);

        Section section = new Section();
        section.setName(request.name().trim());
        section.setProgram(program);
        section.setCurrentTerm(currentTerm);
        sectionRepository.save(section);

        return toSectionResponse(section);
    }

    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionRequest request) {
        Section section = getSection(sectionId);
        Program program = getProgram(request.programId());
        AcademicTerm currentTerm = resolveAcademicTerm(request.currentTermId());
        validateSectionUniqueness(request.name(), program.getId(), sectionId);

        section.setName(request.name().trim());
        section.setProgram(program);
        section.setCurrentTerm(currentTerm);
        return toSectionResponse(section);
    }

    @Transactional(readOnly = true)
    public SectionResponse getSectionById(Long sectionId) {
        return toSectionResponse(getSection(sectionId));
    }

    @Transactional(readOnly = true)
    public Page<SectionResponse> listSections(Long programId, Pageable pageable) {
        Page<Section> page = programId == null
                ? sectionRepository.findAll(pageable)
                : sectionRepository.findAllByProgramId(programId, pageable);
        return page.map(this::toSectionResponse);
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        validateSubjectCodeUniqueness(request.code(), null);
        Department department = getDepartment(request.departmentId());

        Subject subject = new Subject();
        subject.setCode(normalizeCode(request.code()));
        subject.setName(request.name().trim());
        subject.setCredits(request.credits());
        subject.setDepartment(department);
        subject.setActive(request.active());
        subjectRepository.save(subject);

        return toSubjectResponse(subject);
    }

    @Transactional
    public SubjectResponse updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = getSubject(subjectId);
        validateSubjectCodeUniqueness(request.code(), subjectId);
        Department department = getDepartment(request.departmentId());

        subject.setCode(normalizeCode(request.code()));
        subject.setName(request.name().trim());
        subject.setCredits(request.credits());
        subject.setDepartment(department);
        subject.setActive(request.active());
        return toSubjectResponse(subject);
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long subjectId) {
        return toSubjectResponse(getSubject(subjectId));
    }

    @Transactional(readOnly = true)
    public Page<SubjectResponse> listSubjects(Long departmentId, Pageable pageable) {
        Page<Subject> page = departmentId == null
                ? subjectRepository.findAll(pageable)
                : subjectRepository.findAllByDepartmentId(departmentId, pageable);
        return page.map(this::toSubjectResponse);
    }

    @Transactional
    public SubjectResponse addPrerequisite(Long subjectId, SubjectPrerequisiteRequest request) {
        Subject subject = getSubject(subjectId);
        Subject prerequisiteSubject = getSubject(request.prerequisiteSubjectId());

        if (subject.getId().equals(prerequisiteSubject.getId())) {
            throw new ConflictException("Subject cannot be its own prerequisite");
        }

        if (subjectPrerequisiteRepository.existsBySubjectIdAndPrerequisiteSubjectId(subjectId, request.prerequisiteSubjectId())) {
            throw new ConflictException("Prerequisite already exists for this subject");
        }

        SubjectPrerequisite subjectPrerequisite = new SubjectPrerequisite();
        subjectPrerequisite.setSubject(subject);
        subjectPrerequisite.setPrerequisiteSubject(prerequisiteSubject);
        subjectPrerequisiteRepository.save(subjectPrerequisite);

        return toSubjectResponse(subject);
    }

    @Transactional
    public SubjectResponse removePrerequisite(Long subjectId, Long prerequisiteSubjectId) {
        Subject subject = getSubject(subjectId);

        if (!subjectPrerequisiteRepository.existsBySubjectIdAndPrerequisiteSubjectId(subjectId, prerequisiteSubjectId)) {
            throw new ResourceNotFoundException("Prerequisite mapping not found");
        }

        subjectPrerequisiteRepository.deleteBySubjectIdAndPrerequisiteSubjectId(subjectId, prerequisiteSubjectId);
        return toSubjectResponse(subject);
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private Program getProgram(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    private AcademicTerm getAcademicTerm(Long academicTermId) {
        return academicTermRepository.findById(academicTermId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }

    private Section getSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    private Subject getSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    private AcademicTerm resolveAcademicTerm(Long academicTermId) {
        return academicTermId == null ? null : getAcademicTerm(academicTermId);
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

    private void validateAcademicTerm(
            String name,
            String academicYear,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Long currentAcademicTermId
    ) {
        if (endDate.isBefore(startDate)) {
            throw new ConflictException("Academic term endDate cannot be before startDate");
        }

        academicTermRepository.findByNameIgnoreCaseAndAcademicYear(name.trim(), academicYear.trim())
                .filter(existing -> !existing.getId().equals(currentAcademicTermId))
                .ifPresent(existing -> {
                    throw new ConflictException("Academic term with name and academicYear already exists");
                });
    }

    private void validateSectionUniqueness(String name, Long programId, Long currentSectionId) {
        sectionRepository.findAll().stream()
                .filter(existing -> existing.getProgram().getId().equals(programId))
                .filter(existing -> existing.getName().equalsIgnoreCase(name.trim()))
                .filter(existing -> !existing.getId().equals(currentSectionId))
                .findAny()
                .ifPresent(existing -> {
                    throw new ConflictException("Section with name already exists for this program");
                });
    }

    private void validateSubjectCodeUniqueness(String code, Long currentSubjectId) {
        subjectRepository.findByCodeIgnoreCase(code.trim())
                .filter(existing -> !existing.getId().equals(currentSubjectId))
                .ifPresent(existing -> {
                    throw new ConflictException("Subject with code already exists");
                });
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeStatus(String status) {
        return status.trim().toUpperCase();
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

    private AcademicTermResponse toAcademicTermResponse(AcademicTerm academicTerm) {
        return new AcademicTermResponse(
                academicTerm.getId(),
                academicTerm.getName(),
                academicTerm.getAcademicYear(),
                academicTerm.getStartDate(),
                academicTerm.getEndDate(),
                academicTerm.getStatus(),
                academicTerm.getCreatedAt(),
                academicTerm.getUpdatedAt()
        );
    }

    private SectionResponse toSectionResponse(Section section) {
        Program program = section.getProgram();
        AcademicTerm currentTerm = section.getCurrentTerm();

        return new SectionResponse(
                section.getId(),
                section.getName(),
                new SectionResponse.ProgramSummary(
                        program.getId(),
                        program.getCode(),
                        program.getName()
                ),
                currentTerm == null ? null : new SectionResponse.AcademicTermSummary(
                        currentTerm.getId(),
                        currentTerm.getName(),
                        currentTerm.getAcademicYear(),
                        currentTerm.getStatus()
                ),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }

    private SubjectResponse toSubjectResponse(Subject subject) {
        Department department = subject.getDepartment();
        List<SubjectResponse.PrerequisiteSummary> prerequisites = subjectPrerequisiteRepository.findAllBySubjectId(subject.getId())
                .stream()
                .map(SubjectPrerequisite::getPrerequisiteSubject)
                .map(prerequisite -> new SubjectResponse.PrerequisiteSummary(
                        prerequisite.getId(),
                        prerequisite.getCode(),
                        prerequisite.getName()
                ))
                .toList();

        return new SubjectResponse(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getCredits(),
                subject.isActive(),
                new SubjectResponse.DepartmentSummary(
                        department.getId(),
                        department.getCode(),
                        department.getName()
                ),
                prerequisites,
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }
}
