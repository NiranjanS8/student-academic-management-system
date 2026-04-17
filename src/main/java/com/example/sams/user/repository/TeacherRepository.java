package com.example.sams.user.repository;

import com.example.sams.user.domain.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByEmployeeCode(String employeeCode);

    Page<Teacher> findAllByDepartmentId(Long departmentId, Pageable pageable);

    @Query("""
            select t from Teacher t
            join t.user u
            where (:departmentId is null or t.department.id = :departmentId)
              and (
                    :query is null
                    or lower(t.employeeCode) like lower(concat('%', :query, '%'))
                    or lower(coalesce(t.designation, '')) like lower(concat('%', :query, '%'))
                    or lower(u.username) like lower(concat('%', :query, '%'))
                    or lower(u.email) like lower(concat('%', :query, '%'))
              )
            """)
    Page<Teacher> search(
            @Param("departmentId") Long departmentId,
            @Param("query") String query,
            Pageable pageable
    );
}
