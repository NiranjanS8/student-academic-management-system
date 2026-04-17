package com.example.sams.user.repository;

import com.example.sams.user.domain.Teacher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByEmployeeCode(String employeeCode);
}
