package com.example.sams.offering.repository;

import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
import com.example.sams.reporting.projection.TeacherWorkloadProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    boolean existsBySubjectIdAndTermIdAndSectionId(Long subjectId, Long termId, Long sectionId);

    boolean existsBySubjectIdAndTermIdAndSectionIdAndIdNot(Long subjectId, Long termId, Long sectionId, Long id);

    Page<CourseOffering> findAllByTermId(Long termId, Pageable pageable);

    Page<CourseOffering> findAllBySectionId(Long sectionId, Pageable pageable);

    Page<CourseOffering> findAllByTeacherId(Long teacherId, Pageable pageable);

    Page<CourseOffering> findAllBySubjectId(Long subjectId, Pageable pageable);

    Page<CourseOffering> findAllByStatus(CourseOfferingStatus status, Pageable pageable);

    Page<CourseOffering> findAllByTeacherUserId(Long userId, Pageable pageable);

    List<CourseOffering> findAllByTeacherId(Long teacherId);

    Optional<CourseOffering> findByIdAndTeacherUserId(Long offeringId, Long userId);

    @Query("""
            select co from CourseOffering co
            where (:termId is null or co.term.id = :termId)
              and (:sectionId is null or co.section.id = :sectionId)
              and (:teacherId is null or co.teacher.id = :teacherId)
              and (:subjectId is null or co.subject.id = :subjectId)
              and (:status is null or co.status = :status)
            """)
    Page<CourseOffering> search(
            @Param("termId") Long termId,
            @Param("sectionId") Long sectionId,
            @Param("teacherId") Long teacherId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseOfferingStatus status,
            Pageable pageable
    );

    @Query("""
            select co from CourseOffering co
            where co.teacher.user.id = :teacherUserId
              and (:termId is null or co.term.id = :termId)
              and (:sectionId is null or co.section.id = :sectionId)
              and (:subjectId is null or co.subject.id = :subjectId)
              and (:status is null or co.status = :status)
            """)
    Page<CourseOffering> searchAssignedToTeacher(
            @Param("teacherUserId") Long teacherUserId,
            @Param("termId") Long termId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseOfferingStatus status,
            Pageable pageable
    );

    @Query("""
            select co from CourseOffering co
            where co.section.id = :sectionId
              and co.term.id = :termId
              and (:subjectId is null or co.subject.id = :subjectId)
              and (:status is null or co.status = :status)
            """)
    Page<CourseOffering> searchVisibleToStudent(
            @Param("sectionId") Long sectionId,
            @Param("termId") Long termId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseOfferingStatus status,
            Pageable pageable
    );

    @Query("""
            select co from CourseOffering co
            where co.id = :offeringId
              and co.section.id = :sectionId
              and co.term.id = :termId
            """)
    Optional<CourseOffering> findVisibleToStudent(
            @Param("offeringId") Long offeringId,
            @Param("sectionId") Long sectionId,
            @Param("termId") Long termId
    );

    @Query(value = """
            select *
            from (
              select
                t.id as teacherId,
                t.employee_code as employeeCode,
                u.username as teacherUsername,
                u.email as teacherEmail,
                d.name as departmentName,
                t.designation as designation,
                coalesce(count(co.id), 0) as totalOfferings,
                coalesce(sum(case when co.status = 'OPEN' then 1 else 0 end), 0) as openOfferings,
                coalesce(sum(case when co.status = 'CLOSED' then 1 else 0 end), 0) as closedOfferings,
                coalesce(sum(case when co.status = 'ARCHIVED' then 1 else 0 end), 0) as archivedOfferings,
                coalesce(sum(coalesce(enr.enrolled_count, 0)), 0) as totalAssignedStudents,
                coalesce(sum(coalesce(exm.published_exam_count, 0)), 0) as publishedExamCount,
                case
                  when coalesce(sum(co.capacity), 0) = 0 then 0
                  else round(coalesce(sum(coalesce(enr.enrolled_count, 0)), 0) * 100.0 / sum(co.capacity), 2)
                end as averageCapacityUtilization
              from teachers t
              join users u on u.id = t.user_id
              join departments d on d.id = t.department_id
              left join course_offerings co
                on co.teacher_id = t.id
               and (:termId is null or co.term_id = :termId)
              left join (
                select course_offering_id, count(*) as enrolled_count
                from enrollments
                where status = 'ENROLLED'
                group by course_offering_id
              ) enr on enr.course_offering_id = co.id
              left join (
                select course_offering_id, count(*) as published_exam_count
                from exams
                where is_published = true
                group by course_offering_id
              ) exm on exm.course_offering_id = co.id
              where (:departmentId is null or t.department_id = :departmentId)
                and (
                      :query is null
                      or lower(t.employee_code) like lower(concat('%', :query, '%'))
                      or lower(coalesce(t.designation, '')) like lower(concat('%', :query, '%'))
                      or lower(u.username) like lower(concat('%', :query, '%'))
                      or lower(u.email) like lower(concat('%', :query, '%'))
                )
                and (:termId is null or exists (
                      select 1 from course_offerings co2
                      where co2.teacher_id = t.id and co2.term_id = :termId
                ))
              group by t.id, t.employee_code, u.username, u.email, d.name, t.designation
            ) workload_rows
            order by
              case when :sortBy = 'teacherUsername' and :direction = 'asc' then teacherUsername end asc,
              case when :sortBy = 'teacherUsername' and :direction = 'desc' then teacherUsername end desc,
              case when :sortBy = 'departmentName' and :direction = 'asc' then departmentName end asc,
              case when :sortBy = 'departmentName' and :direction = 'desc' then departmentName end desc,
              case when :sortBy = 'totalOfferings' and :direction = 'asc' then totalOfferings end asc,
              case when :sortBy = 'totalOfferings' and :direction = 'desc' then totalOfferings end desc,
              case when :sortBy = 'totalAssignedStudents' and :direction = 'asc' then totalAssignedStudents end asc,
              case when :sortBy = 'totalAssignedStudents' and :direction = 'desc' then totalAssignedStudents end desc,
              case when :sortBy = 'publishedExamCount' and :direction = 'asc' then publishedExamCount end asc,
              case when :sortBy = 'publishedExamCount' and :direction = 'desc' then publishedExamCount end desc,
              case when :sortBy = 'averageCapacityUtilization' and :direction = 'asc' then averageCapacityUtilization end asc,
              case when :sortBy = 'averageCapacityUtilization' and :direction = 'desc' then averageCapacityUtilization end desc,
              case when :sortBy = 'employeeCode' and :direction = 'desc' then employeeCode end desc,
              employeeCode asc
            """, countQuery = """
            select count(*)
            from teachers t
            join users u on u.id = t.user_id
            where (:departmentId is null or t.department_id = :departmentId)
              and (
                    :query is null
                    or lower(t.employee_code) like lower(concat('%', :query, '%'))
                    or lower(coalesce(t.designation, '')) like lower(concat('%', :query, '%'))
                    or lower(u.username) like lower(concat('%', :query, '%'))
                    or lower(u.email) like lower(concat('%', :query, '%'))
              )
              and (:termId is null or exists (
                    select 1 from course_offerings co2
                    where co2.teacher_id = t.id and co2.term_id = :termId
              ))
            """, nativeQuery = true)
    Page<TeacherWorkloadProjection> findTeacherWorkloadRows(
            @Param("departmentId") Long departmentId,
            @Param("termId") Long termId,
            @Param("query") String query,
            @Param("sortBy") String sortBy,
            @Param("direction") String direction,
            Pageable pageable
    );
}
