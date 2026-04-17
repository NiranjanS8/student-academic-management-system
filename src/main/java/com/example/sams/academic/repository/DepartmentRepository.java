package com.example.sams.academic.repository;

import com.example.sams.academic.domain.Department;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    Optional<Department> findByCodeIgnoreCase(String code);
}
