package com.example.sams.user.repository;

import com.example.sams.user.domain.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    boolean existsByStudentCode(String studentCode);

    @Query("""
            select s from Student s
            join s.user u
            where (:departmentId is null or s.department.id = :departmentId)
              and (:programId is null or s.program.id = :programId)
              and (:sectionId is null or s.section.id = :sectionId)
              and (
                    :query is null
                    or lower(s.studentCode) like lower(concat('%', :query, '%'))
                    or lower(u.username) like lower(concat('%', :query, '%'))
                    or lower(u.email) like lower(concat('%', :query, '%'))
              )
            """)
    Page<Student> search(
            @Param("departmentId") Long departmentId,
            @Param("programId") Long programId,
            @Param("sectionId") Long sectionId,
            @Param("query") String query,
            Pageable pageable
    );
}
