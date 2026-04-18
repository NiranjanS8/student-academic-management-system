package com.example.sams.fee.service;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.fee.domain.FeeCategory;
import com.example.sams.fee.domain.FeeStructure;
import com.example.sams.fee.dto.FeeStructureRequest;
import com.example.sams.fee.dto.FeeStructureResponse;
import com.example.sams.fee.repository.FeeStructureRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeStructureAdministrationService {

    private final FeeStructureRepository feeStructureRepository;
    private final ProgramRepository programRepository;
    private final AcademicTermRepository academicTermRepository;

    public FeeStructureAdministrationService(
            FeeStructureRepository feeStructureRepository,
            ProgramRepository programRepository,
            AcademicTermRepository academicTermRepository
    ) {
        this.feeStructureRepository = feeStructureRepository;
        this.programRepository = programRepository;
        this.academicTermRepository = academicTermRepository;
    }

    @Transactional
    public FeeStructureResponse createFeeStructure(FeeStructureRequest request) {
        Program program = getProgram(request.programId());
        AcademicTerm term = getTerm(request.termId());
        FeeCategory feeCategory = parseFeeCategory(request.feeCategory());
        validateActiveDuplicate(program.getId(), term.getId(), feeCategory, request.active(), null);

        FeeStructure feeStructure = new FeeStructure();
        applyRequest(feeStructure, request, program, term, feeCategory);
        feeStructureRepository.save(feeStructure);
        return toResponse(feeStructure);
    }

    @Transactional
    public FeeStructureResponse updateFeeStructure(Long feeStructureId, FeeStructureRequest request) {
        FeeStructure feeStructure = getFeeStructure(feeStructureId);
        Program program = getProgram(request.programId());
        AcademicTerm term = getTerm(request.termId());
        FeeCategory feeCategory = parseFeeCategory(request.feeCategory());
        validateActiveDuplicate(program.getId(), term.getId(), feeCategory, request.active(), feeStructureId);

        applyRequest(feeStructure, request, program, term, feeCategory);
        return toResponse(feeStructure);
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse getFeeStructureById(Long feeStructureId) {
        return toResponse(getFeeStructure(feeStructureId));
    }

    @Transactional(readOnly = true)
    public Page<FeeStructureResponse> listFeeStructures(Long programId, Long termId, Boolean active, Pageable pageable) {
        Page<FeeStructure> page;
        if (programId != null) {
            page = feeStructureRepository.findAllByProgramId(programId, pageable);
        } else if (termId != null) {
            page = feeStructureRepository.findAllByTermId(termId, pageable);
        } else if (active != null) {
            page = feeStructureRepository.findAllByActive(active, pageable);
        } else {
            page = feeStructureRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional
    public FeeStructureResponse deactivateFeeStructure(Long feeStructureId) {
        FeeStructure feeStructure = getFeeStructure(feeStructureId);
        feeStructure.setActive(false);
        return toResponse(feeStructure);
    }

    private void applyRequest(
            FeeStructure feeStructure,
            FeeStructureRequest request,
            Program program,
            AcademicTerm term,
            FeeCategory feeCategory
    ) {
        feeStructure.setProgram(program);
        feeStructure.setTerm(term);
        feeStructure.setName(request.name().trim());
        feeStructure.setFeeCategory(feeCategory);
        feeStructure.setAmount(request.amount());
        feeStructure.setDueDaysFromTermStart(request.dueDaysFromTermStart());
        feeStructure.setDescription(normalize(request.description()));
        feeStructure.setActive(request.active());
    }

    private void validateActiveDuplicate(
            Long programId,
            Long termId,
            FeeCategory feeCategory,
            Boolean active,
            Long currentFeeStructureId
    ) {
        if (!Boolean.TRUE.equals(active)) {
            return;
        }
        feeStructureRepository.findByProgramIdAndTermIdAndFeeCategoryAndActiveTrue(programId, termId, feeCategory)
                .filter(existing -> !existing.getId().equals(currentFeeStructureId))
                .ifPresent(existing -> {
                    throw new ConflictException("An active fee structure already exists for this program, term, and category");
                });
    }

    private FeeStructure getFeeStructure(Long feeStructureId) {
        return feeStructureRepository.findById(feeStructureId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));
    }

    private Program getProgram(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    private AcademicTerm getTerm(Long termId) {
        return academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }

    private FeeCategory parseFeeCategory(String rawCategory) {
        try {
            return FeeCategory.valueOf(rawCategory.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid feeCategory. Allowed values: TUITION, EXAM, LIBRARY, HOSTEL, TRANSPORT, MISCELLANEOUS");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private FeeStructureResponse toResponse(FeeStructure feeStructure) {
        Program program = feeStructure.getProgram();
        AcademicTerm term = feeStructure.getTerm();

        return new FeeStructureResponse(
                feeStructure.getId(),
                feeStructure.getName(),
                feeStructure.getFeeCategory().name(),
                feeStructure.getAmount(),
                feeStructure.getDueDaysFromTermStart(),
                feeStructure.getDescription(),
                feeStructure.isActive(),
                new FeeStructureResponse.ProgramSummary(
                        program.getId(),
                        program.getCode(),
                        program.getName()
                ),
                new FeeStructureResponse.TermSummary(
                        term.getId(),
                        term.getName(),
                        term.getAcademicYear(),
                        term.getStatus()
                ),
                feeStructure.getCreatedAt(),
                feeStructure.getUpdatedAt()
        );
    }
}
