package com.example.sams.reporting.projection;

import java.math.BigDecimal;

public interface AttendanceShortageReportProjection {

    Long getUserId();

    Long getStudentId();

    String getStudentCode();

    String getStudentUsername();

    Long getCourseOfferingId();

    String getSubjectCode();

    String getSubjectName();

    long getTotalSessions();

    long getPresentSessions();

    BigDecimal getAttendancePercentage();
}
