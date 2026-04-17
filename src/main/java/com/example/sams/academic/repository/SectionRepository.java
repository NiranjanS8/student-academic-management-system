package com.example.sams.academic.repository;

import com.example.sams.academic.domain.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

    Page<Section> findAllByProgramId(Long programId, Pageable pageable);
}
