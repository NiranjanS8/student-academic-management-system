package com.example.sams.reporting.projection;

public interface AdminDashboardMetricsProjection {

    long getTotalStudents();

    long getTotalTeachers();

    long getTotalOfferings();

    long getActiveEnrollments();

    long getPublishedResults();

    long getStudentsWithOutstandingDues();

    long getLowAttendanceCases();
}
