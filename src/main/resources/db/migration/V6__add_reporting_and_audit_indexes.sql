CREATE INDEX idx_audit_logs_action_actor_created_at
    ON audit_logs(action_type, actor_user_id, created_at);

CREATE INDEX idx_audit_logs_entity_type_entity_id_created_at
    ON audit_logs(entity_type, entity_id, created_at);

CREATE INDEX idx_students_department_program_term_section_status
    ON students(department_id, program_id, current_term_id, section_id, academic_status);

CREATE INDEX idx_enrollments_status_offering_student
    ON enrollments(status, course_offering_id, student_id);

CREATE INDEX idx_course_offerings_teacher_term_status
    ON course_offerings(teacher_id, term_id, status);

CREATE INDEX idx_exams_offering_published
    ON exams(course_offering_id, is_published);

CREATE INDEX idx_semester_fees_term_status_due_date
    ON semester_fees(term_id, status, due_date);
