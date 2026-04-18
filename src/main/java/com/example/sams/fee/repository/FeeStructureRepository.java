package com.example.sams.fee.repository;

import com.example.sams.fee.domain.FeeCategory;
import com.example.sams.fee.domain.FeeStructure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    Page<FeeStructure> findAllByProgramId(Long programId, Pageable pageable);

    Page<FeeStructure> findAllByTermId(Long termId, Pageable pageable);

    Page<FeeStructure> findAllByActive(boolean active, Pageable pageable);

    Optional<FeeStructure> findByProgramIdAndTermIdAndFeeCategoryAndActiveTrue(
            Long programId,
            Long termId,
            FeeCategory feeCategory
    );

    List<FeeStructure> findAllByProgramIdAndTermIdAndActiveTrue(Long programId, Long termId);
}
