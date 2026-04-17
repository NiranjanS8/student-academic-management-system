package com.example.sams.academic.repository;

import com.example.sams.academic.domain.Subject;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCodeIgnoreCase(String code);

    Page<Subject> findAllByDepartmentId(Long departmentId, Pageable pageable);
}
