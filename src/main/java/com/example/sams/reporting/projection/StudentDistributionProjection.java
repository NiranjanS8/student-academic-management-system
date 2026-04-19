package com.example.sams.reporting.projection;

public interface StudentDistributionProjection {

    Long getDepartmentId();

    String getDepartmentName();

    Long getProgramId();

    String getProgramName();

    Long getTermId();

    String getTermName();

    String getAcademicYear();

    Long getSectionId();

    String getSectionName();

    String getAcademicStatus();

    long getStudentCount();
}
