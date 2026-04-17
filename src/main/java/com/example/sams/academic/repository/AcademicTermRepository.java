package com.example.sams.academic.repository;

import com.example.sams.academic.domain.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
}
