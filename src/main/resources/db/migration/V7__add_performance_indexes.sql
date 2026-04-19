CREATE INDEX idx_attendance_records_student_session_status
    ON attendance_records(student_id, attendance_session_id, status);

CREATE INDEX idx_attendance_sessions_offering_session_date
    ON attendance_sessions(course_offering_id, session_date);

CREATE INDEX idx_audit_logs_actor_user_created_at
    ON audit_logs(actor_user_id, created_at);
