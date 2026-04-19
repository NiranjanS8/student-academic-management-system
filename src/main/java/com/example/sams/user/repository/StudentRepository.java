package com.example.sams.user.repository;

import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.reporting.projection.CountBreakdownProjection;
import com.example.sams.reporting.projection.StudentDistributionProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    boolean existsByStudentCode(String studentCode);

    long countByAcademicStatus(AcademicStatus academicStatus);

    long countBySectionIsNull();

    long countByCurrentTermIsNull();

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

    @Query(value = """
            select *
            from (
              select
                d.id as departmentId,
                d.name as departmentName,
                p.id as programId,
                p.name as programName,
                t.id as termId,
                coalesce(t.name, 'Unassigned') as termName,
                t.academic_year as academicYear,
                sec.id as sectionId,
                coalesce(sec.name, 'Unassigned') as sectionName,
                s.academic_status as academicStatus,
                count(*) as studentCount
              from students s
              join departments d on d.id = s.department_id
              join programs p on p.id = s.program_id
              left join academic_terms t on t.id = s.current_term_id
              left join sections sec on sec.id = s.section_id
              where (:departmentId is null or s.department_id = :departmentId)
                and (:programId is null or s.program_id = :programId)
                and (:termId is null or s.current_term_id = :termId)
                and (:sectionId is null or s.section_id = :sectionId)
                and (:academicStatus is null or s.academic_status = :academicStatus)
              group by d.id, d.name, p.id, p.name, t.id, t.name, t.academic_year, sec.id, sec.name, s.academic_status
            ) distribution_rows
            order by
              case when :sortBy = 'departmentName' and :direction = 'asc' then departmentName end asc,
              case when :sortBy = 'departmentName' and :direction = 'desc' then departmentName end desc,
              case when :sortBy = 'programName' and :direction = 'asc' then programName end asc,
              case when :sortBy = 'programName' and :direction = 'desc' then programName end desc,
              case when :sortBy = 'termName' and :direction = 'asc' then termName end asc,
              case when :sortBy = 'termName' and :direction = 'desc' then termName end desc,
              case when :sortBy = 'sectionName' and :direction = 'asc' then sectionName end asc,
              case when :sortBy = 'sectionName' and :direction = 'desc' then sectionName end desc,
              case when :sortBy = 'academicStatus' and :direction = 'asc' then academicStatus end asc,
              case when :sortBy = 'academicStatus' and :direction = 'desc' then academicStatus end desc,
              case when :sortBy = 'studentCount' and :direction = 'asc' then studentCount end asc,
              case when :sortBy = 'studentCount' and :direction = 'desc' then studentCount end desc,
              programName asc,
              sectionName asc
            """, countQuery = """
            select count(*)
            from (
              select 1
              from students s
              where (:departmentId is null or s.department_id = :departmentId)
                and (:programId is null or s.program_id = :programId)
                and (:termId is null or s.current_term_id = :termId)
                and (:sectionId is null or s.section_id = :sectionId)
                and (:academicStatus is null or s.academic_status = :academicStatus)
              group by s.department_id, s.program_id, s.current_term_id, s.section_id, s.academic_status
            ) distribution_count_rows
            """, nativeQuery = true)
    Page<StudentDistributionProjection> findStudentDistributionRows(
            @Param("departmentId") Long departmentId,
            @Param("programId") Long programId,
            @Param("termId") Long termId,
            @Param("sectionId") Long sectionId,
            @Param("academicStatus") String academicStatus,
            @Param("sortBy") String sortBy,
            @Param("direction") String direction,
            Pageable pageable
    );

    @Query(value = """
            select d.id as id, d.name as name, count(*) as studentCount
            from students s
            join departments d on d.id = s.department_id
            group by d.id, d.name
            """, nativeQuery = true)
    List<CountBreakdownProjection> findDepartmentBreakdown();

    @Query(value = """
            select p.id as id, p.name as name, count(*) as studentCount
            from students s
            join programs p on p.id = s.program_id
            group by p.id, p.name
            """, nativeQuery = true)
    List<CountBreakdownProjection> findProgramBreakdown();

    @Query(value = """
            select t.id as id, concat(t.name, ' (', t.academic_year, ')') as name, count(*) as studentCount
            from students s
            join academic_terms t on t.id = s.current_term_id
            group by t.id, t.name, t.academic_year
            """, nativeQuery = true)
    List<CountBreakdownProjection> findTermBreakdown();
}
