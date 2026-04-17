package com.example.sams.academic.repository;

import com.example.sams.academic.domain.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Page<Program> findAllByDepartmentId(Long departmentId, Pageable pageable);
}
