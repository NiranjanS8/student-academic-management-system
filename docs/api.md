# API Documentation

## Overview
- Project: `student-academic-management-system`
- Base path: `/api/v1`
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- Health endpoints:
  - `/api/v1/health`
  - `/actuator/health`

This document is a compact reference for the implemented API surface in the repository. For full request and response schemas, use Swagger/OpenAPI at runtime.

## Authentication
- Auth style: JWT bearer token
- Login endpoint: `POST /api/v1/auth/login`
- Refresh endpoint: `POST /api/v1/auth/refresh`
- Send access token as:

```http
Authorization: Bearer <access-token>
```

## Authorization Rules
- Public:
  - `/api/v1/health`
  - `/api/v1/auth/**`
  - `/actuator/health`
  - `/v3/api-docs/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
- `ADMIN`:
  - `/api/v1/admin/academic/**`
  - `/api/v1/admin/audit/**`
  - `/api/v1/admin/fees/**`
  - `/api/v1/admin/offerings/**`
  - `/api/v1/admin/reports/**`
  - `POST /api/v1/admin/users/**`
- `TEACHER`:
  - `/api/v1/teacher/attendance/**`
  - `/api/v1/teacher/offerings/**`
  - `/api/v1/teacher/exams/**`
  - `/api/v1/teacher/notifications/**`
- `STUDENT`:
  - `/api/v1/student/offerings/**`
  - `/api/v1/student/fees/**`
  - `/api/v1/student/enrollments/**`
  - `/api/v1/student/results/**`
- Any authenticated user:
  - `/api/v1/notifications/**`
  - `GET /api/v1/users/me`

## Response Conventions
- Standard response envelope:

```json
{
  "success": true,
  "message": "Operation completed",
  "data": {},
  "timestamp": "2026-04-20T10:00:00Z"
}
```

- Paginated endpoints wrap `data` in:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "empty": true
}
```

## Common Query Parameters
- Pagination:
  - `page`
  - `size`
- Sorting:
  - `sortBy`
  - `direction`
- Common filters appear per module:
  - `query`
  - `termId`
  - `programId`
  - `sectionId`
  - `status`
  - `teacherId`
  - `subjectId`

## Endpoint Map

### Auth
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

### Health
- `GET /api/v1/health`

### User Profile
- `GET /api/v1/users/me`

### Admin User Management
Base: `/api/v1/admin/users`

- Teachers
  - `POST /teachers`
  - `GET /teachers/{teacherId}`
  - `GET /teachers`
  - `PUT /teachers/{teacherId}`
- Students
  - `POST /students`
  - `GET /students/{studentId}`
  - `GET /students`
  - `PUT /students/{studentId}`

### Admin Academic Management
Base: `/api/v1/admin/academic`

- Departments
  - `POST /departments`
  - `PUT /departments/{departmentId}`
  - `GET /departments/{departmentId}`
  - `GET /departments`
- Programs
  - `POST /programs`
  - `PUT /programs/{programId}`
  - `GET /programs/{programId}`
  - `GET /programs`
- Academic terms
  - `POST /terms`
  - `PUT /terms/{termId}`
  - `GET /terms/{termId}`
  - `GET /terms`
- Sections
  - `POST /sections`
  - `PUT /sections/{sectionId}`
  - `GET /sections/{sectionId}`
  - `GET /sections`
- Subjects
  - `POST /subjects`
  - `PUT /subjects/{subjectId}`
  - `GET /subjects/{subjectId}`
  - `GET /subjects`
- Subject prerequisites
  - `POST /subjects/{subjectId}/prerequisites`
  - `DELETE /subjects/{subjectId}/prerequisites/{prerequisiteSubjectId}`

### Admin Course Offering Management
Base: `/api/v1/admin/offerings`

- `POST /`
- `PUT /{offeringId}`
- `GET /{offeringId}`
- `GET /`

### Student Course Offering Access
Base: `/api/v1/student/offerings`

- `GET /`
- `GET /{offeringId}`

### Teacher Course Offering Access
Base: `/api/v1/teacher/offerings`

- `GET /`
- `GET /{offeringId}`

### Student Enrollment
Base: `/api/v1/student/enrollments`

- `POST /`
- `POST /{enrollmentId}/drop`
- `GET /`
- `GET /history`

### Teacher Attendance
Base: `/api/v1/teacher/attendance`

- Sessions
  - `POST /sessions`
  - `PUT /sessions/{sessionId}`
  - `GET /sessions`
  - `GET /sessions/{sessionId}`
- Enrollment roster for marking attendance
  - `GET /offerings/{offeringId}/students`

### Teacher Exams, Marks, and Publishing
Base: `/api/v1/teacher/exams`

- Exams
  - `POST /`
  - `GET /`
  - `GET /{examId}`
- Marks
  - `POST /{examId}/marks`
  - `PUT /{examId}/marks/{markEntryId}`
  - `GET /{examId}/marks`
- Result publishing
  - `POST /{examId}/publish`

### Student Results
Base: `/api/v1/student/results`

- `GET /`
- `GET /summary`

### Fee Administration
Base: `/api/v1/admin/fees`

- Fee structures
  - `POST /structures`
  - `PUT /structures/{feeStructureId}`
  - `POST /structures/{feeStructureId}/deactivate`
  - `GET /structures/{feeStructureId}`
  - `GET /structures`
- Semester fees
  - `POST /semester-fees/generate`
  - `GET /semester-fees/{semesterFeeId}`
  - `GET /semester-fees`
- Payments
  - `POST /semester-fees/{semesterFeeId}/payments`
  - `GET /semester-fees/{semesterFeeId}/payments`

### Student Fee Access
Base: `/api/v1/student/fees`

- `GET /`
- `GET /eligibility`
- `GET /{semesterFeeId}/payments`

### Notifications

Teacher announcements:
- Base: `/api/v1/teacher/notifications`
- `POST /announcements`

User notification center:
- Base: `/api/v1/notifications`
- `GET /me`
- `GET /me/unread-count`
- `POST /{notificationId}/read`
- `POST /{notificationId}/unread`
- `POST /me/read-all`

### Admin Reporting
Base: `/api/v1/admin/reports`

- Dashboard
  - `GET /dashboard`
- Fee reports
  - `GET /fee-defaulters`
  - `GET /fee-defaulters/export`
- Attendance reports
  - `GET /attendance-shortages`
  - `GET /attendance-shortages/export`
- Result reports
  - `GET /results-summary`
  - `GET /students/{studentId}/academic-snapshot`
- Student analytics
  - `GET /student-distribution`
  - `GET /student-distribution/export`
  - `GET /student-analytics`
- Teacher workload
  - `GET /teacher-workloads`
  - `GET /teacher-workloads/export`

### Admin Audit
Base: `/api/v1/admin/audit`

- `GET /logs`
- `GET /logs/export`

## High-Value Filters by Module
- Admin users:
  - teachers: department and free-text search
  - students: department, program, section, and free-text search
- Offerings:
  - term, section, teacher, subject, status
- Enrollments:
  - term and status for student-facing views
- Student fees:
  - term and fee status
- Reports:
  - term, program, section, query, academic status, date ranges, sort options
- Audit logs:
  - `actionType`, `actorUserId`, `entityType`, `entityId`, `createdFrom`, `createdTo`

## Notes for API Consumers
- CSV export endpoints return `text/csv`.
- Reporting endpoints can be expensive on large datasets, but query-side aggregation and indexes are already in place for the current implementation.
- Swagger/OpenAPI is enabled, but endpoint-level operation descriptions are still minimal in code. Use this file plus Swagger UI together.

## Source of Truth
- Security rules: [SecurityConfig.java](C:\Users\Niranjan\Desktop\Student_mngmnt\src\main\java\com\example\sams\config\SecurityConfig.java)
- OpenAPI setup: [OpenApiConfig.java](C:\Users\Niranjan\Desktop\Student_mngmnt\src\main\java\com\example\sams\config\OpenApiConfig.java)
- Application config: [application.yml](C:\Users\Niranjan\Desktop\Student_mngmnt\src\main\resources\application.yml)
- Controllers: `src/main/java/com/example/sams/**/controller/*Controller.java`
