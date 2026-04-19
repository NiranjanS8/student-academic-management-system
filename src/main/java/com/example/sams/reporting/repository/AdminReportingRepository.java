package com.example.sams.reporting.repository;

import com.example.sams.reporting.projection.AdminDashboardMetricsProjection;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminReportingRepository extends Repository<com.example.sams.user.domain.Student, Long> {

    @Query(value = """
            select
              (select count(*) from students) as totalStudents,
              (select count(*) from teachers) as totalTeachers,
              (select count(*) from course_offerings) as totalOfferings,
              (select count(*) from enrollments where status = 'ENROLLED') as activeEnrollments,
              (select count(*) from exams where is_published = true) as publishedResults,
              (select count(distinct student_id) from semester_fees where total_payable > paid_amount) as studentsWithOutstandingDues,
              (
                select count(*)
                from (
                  select e.id
                  from enrollments e
                  left join attendance_sessions ats on ats.course_offering_id = e.course_offering_id
                  left join attendance_records ar on ar.attendance_session_id = ats.id and ar.student_id = e.student_id
                  where e.status = 'ENROLLED'
                  group by e.id
                  having count(ar.id) > 0
                     and (sum(case when ar.status = 'PRESENT' then 1 else 0 end) * 100.0 / count(ar.id)) < :minimumPercentage
                ) shortage_rows
              ) as lowAttendanceCases
            """, nativeQuery = true)
    AdminDashboardMetricsProjection fetchDashboardMetrics(@Param("minimumPercentage") BigDecimal minimumPercentage);
}
