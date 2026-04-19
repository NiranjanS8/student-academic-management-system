CREATE TABLE attendance_sessions (
    id BIGSERIAL PRIMARY KEY,
    course_offering_id BIGINT NOT NULL REFERENCES course_offerings(id),
    session_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attendance_session_offering_date UNIQUE (course_offering_id, session_date)
);

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    attendance_session_id BIGINT NOT NULL REFERENCES attendance_sessions(id),
    student_id BIGINT NOT NULL REFERENCES students(id),
    status VARCHAR(30) NOT NULL,
    marked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attendance_record_session_student UNIQUE (attendance_session_id, student_id)
);

CREATE INDEX idx_attendance_sessions_offering_id ON attendance_sessions(course_offering_id);
CREATE INDEX idx_attendance_records_session_id ON attendance_records(attendance_session_id);
CREATE INDEX idx_attendance_records_student_id ON attendance_records(student_id);
