package com.example.sams.reporting.projection;

import java.math.BigDecimal;

public interface TeacherWorkloadProjection {

    Long getTeacherId();

    String getEmployeeCode();

    String getTeacherUsername();

    String getTeacherEmail();

    String getDepartmentName();

    String getDesignation();

    long getTotalOfferings();

    long getOpenOfferings();

    long getClosedOfferings();

    long getArchivedOfferings();

    long getTotalAssignedStudents();

    long getPublishedExamCount();

    BigDecimal getAverageCapacityUtilization();
}
